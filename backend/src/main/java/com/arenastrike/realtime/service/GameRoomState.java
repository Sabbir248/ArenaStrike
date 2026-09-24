package com.arenastrike.realtime.service;

import com.arenastrike.realtime.dto.*;
import com.arenastrike.lobby.model.GameMap;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.StampedLock;
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
            
    private static final List<Vector3> WAREHOUSE_SPAWNS = List.of(
            new Vector3(0, 0, 35), new Vector3(0, 0, -35),
            new Vector3(15, 0, 35), new Vector3(-15, 0, -35),
            new Vector3(-15, 0, 35), new Vector3(15, 0, -35),
            new Vector3(25, 0, 15), new Vector3(-25, 0, -15),
            new Vector3(-25, 0, 15), new Vector3(25, 0, -15)
    );
    private static final List<Vector3> BUNKER_SPAWNS = List.of(
            new Vector3(0, 0, 45), new Vector3(0, 0, -45),
            new Vector3(15, 0, 45), new Vector3(-15, 0, -45),
            new Vector3(-15, 0, 45), new Vector3(15, 0, -45),
            new Vector3(30, 0, 15), new Vector3(-30, 0, -15),
            new Vector3(-30, 0, 15), new Vector3(30, 0, -15)
    );
            
    private final String roomCode;
    private final GameMap map;
    private final int maxPlayers;
    private final ConcurrentHashMap<Long, PlayerState> players = new ConcurrentHashMap<>();
    private final Scoreboard scoreboard = new Scoreboard();
    private final ConcurrentHashMap<Long, PlayerWeaponState> weaponStates = new ConcurrentHashMap<>();
    private final AtomicLong tick = new AtomicLong();
    private final java.util.concurrent.atomic.AtomicInteger nextSpawnIndex = new java.util.concurrent.atomic.AtomicInteger(0);
    
    // Concurrency optimization: StampedLock instead of intrinsic locks
    private final StampedLock lock = new StampedLock();
    
    private volatile RoomState roomState = RoomState.LOBBY;
    private volatile long matchEndsAtMillis;
    private volatile MatchOverEvent matchOverEvent;

    GameRoomState(String roomCode, GameMap map, int maxPlayers) {
        this.roomCode = roomCode;
        this.map = map;
        this.maxPlayers = maxPlayers;
    }

    public void addPlayer(PlayerState player) {
        long stamp = lock.writeLock();
        try {
            if (roomState == RoomState.FINISHED) {
                throw new GameStateConflictException("Match is over");
            }
            Vector3 spawnPoint = nextSpawnPoint();
            PlayerState spawnedPlayer = new PlayerState(player.playerId(), player.displayName(), spawnPoint,
                    player.rotation(), player.health(), player.currentWeapon(), player.state());
            if (players.putIfAbsent(player.playerId(), spawnedPlayer) != null) {
                throw new GameStateConflictException("Player is already active in this room");
            }
            scoreboard.addPlayer(player.playerId());
            weaponStates.putIfAbsent(player.playerId(), new PlayerWeaponState(
                    WeaponFactory.get(player.currentWeapon())));
        } finally {
            lock.unlockWrite(stamp);
        }
    }
    
    private Vector3 nextSpawnPoint() {
        List<Vector3> spawns = map == GameMap.MAP_WAREHOUSE ? WAREHOUSE_SPAWNS : BUNKER_SPAWNS;
        return spawns.get(nextSpawnIndex.getAndIncrement() % spawns.size());
    }

    public void updatePlayer(UpdatePlayerStateCommand update) {
        long stamp = lock.writeLock();
        try {
            ensureNotFinished();
            players.compute(update.playerId(), (id, current) -> {
                if (current == null) {
                    throw new GameStateConflictException("Player is not active in this room");
                }
                Vector3 safePosition = validatePosition(current.position(), update.position(), update.state(), 0.05);
                return new PlayerState(current.playerId(), current.displayName(), safePosition,
                        update.rotation(), current.health(), update.currentWeapon(), update.state());
            });
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public void applyMovement(Long playerId, PlayerMovementPacket input, double deltaSeconds,
                           ConcurrentHashMap<Long, Double> verticalVelocity) {
        long stamp = lock.writeLock();
        try {
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
        } finally {
            lock.unlockWrite(stamp);
        }
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
            case DEAD -> 0.0;
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

    public boolean blocksRay(Vector3 origin, Vector3 direction, double maxDistance) {
        long stamp = lock.tryOptimisticRead();
        boolean blocks = firstObstacleDistance(origin, direction, maxDistance) < maxDistance;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                blocks = firstObstacleDistance(origin, direction, maxDistance) < maxDistance;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return blocks;
    }

    public double firstObstacleDistance(Vector3 origin, Vector3 direction, double maxDistance) {
        long stamp = lock.tryOptimisticRead();
        double nearest = calculateFirstObstacleDistance(origin, direction, maxDistance);
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                nearest = calculateFirstObstacleDistance(origin, direction, maxDistance);
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return nearest;
    }

    private double calculateFirstObstacleDistance(Vector3 origin, Vector3 direction, double maxDistance) {
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

    public void removePlayer(Long playerId) {
        long stamp = lock.writeLock();
        try {
            players.remove(playerId);
            scoreboard.removePlayer(playerId);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public PlayerState removePlayerAndReturn(Long playerId) {
        long stamp = lock.writeLock();
        try {
            PlayerState removed = players.remove(playerId);
            scoreboard.removePlayer(playerId);
            weaponStates.remove(playerId);
            return removed;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public PlayerState respawnPlayer(Long playerId) {
        long stamp = lock.writeLock();
        try {
            return players.computeIfPresent(playerId, (id, current) -> {
                Vector3 spawnPosition = nextSpawnPoint();

                return new PlayerState(current.playerId(), current.displayName(), spawnPosition,
                        current.rotation(), 100, // Restore HP
                        current.currentWeapon(), MovementState.STANDING); // Reset state
            });
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public PlayerWeaponState weaponState(Long playerId, Weapon weapon) {
        long stamp = lock.writeLock();
        try {
            return getOrCreateWeaponState(playerId, weapon);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private PlayerWeaponState getOrCreateWeaponState(Long playerId, Weapon weapon) {
        PlayerWeaponState current = weaponStates.get(playerId);
        if (current == null || current.weapon() != weapon) {
            current = new PlayerWeaponState(weapon);
            weaponStates.put(playerId, current);
        }
        return current;
    }

    public List<AmmoStateEvent> completeReloads(long now) {
        long stamp = lock.writeLock();
        try {
            List<AmmoStateEvent> completed = new ArrayList<>();
            weaponStates.forEach((playerId, state) -> {
                if (state.completeReloadIfReady(now)) {
                    completed.add(new AmmoStateEvent("AMMO_STATE", String.valueOf(playerId),
                            state.weapon().name(), state.currentAmmo(), false));
                }
            });
            return completed;
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public AmmoStateEvent beginReload(Long playerId, Weapon weapon, long now) {
        long stamp = lock.writeLock();
        try {
            PlayerWeaponState state = getOrCreateWeaponState(playerId, weapon);
            state.beginReload(now);
            return new AmmoStateEvent("AMMO_STATE", String.valueOf(playerId),
                    weapon.name(), state.currentAmmo(), state.isReloading());
        } finally {
            lock.unlockWrite(stamp);
        }
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

    public int applyDamageAndGetHealth(Long playerId, int damage, boolean instantKill) {
        long stamp = lock.writeLock();
        try {
            final boolean[] died = {false};
            final int[] remainingHealth = {-1};
            players.computeIfPresent(playerId, (id, current) -> {
                int health = instantKill ? 0 : Math.max(0, current.health() - damage);
                died[0] = current.health() > 0 && health == 0;
                remainingHealth[0] = health;
                MovementState newState = died[0] ? MovementState.DEAD : current.state();
                return new PlayerState(
                    current.playerId(), current.displayName(), current.position(),
                    current.rotation(), health,
                    current.currentWeapon(), newState);
            });
            if (died[0]) {
                scoreboard.addDeath(playerId);
            }
            return remainingHealth[0];
        } finally {
            lock.unlockWrite(stamp);
        }
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

    public void manualStart() {
        long stamp = lock.writeLock();
        try {
            if (roomState == RoomState.LOBBY) {
                roomState = RoomState.STARTING;
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public void activateIfStarting() {
        long stamp = lock.writeLock();
        try {
            if (roomState == RoomState.STARTING) {
                roomState = RoomState.ACTIVE;
                matchEndsAtMillis = System.currentTimeMillis() + 300_000L;
            }
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public long matchEndsAtMillis() {
        return matchEndsAtMillis;
    }

    public MatchSummary forceFinishMatch() {
        long stamp = lock.writeLock();
        try {
            if (roomState != RoomState.ACTIVE) {
                return null;
            }
            return finishMatch();
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    public MatchSummary finishIfForfeit() {
        long stamp = lock.writeLock();
        try {
            if (roomState != RoomState.ACTIVE || players.size() > 1) {
                return null;
            }
            return finishMatch();
        } finally {
            lock.unlockWrite(stamp);
        }
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

    public RoomGameState snapshot() {
        return snapshot(0L);
    }

    public RoomGameState snapshot(long remainingSeconds) {
        long stamp = lock.tryOptimisticRead();
        RoomState currentRoomState = roomState;
        List<PlayerState> playerList = players.values().stream()
                .sorted(Comparator.comparing(PlayerState::playerId))
                .toList();
        long currentTick = tick.get(); // not covered by stamped lock, it's atomic

        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                currentRoomState = roomState;
                playerList = players.values().stream()
                        .sorted(Comparator.comparing(PlayerState::playerId))
                        .toList();
            } finally {
                lock.unlockRead(stamp);
            }
        }
        
        return new RoomGameState(roomCode, tick.incrementAndGet(),
                playerList, currentRoomState, map.name(), maxPlayers, remainingSeconds);
    }

    public boolean isEmpty() {
        return players.isEmpty();
    }

    public boolean isFinished() {
        long stamp = lock.tryOptimisticRead();
        boolean finished = roomState == RoomState.FINISHED;
        if (!lock.validate(stamp)) {
            stamp = lock.readLock();
            try {
                finished = roomState == RoomState.FINISHED;
            } finally {
                lock.unlockRead(stamp);
            }
        }
        return finished;
    }

    private void ensureNotFinished() {
        if (roomState == RoomState.FINISHED) {
            throw new GameStateConflictException("Match is over");
        }
    }

    private record Obstacle(double minX, double maxX, double minZ, double maxZ) {
    }

    private final Map<Long, Long> lastClientShotTimestamps = new HashMap<>();
    private static final double SHOTGUN_MAX_RANGE = 20.0;
    private static final double SHOTGUN_FULL_DAMAGE_RANGE = 7.0;
    private static final double SHOTGUN_MIN_FALLOFF = 0.25;

    public VerifiedShot shoot(Long shooterId, ShootCommand command, Weapon weapon) {
        long stamp = lock.writeLock();
        try {
            PlayerState shooter = players.get(shooterId);
            if (shooter == null) {
                throw new GameStateConflictException("Shooter must be an active player");
            }
            if (shooter.health() <= 0 || roomState != RoomState.ACTIVE) {
                throw new GameStateConflictException("Player cannot shoot in the current match state");
            }
            if (!weapon.name().equals(command.weaponId())
                    || !weapon.name().equals(shooter.currentWeapon())) {
                throw new GameStateConflictException("Weapon is not equipped by the shooter");
            }
            long now = System.currentTimeMillis();
            PlayerWeaponState weaponState = getOrCreateWeaponState(shooterId, weapon);
            if (weaponState.isReloading() || weaponState.reloadInProgress(now)) {
                return new VerifiedShot("SHOT_VERIFIED", String.valueOf(shooterId),
                        weapon.name(), weaponState.currentAmmo(), true, null);
            }
            if (now - weaponState.lastShotTimestamp() < weapon.cooldownMillis()) {
                throw new GameStateConflictException("Weapon is on cooldown");
            }
            long previousClientTimestamp = lastClientShotTimestamps.getOrDefault(shooterId, Long.MIN_VALUE);
            boolean firstClientTimestamp = previousClientTimestamp == Long.MIN_VALUE;
            if (Math.abs(now - command.timestamp()) > 5000
                    || (!firstClientTimestamp && command.timestamp() <= previousClientTimestamp)) {
                throw new GameStateConflictException("Shot timestamp is invalid");
            }
            int ammo = weaponState.currentAmmo();
            if (ammo <= 0) {
                weaponState.beginReload(now);
                return new VerifiedShot("SHOT_VERIFIED", String.valueOf(shooterId),
                        weapon.name(), 0, weaponState.isReloading(), null);
            }
            lastClientShotTimestamps.put(shooterId, command.timestamp());
            weaponState.markShot(now);
            if (weaponState.currentAmmo() == 0) {
                weaponState.beginReload(now);
            }
            int ammoAfterShot = weaponState.currentAmmo();
            double spreadMultiplier = switch (shooter.state()) {
                case JUMPING -> 2.5;
                case STANDING -> 1.0;
                case CROUCHING -> 0.75;
                case PRONE -> 0.5;
                case DEAD -> 0.0;
            };
            double maxSpread = weapon.maxSpreadRadians() * spreadMultiplier;
            double basePitch = shooter.rotation().x();
            double pitch = Math.max(basePitch - maxSpread, Math.min(basePitch + maxSpread, command.angle()));
            double yaw = shooter.rotation().y();
            
            // Add eye height to the shooter's feet position to accurately trace from camera
            double eyeHeight = switch (shooter.state()) {
                case PRONE -> 0.65;
                case CROUCHING -> 1.15;
                default -> 1.7;
            };
            Vector3 origin = new Vector3(shooter.position().x(), shooter.position().y() + eyeHeight, shooter.position().z());
            
            Vector3 direction = new Vector3(
                    -Math.sin(yaw) * Math.cos(pitch),
                    Math.sin(pitch),
                    -Math.cos(yaw) * Math.cos(pitch));
            List<PlayerHitbox> hitboxes = players.values().stream()
                    .filter(p -> p.state() != MovementState.DEAD && p.health() > 0)
                    .map(PlayerHitbox::from).toList();
            if (weapon.id().equals("SHOTGUN")) {
                return shotgunShot(shooter, shooterId, weapon, ammoAfterShot, weaponState.isReloading(),
                        origin, yaw, pitch, hitboxes);
            }
            double obstacleDistance = calculateFirstObstacleDistance(origin, direction, 200);
            RaycastHit hit = CombatRaycast.firstHit(origin, direction, shooter.playerId(), hitboxes,
                    obstacleDistance);
            if (hit == null || hit.hitLocation() == HitLocation.MISS
                    || hit.hitLocation() == HitLocation.NONE) {
                return new VerifiedShot("SHOT_VERIFIED", String.valueOf(shooterId),
                        weapon.name(), ammoAfterShot, weaponState.isReloading(), null);
            }
            PlayerState target = players.get(hit.targetId());
            if (target == null || target.health() <= 0) {
                return new VerifiedShot("SHOT_VERIFIED", String.valueOf(shooterId),
                        weapon.name(), ammoAfterShot, weaponState.isReloading(), null);
            }
            int damage = weapon.damageFor(hit.hitLocation());
            boolean instantKill = weapon.isInstantKill(hit.hitLocation());
            
            int health = instantKill ? 0 : Math.max(0, target.health() - damage);
            boolean died = target.health() > 0 && health == 0;
            MovementState newState = died ? MovementState.DEAD : target.state();
            players.put(hit.targetId(), new PlayerState(
                target.playerId(), target.displayName(), target.position(),
                target.rotation(), health,
                target.currentWeapon(), newState));
            
            if (died) scoreboard.addDeath(hit.targetId());
            scoreboard.addDamage(shooterId, damage);
            if (health == 0) scoreboard.addKill(shooterId, hit.hitLocation() == HitLocation.HEAD);

            PlayerHitEvent hitEvent = new PlayerHitEvent("PLAYER_HIT",
                    String.valueOf(target.playerId()), String.valueOf(shooter.playerId()),
                    damage, hit.hitLocation().name(), health, ammoAfterShot);
            return new VerifiedShot("SHOT_VERIFIED", String.valueOf(shooterId),
                    weapon.name(), ammoAfterShot, weaponState.isReloading(), hitEvent);
        } finally {
            lock.unlockWrite(stamp);
        }
    }

    private VerifiedShot shotgunShot(PlayerState shooter, Long shooterId, Weapon weapon,
                                     int ammoAfterShot, boolean reloading, Vector3 origin, double yaw, double pitch,
                                     List<PlayerHitbox> hitboxes) {
        Map<Long, PelletAggregate> hits = new HashMap<>();
        for (int pellet = 0; pellet < weapon.pelletCount(); pellet++) {
            double phase = 2 * Math.PI * pellet / weapon.pelletCount();
            double radius = weapon.pelletSpreadRadians() * ((pellet % 2) == 0 ? 0.65 : 1.0);
            double pelletYaw = yaw + Math.cos(phase) * radius;
            double pelletPitch = pitch + Math.sin(phase) * radius;
            Vector3 direction = new Vector3(
                    -Math.sin(pelletYaw) * Math.cos(pelletPitch),
                    Math.sin(pelletPitch),
                    -Math.cos(pelletYaw) * Math.cos(pelletPitch));
            double obstacleDistance = calculateFirstObstacleDistance(origin, direction, SHOTGUN_MAX_RANGE);
            RaycastHit hit = CombatRaycast.firstHit(origin, direction,
                    shooter.playerId(), hitboxes, obstacleDistance);
            if (hit == null || hit.hitLocation() == HitLocation.MISS || hit.hitLocation() == HitLocation.NONE) {
                continue;
            }
            double falloff = shotgunFalloff(hit.distance());
            PelletAggregate current = hits.get(hit.targetId());
            hits.put(hit.targetId(), current == null
                    ? PelletAggregate.first(hit.hitLocation(), hit.distance(), falloff)
                    : current.add(hit.hitLocation(), hit.distance(), falloff));
        }

        Map.Entry<Long, PelletAggregate> selected = hits.entrySet().stream()
                .max(Map.Entry.comparingByValue(java.util.Comparator.comparingDouble(
                        PelletAggregate::damageScore))).orElse(null);
        if (selected == null) {
            return new VerifiedShot("SHOT_VERIFIED", String.valueOf(shooterId),
                    weapon.name(), ammoAfterShot, reloading, null);
        }
        Long targetId = selected.getKey();
        PelletAggregate aggregate = selected.getValue();
        int damage = Math.min(90, (int) Math.round(aggregate.totalDamage()));
        PlayerState target = players.get(targetId);
        if (target == null || target.health() <= 0) {
            return new VerifiedShot("SHOT_VERIFIED", String.valueOf(shooterId),
                    weapon.name(), ammoAfterShot, reloading, null);
        }
        
        int health = Math.max(0, target.health() - damage);
        boolean died = target.health() > 0 && health == 0;
        MovementState newState = died ? MovementState.DEAD : target.state();
        players.put(targetId, new PlayerState(
            target.playerId(), target.displayName(), target.position(),
            target.rotation(), health,
            target.currentWeapon(), newState));
            
        if (died) scoreboard.addDeath(targetId);
        scoreboard.addDamage(shooterId, damage);
        if (health == 0) scoreboard.addKill(shooterId, aggregate.zone() == HitLocation.HEAD);

        PlayerHitEvent hitEvent = new PlayerHitEvent("PLAYER_HIT",
                String.valueOf(targetId), String.valueOf(shooterId), damage,
                aggregate.zone().name(), health, ammoAfterShot);
        return new VerifiedShot("SHOT_VERIFIED", String.valueOf(shooterId),
                weapon.name(), ammoAfterShot, reloading, hitEvent);
    }

    private double shotgunFalloff(double distance) {
        if (distance <= SHOTGUN_FULL_DAMAGE_RANGE) {
            return 1.0;
        }
        double range = Math.max(0, Math.min(1,
                (distance - SHOTGUN_FULL_DAMAGE_RANGE)
                        / (SHOTGUN_MAX_RANGE - SHOTGUN_FULL_DAMAGE_RANGE)));
        return 1.0 - range * (1.0 - SHOTGUN_MIN_FALLOFF);
    }

    private record PelletAggregate(HitLocation zone, double distance, int pellets, double totalDamage) {
        static PelletAggregate first(HitLocation zone, double distance, double falloff) {
            return new PelletAggregate(zone, distance, 1,
                    (double) WeaponFactory.get("SHOTGUN").damageFor(zone) * falloff / WeaponFactory.get("SHOTGUN").pelletCount());
        }

        PelletAggregate add(HitLocation nextZone, double nextDistance, double nextFalloff) {
            HitLocation strongest = WeaponFactory.get("SHOTGUN").damageFor(nextZone)
                    > WeaponFactory.get("SHOTGUN").damageFor(zone) ? nextZone : zone;
            return new PelletAggregate(strongest, Math.min(distance, nextDistance),
                    pellets + 1, totalDamage
                            + (double) WeaponFactory.get("SHOTGUN").damageFor(nextZone) * nextFalloff / WeaponFactory.get("SHOTGUN").pelletCount());
        }

        double damageScore() {
            return totalDamage;
        }
    }
}
