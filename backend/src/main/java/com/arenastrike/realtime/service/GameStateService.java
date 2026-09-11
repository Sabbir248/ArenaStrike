package com.arenastrike.realtime.service;

import com.arenastrike.realtime.dto.*;
import org.springframework.stereotype.Service;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import com.arenastrike.combat.Weapon;
import com.arenastrike.combat.WeaponFactory;
import com.arenastrike.combat.HitLocation;
import com.arenastrike.combat.PlayerWeaponState;
import com.arenastrike.combat.CombatRaycast;
import com.arenastrike.combat.PlayerHitbox;
import com.arenastrike.combat.RaycastHit;
import com.arenastrike.lobby.repository.LobbyRepository;
import com.arenastrike.lobby.model.GameMap;
import com.arenastrike.player.service.StatsService;
import com.arenastrike.realtime.GameRoom;
import com.arenastrike.realtime.GameRoomManager;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import jakarta.annotation.PreDestroy;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.Locale;

@Service
public class GameStateService {
    private static final double SHOTGUN_FULL_DAMAGE_RANGE = 10.0;
    private static final double SHOTGUN_MAX_RANGE = 200.0;
    private static final double SHOTGUN_MIN_FALLOFF = 0.3;
    private final ConcurrentHashMap<String, GameRoomState> activeRooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, ConcurrentHashMap<Long, Long>> lastClientShotTimestamps =
            new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SessionPlayer> sessions = new ConcurrentHashMap<>();
    private final LobbyRepository lobbyRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StatsService statsService;
    private final GameRoomManager roomManager;

    public GameStateService(LobbyRepository lobbyRepository, SimpMessagingTemplate messagingTemplate,
                            StatsService statsService, GameRoomManager roomManager) {
        this.lobbyRepository = lobbyRepository;
        this.messagingTemplate = messagingTemplate;
        this.statsService = statsService;
        this.roomManager = roomManager;
    }

