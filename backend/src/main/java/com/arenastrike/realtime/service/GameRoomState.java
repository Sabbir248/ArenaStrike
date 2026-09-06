package com.arenastrike.realtime.service;

import com.arenastrike.realtime.dto.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import com.arenastrike.combat.*;

/**
 * Thread-safe state for one active room. Player updates replace immutable
 * values, so the scheduler never observes a partially-mutated player state.
 */
public final class GameRoomState {
    private final String roomCode;
    private final ConcurrentHashMap<Long, PlayerState> players = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Long, Integer> verifiedKills = new ConcurrentHashMap<>();
    private final AtomicLong tick = new AtomicLong();
    private volatile MatchStatus matchStatus = MatchStatus.WAITING;
    private volatile long matchEndsAtMillis;
    private volatile MatchOverEvent matchOverEvent;

    GameRoomState(String roomCode) {
        this.roomCode = roomCode;
    }

    public void addPlayer(PlayerState player) {
        if (matchStatus == MatchStatus.OVER) {
            throw new GameStateConflictException("Match is over");
        }
        if (players.putIfAbsent(player.playerId(), player) != null) {
            throw new GameStateConflictException("Player is already active in this room");
        }
        verifiedKills.putIfAbsent(player.playerId(), 0);
    }

    public void updatePlayer(UpdatePlayerStateCommand update) {
        players.compute(update.playerId(), (id, current) -> {
            if (current == null) {
                throw new GameStateConflictException("Player is not active in this room");
            }
            return new PlayerState(
                current.playerId(), current.displayName(), update.position(),
                update.rotation(), current.health(), update.currentWeapon(), update.stance());
        });
    }

    public void removePlayer(Long playerId) {
        players.remove(playerId);
        verifiedKills.remove(playerId);
    }

    public PlayerState removePlayerAndReturn(Long playerId) {
        PlayerState removed = players.remove(playerId);
        verifiedKills.remove(playerId);
        return removed;
    }

    public PlayerState player(Long playerId) {
        return players.get(playerId);
    }

    public boolean applyDamage(Long playerId, int damage) {
        final boolean[] died = {false};
        players.computeIfPresent(playerId, (id, current) -> {
            int health = Math.max(0, current.health() - damage);
            died[0] = current.health() > 0 && health == 0;
            return new PlayerState(
                current.playerId(), current.displayName(), current.position(),
                current.rotation(), health,
                current.currentWeapon(), current.stance());
        });
        return died[0];
    }

    public void recordKill(Long killerId) {
        verifiedKills.computeIfPresent(killerId, (id, count) -> count + 1);
    }

    public int killsFor(Long playerId) {
        return verifiedKills.getOrDefault(playerId, 0);
    }

    public synchronized void startIfFull(int playerLimit) {
        if (matchStatus == MatchStatus.WAITING && playerLimit >= 2 && players.size() == playerLimit) {
            matchStatus = MatchStatus.RUNNING;
            matchEndsAtMillis = System.currentTimeMillis() + 300_000L;
        }
    }

    public synchronized MatchOverEvent advanceMatch() {
        if (matchStatus != MatchStatus.RUNNING || System.currentTimeMillis() < matchEndsAtMillis) {
            return null;
        }
        matchStatus = MatchStatus.OVER;
        Long winnerId = verifiedKills.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        PlayerState winner = winnerId == null ? null : players.get(winnerId);
        matchOverEvent = new MatchOverEvent(
                winnerId, winner == null ? null : winner.displayName(),
                winnerId == null ? 0 : killsFor(winnerId));
        return matchOverEvent;
    }

    public MatchOverEvent matchOverEvent() {
        return matchOverEvent;
    }

    public RoomGameState snapshot() {
        long remainingSeconds = matchStatus == MatchStatus.RUNNING
                ? Math.max(0, (matchEndsAtMillis - System.currentTimeMillis() + 999) / 1000)
                : 0;
        return new RoomGameState(roomCode, tick.incrementAndGet(),
                players.values().stream()
                        .sorted(Comparator.comparing(PlayerState::playerId))
                        .toList(), matchStatus, remainingSeconds);
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }
}
