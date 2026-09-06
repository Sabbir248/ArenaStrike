package com.arenastrike.realtime.service;

import com.arenastrike.realtime.dto.*;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import com.arenastrike.combat.WeaponType;
import com.arenastrike.lobby.repository.LobbyRepository;
import java.util.Optional;
import java.util.Locale;

@Service
public class GameStateService {
    private final ConcurrentHashMap<String, GameRoomState> activeRooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, Long>> lastShots = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SessionPlayer> sessions = new ConcurrentHashMap<>();
    private final LobbyRepository lobbyRepository;

    public GameStateService(LobbyRepository lobbyRepository) {
        this.lobbyRepository = lobbyRepository;
    }

    public void joinRoom(String roomCode, PlayerState player, String sessionId) {
        String normalized = normalizeRoomCode(roomCode);
        GameRoomState gameRoom = room(normalized);
        synchronized (gameRoom) {
            gameRoom.addPlayer(player);
            if (sessionId != null && !sessionId.isBlank()) {
                sessions.put(sessionId, new SessionPlayer(normalized, player.playerId()));
            }
        }
        lobbyRepository.findByRoomCode(normalized).ifPresent(lobby -> {
            long lobbyPlayers = lobbyRepository.countParticipantsByRoomCode(normalized);
            if (lobby.getPlayerLimit() >= 2 && lobbyPlayers == lobby.getPlayerLimit()) {
                gameRoom.startIfFull(lobby.getPlayerLimit());
            }
        });
    }

    public void updatePlayer(String roomCode, UpdatePlayerStateCommand update) {
        GameRoomState room = activeRooms.get(normalizeRoomCode(roomCode));
        if (room == null) {
            throw new GameStateConflictException("Room is not active");
        }
        room.updatePlayer(update);
    }

    public Optional<PlayerExitEvent> leaveRoom(String roomCode, Long playerId) {
        String normalizedRoomCode = normalizeRoomCode(roomCode);
        GameRoomState room = activeRooms.get(normalizedRoomCode);
        if (room == null) {
            return Optional.empty();
        }
        PlayerState removed;
        synchronized (room) {
            removed = room.removePlayerAndReturn(playerId);
        }
        if (removed == null) {
            return Optional.empty();
        }
        sessions.entrySet().removeIf(entry -> entry.getValue().matches(normalizedRoomCode, playerId));
        if (room.isEmpty()) {
            activeRooms.remove(normalizedRoomCode, room);
            lastShots.remove(normalizedRoomCode);
        }
        return Optional.of(new PlayerExitEvent(
                normalizedRoomCode, removed.playerId(), removed.displayName()));
    }

    public Optional<PlayerExitEvent> disconnectSession(String sessionId) {
        SessionPlayer session = sessions.remove(sessionId);
        return session == null ? Optional.empty() : leaveRoom(session.roomCode(), session.playerId());
    }

    public Optional<KillEvent> shoot(String roomCode, ShootCommand command) {
        String normalized = normalizeRoomCode(roomCode);
        GameRoomState room = activeRooms.get(normalized);
        if (room == null) {
            throw new GameStateConflictException("Room is not active");
        }
        synchronized (room) {
            return shootLocked(normalized, room, command);
        }
    }

    private Optional<KillEvent> shootLocked(
            String normalized, GameRoomState room, ShootCommand command) {
        PlayerState shooter = room.player(command.shooterId());
        PlayerState target = room.player(command.targetId());
        if (shooter == null || target == null) {
            throw new GameStateConflictException("Shooter and target must be active players");
        }
        if (shooter.health() <= 0 || target.health() <= 0 || room.snapshot().matchStatus() != MatchStatus.RUNNING) {
            throw new GameStateConflictException("Player cannot shoot in the current match state");
        }
        double dx = shooter.position().x() - target.position().x();
        double dy = shooter.position().y() - target.position().y();
        double dz = shooter.position().z() - target.position().z();
        if (dx * dx + dy * dy + dz * dz > 200 * 200) {
            throw new GameStateConflictException("Target is out of range");
        }
        long now = System.currentTimeMillis();
        ConcurrentHashMap<Long, Long> roomShots = lastShots
                .computeIfAbsent(normalized, ignored -> new ConcurrentHashMap<>())
                ;
        long previous = roomShots.getOrDefault(command.shooterId(), 0L);
        WeaponType weapon = command.weapon();
        if (now - previous < weapon.cooldownMillis()) {
            throw new GameStateConflictException("Weapon is on cooldown");
        }
        roomShots.put(command.shooterId(), now);
        boolean killed = room.applyDamage(command.targetId(), weapon.damageFor(command.hitLocation()));
        if (!killed) {
            return Optional.empty();
        }
        room.recordKill(command.shooterId());
        return Optional.of(new KillEvent(
                shooter.playerId(), shooter.displayName(), target.playerId(),
                target.displayName(), weapon.name()));
    }

    public Collection<GameRoomState> activeRooms() {
        return List.copyOf(activeRooms.values());
    }

    private GameRoomState room(String roomCode) {
        return activeRooms.computeIfAbsent(normalizeRoomCode(roomCode), GameRoomState::new);
    }

    private String normalizeRoomCode(String roomCode) {
        if (roomCode == null || !roomCode.trim().matches("[A-Za-z0-9]{6}")) {
            throw new GameStateConflictException("Invalid room code");
        }
        return roomCode.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private record SessionPlayer(String roomCode, Long playerId) {
        boolean matches(String roomCode, Long playerId) {
            return this.roomCode.equals(roomCode) && this.playerId.equals(playerId);
        }
    }
}
