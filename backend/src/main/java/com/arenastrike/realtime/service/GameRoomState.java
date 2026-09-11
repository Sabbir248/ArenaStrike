package com.arenastrike.realtime.service;

import com.arenastrike.realtime.dto.*;
import com.arenastrike.lobby.model.GameMap;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import com.arenastrike.combat.*;

/**
 * Thread-safe state for one active room. Player updates replace immutable
 * values, so the scheduler never observes a partially-mutated player state.
 */
public final class GameRoomState {
    private static final double BASE_SPEED = 7.0;
    private static final double MOVEMENT_TOLERANCE = 0.05;
    private static final double PLAYER_RADIUS = 0.45;
    private static final List<Obstacle> WAREHOUSE_OBSTACLES = List.of(
            new Obstacle(-28, -18, -8, 2), new Obstacle(28, 18, -8, 2),
            new Obstacle(-28, -18, 10, 14), new Obstacle(28, 18, 10, 14),
            new Obstacle(-9.5, -6.5, 24.5, 27.5), new Obstacle(6.5, 9.5, -27.5, -24.5));
    private static final List<Obstacle> BUNKER_OBSTACLES = List.of(
            new Obstacle(-39, -21, -30, -26), new Obstacle(21, 39, 26, 30),
            new Obstacle(-24, -20, 19, 37), new Obstacle(20, 24, -37, -19));
    private final String roomCode;
    private final GameMap map;
    private final int maxPlayers;
    private final ConcurrentHashMap<Long, PlayerState> players = new ConcurrentHashMap<>();
    private final Scoreboard scoreboard = new Scoreboard();
    private final ConcurrentHashMap<Long, PlayerWeaponState> weaponStates = new ConcurrentHashMap<>();
    private final AtomicLong tick = new AtomicLong();
    private volatile RoomState roomState = RoomState.LOBBY;
    private volatile long matchEndsAtMillis;
    private volatile MatchOverEvent matchOverEvent;

    GameRoomState(String roomCode, GameMap map, int maxPlayers) {
        this.roomCode = roomCode;
        this.map = map;
        this.maxPlayers = maxPlayers;
    }

    public synchronized void addPlayer(PlayerState player) {
        if (roomState == RoomState.FINISHED) {
            throw new GameStateConflictException("Match is over");
        }
        if (players.putIfAbsent(player.playerId(), player) != null) {
            throw new GameStateConflictException("Player is already active in this room");
        }
        scoreboard.addPlayer(player.playerId());
        weaponStates.putIfAbsent(player.playerId(), new PlayerWeaponState(
                WeaponFactory.get(player.currentWeapon())));
    }

    public synchronized void updatePlayer(UpdatePlayerStateCommand update) {
        ensureNotFinished();
        players.compute(update.playerId(), (id, current) -> {
            if (current == null) {
                throw new GameStateConflictException("Player is not active in this room");
            }
            Vector3 safePosition = validatePosition(current.position(), update.position(), update.state(), 0.05);
            return new PlayerState(current.playerId(), current.displayName(), safePosition,
                    update.rotation(), current.health(), update.currentWeapon(), update.state());
        });
    }

    public synchronized void applyMovement(Long playerId, PlayerMovementPacket input, double deltaSeconds,
                           ConcurrentHashMap<Long, Double> verticalVelocity) {
        if (roomState == RoomState.FINISHED) {
            return;
        }
        players.computeIfPresent(playerId, (id, current) -> {
            double velocity = verticalVelocity.getOrDefault(id, 0.0);
            if (input.isJumping() && current.state() != MovementState.PRONE
                    && current.position().y() <= 0.01) {
                velocity = 6.5;
            }
            velocity -= 18.0 * deltaSeconds;
            double expectedY = current.position().y() + velocity * deltaSeconds;
            if (expectedY < 0) {
                expectedY = 0;
                velocity = 0;
            }
            
            Vector3 clientProposed = new Vector3(input.x(), expectedY, input.z());
            MovementState state = velocity != 0 ? MovementState.JUMPING : input.state();
            
            Vector3 safePosition = validatePosition(current.position(), clientProposed, state, deltaSeconds);
            verticalVelocity.put(id, velocity);
            return new PlayerState(id, current.displayName(),
                    safePosition, input.rotation(),
                    current.health(), input.currentWeapon(), state);
        });
    }