    public void joinRoom(String roomCode, PlayerState player, String sessionId) {
        String normalized = normalizeRoomCode(roomCode);
        GameRoomState gameRoom = room(normalized, lobbyMap(normalized), lobbyLimit(normalized));
        roomManager.computeIfAbsent(normalized,
                ignored -> new GameRoom(normalized, gameRoom, messagingTemplate,
                        summary -> finishRoom(normalized, summary))).start();
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

    public void enqueueInput(String roomCode, PlayerMovementPacket input) {
        String normalized = normalizeRoomCode(roomCode);
        GameRoom engine = roomManager.get(normalized);
        GameRoomState room = activeRooms.get(normalized);
        if (engine == null || room == null) {
            throw new GameStateConflictException("Room is not active");
        }
        if (room.isFinished()) {
            throw new GameStateConflictException("Match is over");
        }
        if (room.player(input.playerId()) == null) {
            throw new GameStateConflictException("Player is not active in this room");
        }
        engine.enqueueInput(input);
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
        GameRoom engine = roomManager.get(normalizedRoomCode);
        if (engine != null) {
            engine.removePlayer(playerId);
        }
        if (room.isEmpty()) {
            GameRoom removedEngine = roomManager.remove(normalizedRoomCode);
            if (removedEngine != null) {
                removedEngine.close();
            }
            activeRooms.remove(normalizedRoomCode, room);
            lastClientShotTimestamps.remove(normalizedRoomCode);
        } else if (room.snapshot().roomState() == RoomState.ACTIVE) {
            GameRoom activeEngine = roomManager.get(normalizedRoomCode);
            if (activeEngine != null) {
                activeEngine.finishForfeit();
            }
        }
        return Optional.of(new PlayerExitEvent(
                normalizedRoomCode, removed.playerId(), removed.displayName()));
    }

    public Optional<PlayerExitEvent> disconnectSession(String sessionId) {
        SessionPlayer session = sessions.remove(sessionId);
        return session == null ? Optional.empty() : leaveRoom(session.roomCode(), session.playerId());
    }

    public VerifiedShot shoot(String roomCode, String sessionId, ShootCommand command) {
        String normalized = normalizeRoomCode(roomCode);
        GameRoomState room = activeRooms.get(normalized);
        if (room == null) {
            throw new GameStateConflictException("Room is not active");
        }

        SessionPlayer session = sessions.get(sessionId);
        if (session == null || !session.roomCode().equals(normalized)) {
            throw new GameStateConflictException("Shooting session is not authorized");
        }
        synchronized (room) {
            return shootLocked(normalized, room, session.playerId(), command);
        }
    }

    public AmmoStateEvent reload(String roomCode, String sessionId, ReloadCommand command) {
        String normalized = normalizeRoomCode(roomCode);
        GameRoomState room = activeRooms.get(normalized);
        SessionPlayer session = sessions.get(sessionId);
        if (room == null || session == null || !session.roomCode().equals(normalized)) {
            throw new GameStateConflictException("Reload session is not authorized");
        }
        synchronized (room) {
            PlayerState player = room.player(session.playerId());
            if (player == null || !player.currentWeapon().equals(command.weaponId())) {
                throw new GameStateConflictException("Weapon is not equipped by the player");
            }
            Weapon weapon = weapon(command.weaponId());
            return room.beginReload(session.playerId(), weapon, System.currentTimeMillis());
        }
    }

    private VerifiedShot shootLocked(
            String normalized, GameRoomState room, Long shooterId, ShootCommand command) {
        PlayerState shooter = room.player(shooterId);
        if (shooter == null) {
            throw new GameStateConflictException("Shooter must be an active player");
        }
        if (shooter.health() <= 0 || room.snapshot().roomState() != RoomState.ACTIVE) {
            throw new GameStateConflictException("Player cannot shoot in the current match state");
        }
        Weapon weapon = weapon(command.weaponId());
        if (!weapon.name().equals(command.weaponId())
                || !weapon.name().equals(shooter.currentWeapon())) {
            throw new GameStateConflictException("Weapon is not equipped by the shooter");
        }
        long now = System.currentTimeMillis();
        PlayerWeaponState weaponState = room.weaponState(shooterId, weapon);
        if (weaponState.isReloading() || weaponState.reloadInProgress(now)) {
            return new VerifiedShot("SHOT_VERIFIED", String.valueOf(shooterId),
                    weapon.name(), weaponState.currentAmmo(), true, null);
        }
        if (now - weaponState.lastShotTimestamp() < weapon.cooldownMillis()) {
            throw new GameStateConflictException("Weapon is on cooldown");
        }
        ConcurrentHashMap<Long, Long> clientTimestamps = lastClientShotTimestamps
                .computeIfAbsent(normalized, ignored -> new ConcurrentHashMap<>());
        long previousClientTimestamp = clientTimestamps.getOrDefault(shooterId, Long.MIN_VALUE);
        boolean firstClientTimestamp = previousClientTimestamp == Long.MIN_VALUE;
        if (Math.abs(now - command.timestamp()) > 5000
                || (!firstClientTimestamp && command.timestamp() <= previousClientTimestamp)) {
            throw new GameStateConflictException("Shot timestamp is invalid");
        }
        int ammo = weaponState.currentAmmo();
        if (ammo <= 0) {
            weaponState.beginReload();
            return new VerifiedShot("SHOT_VERIFIED", String.valueOf(shooterId),
                    weapon.name(), 0, weaponState.isReloading(), null);
        }
        clientTimestamps.put(shooterId, command.timestamp());
        weaponState.markShot(now);
        if (weaponState.currentAmmo() == 0) {
            weaponState.beginReload();
        }
        int ammoAfterShot = weaponState.currentAmmo();
        double pitch = command.angle();
        double yaw = shooter.rotation().y();
        Vector3 origin = shooter.position();
        Vector3 direction = new Vector3(
                -Math.sin(yaw) * Math.cos(pitch),
                Math.sin(pitch),
                -Math.cos(yaw) * Math.cos(pitch));
        List<PlayerHitbox> hitboxes = room.snapshot().players().stream()
                .map(PlayerHitbox::from).toList();
        if (weapon.id().equals("SHOTGUN")) {
            return shotgunShot(shooter, shooterId, weapon, ammoAfterShot, weaponState.isReloading(),
                    origin, yaw, pitch, hitboxes, room);
        }
        double obstacleDistance = room.firstObstacleDistance(origin, direction, 200);
        RaycastHit hit = CombatRaycast.firstHit(origin, direction, shooter.playerId(), hitboxes,
                obstacleDistance);
        if (hit == null || hit.hitLocation() == HitLocation.MISS
                || hit.hitLocation() == HitLocation.NONE) {
            return new VerifiedShot("SHOT_VERIFIED", String.valueOf(shooterId),
                    weapon.name(), ammoAfterShot, weaponState.isReloading(), null);
        }
        PlayerState target = room.player(hit.targetId());
        if (target == null || target.health() <= 0) {
            return new VerifiedShot("SHOT_VERIFIED", String.valueOf(shooterId),
                    weapon.name(), ammoAfterShot, weaponState.isReloading(), null);
        }
        int damage = weapon.damageFor(hit.hitLocation());
        boolean instantKill = weapon.isInstantKill(hit.hitLocation());
        int currentHp = room.applyDamageAndGetHealth(hit.targetId(), damage, instantKill);
        room.recordDamageDealt(shooterId, damage);
        if (currentHp == 0) {
            room.recordKill(shooterId, hit.hitLocation());
        }
        PlayerHitEvent hitEvent = new PlayerHitEvent("PLAYER_HIT",
                String.valueOf(target.playerId()), String.valueOf(shooter.playerId()),
                damage, hit.hitLocation().name(), currentHp, ammoAfterShot);
        return new VerifiedShot("SHOT_VERIFIED", String.valueOf(shooterId),
                weapon.name(), ammoAfterShot, weaponState.isReloading(), hitEvent);
    }

    private double distanceSquared(Vector3 first, Vector3 second) {
        double dx = first.x() - second.x();
        double dy = first.y() - second.y();
        double dz = first.z() - second.z();
        return dx * dx + dy * dy + dz * dz;
    }

    private VerifiedShot shotgunShot(PlayerState shooter, Long shooterId, Weapon weapon,
                                     int ammoAfterShot, boolean reloading, Vector3 origin, double yaw, double pitch,
                                     List<PlayerHitbox> hitboxes, GameRoomState room) {
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
            RaycastHit hit = CombatRaycast.firstHit(origin, direction,
                    shooter.playerId(), hitboxes, SHOTGUN_MAX_RANGE);
            double obstacleDistance = room.firstObstacleDistance(origin, direction, SHOTGUN_MAX_RANGE);
            if (hit == null || (obstacleDistance < hit.distance())) {
                continue;
            }
            if (hit.hitLocation() == HitLocation.MISS || hit.hitLocation() == HitLocation.NONE) {
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
        PlayerState target = room.player(targetId);
        int currentHp = room.applyDamageAndGetHealth(targetId, damage, false);
        room.recordDamageDealt(shooterId, damage);
        if (currentHp == 0) {
            room.recordKill(shooterId, aggregate.zone());
        }
        PlayerHitEvent hitEvent = new PlayerHitEvent("PLAYER_HIT",
                String.valueOf(targetId), String.valueOf(shooterId), damage,
                aggregate.zone().name(), currentHp, ammoAfterShot);
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
                    90.0 * falloff / WeaponFactory.get("SHOTGUN").pelletCount());
        }

        PelletAggregate add(HitLocation nextZone, double nextDistance, double nextFalloff) {
            HitLocation strongest = WeaponFactory.get("SHOTGUN").damageFor(nextZone)
                    > WeaponFactory.get("SHOTGUN").damageFor(zone) ? nextZone : zone;
            return new PelletAggregate(strongest, Math.min(distance, nextDistance),
                    pellets + 1, totalDamage
                            + 90.0 * nextFalloff / WeaponFactory.get("SHOTGUN").pelletCount());
        }

        double damageScore() {
            return totalDamage;
        }
    }

    private Weapon weapon(String weaponId) {
        try {
            return WeaponFactory.get(weaponId);
        } catch (IllegalArgumentException exception) {
            throw new GameStateConflictException("Unknown weapon");
        }
    }

    public Collection<GameRoomState> activeRooms() {
        return List.copyOf(activeRooms.values());
    }

    @PreDestroy
    public void shutdownEngines() {
        roomManager.getAllRooms().forEach(GameRoom::close);
        roomManager.clear();
    }

    private GameRoomState room(String roomCode, GameMap map, int maxPlayers) {
        return activeRooms.computeIfAbsent(normalizeRoomCode(roomCode),
                ignored -> new GameRoomState(roomCode, map, maxPlayers));
    }

    private GameMap lobbyMap(String roomCode) {
        return lobbyRepository.findByRoomCode(roomCode)
                .map(lobby -> lobby.getMap())
                .orElseThrow(() -> new GameStateConflictException("Lobby is not active"));
    }

    private int lobbyLimit(String roomCode) {
        return lobbyRepository.findByRoomCode(roomCode)
                .map(lobby -> lobby.getPlayerLimit())
                .orElseThrow(() -> new GameStateConflictException("Lobby is not active"));
    }

    private void finishRoom(String roomCode, MatchSummary summary) {
        statsService.recordCompletedMatchAsync(summary, 300);
        lobbyRepository.findByRoomCode(roomCode).ifPresent(lobby -> lobby.resetForNextMatch());
        GameRoom engine = roomManager.remove(roomCode);
        if (engine != null) {
            activeRooms.remove(roomCode);
        }
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