    private Vector3 validatePosition(Vector3 oldPosition, Vector3 requested,
                                     MovementState state, double deltaSeconds) {
        double dx = requested.x() - oldPosition.x();
        double dz = requested.z() - oldPosition.z();
        double distance = Math.sqrt(dx * dx + dz * dz);
        
        double multiplier = switch (state) {
            case STANDING -> 1.0;
            case CROUCHING -> 0.5;
            case PRONE -> 0.2;
            case JUMPING -> 0.85;
        };
        double maxDistance = BASE_SPEED * multiplier * deltaSeconds + MOVEMENT_TOLERANCE;
        
        if (distance > maxDistance) {
            double scale = maxDistance / distance;
            requested = new Vector3(oldPosition.x() + dx * scale,
                    requested.y(), oldPosition.z() + dz * scale);
        }
        
        requested = new Vector3(Math.max(-60, Math.min(60, requested.x())),
                Math.max(0, requested.y()), Math.max(-60, Math.min(60, requested.z())));
                
        return isBlocked(requested) ? oldPosition : requested;
    }

    private boolean isBlocked(Vector3 position) {
        return obstacles().stream().anyMatch(obstacle ->
                position.x() > obstacle.minX() - PLAYER_RADIUS
                        && position.x() < obstacle.maxX() + PLAYER_RADIUS
                        && position.z() > obstacle.minZ() - PLAYER_RADIUS
                        && position.z() < obstacle.maxZ() + PLAYER_RADIUS);
    }

    public synchronized boolean blocksRay(Vector3 origin, Vector3 direction, double maxDistance) {
        return firstObstacleDistance(origin, direction, maxDistance) < maxDistance;
    }

    public synchronized double firstObstacleDistance(Vector3 origin, Vector3 direction, double maxDistance) {
        double nearest = maxDistance;
        for (Obstacle obstacle : obstacles()) {
            double entry = 0;
            double exit = maxDistance;
            double[] x = raySlab(origin.x(), direction.x(), obstacle.minX(), obstacle.maxX());
            double[] z = raySlab(origin.z(), direction.z(), obstacle.minZ(), obstacle.maxZ());
            if (x == null || z == null) {
                continue;
            }
            entry = Math.max(entry, Math.max(x[0], z[0]));
            exit = Math.min(exit, Math.min(x[1], z[1]));
            if (entry <= exit && exit >= 0 && entry <= maxDistance) {
                nearest = Math.min(nearest, Math.max(0, entry));
            }
        }
        return nearest;
    }

    private double[] raySlab(double origin, double direction, double min, double max) {
        if (Math.abs(direction) < 1e-9) {
            return origin >= min && origin <= max
                    ? new double[]{0, Double.POSITIVE_INFINITY} : null;
        }
        double first = (min - origin) / direction;
        double second = (max - origin) / direction;
        return first <= second ? new double[]{first, second} : new double[]{second, first};
    }

    private List<Obstacle> obstacles() {
        return map == GameMap.MAP_WAREHOUSE ? WAREHOUSE_OBSTACLES : BUNKER_OBSTACLES;
    }

    public synchronized void removePlayer(Long playerId) {
        players.remove(playerId);
        scoreboard.removePlayer(playerId);
    }

    public synchronized PlayerState removePlayerAndReturn(Long playerId) {
        PlayerState removed = players.remove(playerId);
        scoreboard.removePlayer(playerId);
        weaponStates.remove(playerId);
        return removed;
    }

    public synchronized PlayerWeaponState weaponState(Long playerId, Weapon weapon) {
        PlayerWeaponState current = weaponStates.get(playerId);
        if (current == null || current.weapon() != weapon) {
            current = new PlayerWeaponState(weapon);
            weaponStates.put(playerId, current);
        }
        return current;
    }

    public synchronized List<AmmoStateEvent> completeReloads(long now) {
        List<AmmoStateEvent> completed = new ArrayList<>();
        weaponStates.forEach((playerId, state) -> {
            if (state.completeReloadIfReady(now)) {
                completed.add(new AmmoStateEvent("AMMO_STATE", String.valueOf(playerId),
                        state.weapon().name(), state.currentAmmo(), false));
            }
        });
        return completed;
    }

    public synchronized AmmoStateEvent beginReload(Long playerId, Weapon weapon, long now) {
        PlayerWeaponState state = weaponState(playerId, weapon);
        state.beginReload(now);
        return new AmmoStateEvent("AMMO_STATE", String.valueOf(playerId),
                weapon.name(), state.currentAmmo(), state.isReloading());
    }

    public PlayerState player(Long playerId) {
        return players.get(playerId);
    }

    public boolean applyDamage(Long playerId, int damage) {
        return applyDamage(playerId, damage, false);
    }

    public boolean applyDamage(Long playerId, int damage, boolean instantKill) {
        return applyDamageAndGetHealth(playerId, damage, instantKill) == 0;
    }

    public synchronized int applyDamageAndGetHealth(Long playerId, int damage, boolean instantKill) {
        final boolean[] died = {false};
        final int[] remainingHealth = {-1};
        players.computeIfPresent(playerId, (id, current) -> {
            int health = instantKill ? 0 : Math.max(0, current.health() - damage);
            died[0] = current.health() > 0 && health == 0;
            remainingHealth[0] = health;
            return new PlayerState(
                current.playerId(), current.displayName(), current.position(),
                current.rotation(), health,
                current.currentWeapon(), current.state());
        });
        if (died[0]) {
            scoreboard.addDeath(playerId);
        }
        return remainingHealth[0];
    }

    public void recordDamageDealt(Long attackerId, int damage) {
        scoreboard.addDamage(attackerId, damage);
    }

    public void recordKill(Long killerId, HitLocation location) {
        scoreboard.addKill(killerId, location == HitLocation.HEAD);
    }

    public int killsFor(Long playerId) {
        Scoreboard.PlayerScore score = scoreboard.getScore(playerId);
        return score == null ? 0 : score.getKills();
    }

    public synchronized void startIfFull(int playerLimit) {
        if (roomState == RoomState.LOBBY && playerLimit >= 2 && players.size() == playerLimit) {
            roomState = RoomState.STARTING;
        }
    }

    public synchronized void activateIfStarting() {
        if (roomState == RoomState.STARTING) {
            roomState = RoomState.ACTIVE;
            matchEndsAtMillis = System.currentTimeMillis() + 300_000L;
        }
    }

    public synchronized MatchSummary forceFinishMatch() {
        if (roomState != RoomState.ACTIVE) {
            return null;
        }
        return finishMatch();
    }

    public synchronized MatchSummary finishIfForfeit() {
        if (roomState != RoomState.ACTIVE || players.size() > 1) {
            return null;
        }
        return finishMatch();
    }

    private MatchSummary finishMatch() {
        roomState = RoomState.FINISHED;
        List<MatchSummary.PlayerResult> results = scoreboard.getAllScores().stream()
                .map(score -> {
                    PlayerState state = players.get(score.getPlayerId());
                    String name = state != null ? state.displayName() : "Unknown";
                    String weaponUsed = state != null ? state.currentWeapon() : "PISTOL";
                    return new MatchSummary.PlayerResult(score.getPlayerId(), name,
                            score.getKills(), score.getDeaths(),
                            score.getHeadshotKills(), score.getDamageDealt(), weaponUsed);
                })
                .sorted(Comparator.comparingInt(MatchSummary.PlayerResult::kills).reversed()
                        .thenComparingInt(MatchSummary.PlayerResult::deaths)
                        .thenComparing(MatchSummary.PlayerResult::playerId))
                .toList();
        int highestKills = results.isEmpty() ? 0 : results.get(0).kills();
        int fewestDeaths = results.stream()
                .filter(result -> result.kills() == highestKills)
                .mapToInt(MatchSummary.PlayerResult::deaths)
                .min().orElse(0);
        List<Long> winnerIds = results.stream()
                .filter(result -> result.kills() == highestKills && result.deaths() == fewestDeaths)
                .map(MatchSummary.PlayerResult::playerId)
                .toList();
        Long winnerId = winnerIds.isEmpty() ? null : winnerIds.get(0);
        PlayerState winner = winnerId == null ? null : players.get(winnerId);
        return new MatchSummary("MATCH_END", roomCode, roomState, winnerId,
                winnerIds,
                winner == null ? null : winner.displayName(),
                winnerId == null ? 0 : killsFor(winnerId), map.name(), results);
    }

    public synchronized RoomGameState snapshot() {
        return snapshot(0L);
    }

    public synchronized RoomGameState snapshot(long remainingSeconds) {
        return new RoomGameState(roomCode, tick.incrementAndGet(),
                players.values().stream()
                        .sorted(Comparator.comparing(PlayerState::playerId))
                        .toList(), roomState, map.name(), maxPlayers, remainingSeconds);
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public synchronized boolean isFinished() {
        return roomState == RoomState.FINISHED;
    }

    private void ensureNotFinished() {
        if (roomState == RoomState.FINISHED) {
            throw new GameStateConflictException("Match is over");
        }
    }

    private record Obstacle(double minX, double maxX, double minZ, double maxZ) {
    }
}
