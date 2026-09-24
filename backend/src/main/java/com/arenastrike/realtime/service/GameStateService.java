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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import jakarta.annotation.PreDestroy;
import org.springframework.transaction.annotation.Transactional;
import java.util.Optional;
import java.util.Locale;

@Service
public class GameStateService {
    private final ConcurrentHashMap<String, GameRoomBundle> activeRooms = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, SessionPlayer> sessions = new ConcurrentHashMap<>();
    private final LobbyRepository lobbyRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final StatsService statsService;

    public GameStateService(LobbyRepository lobbyRepository, SimpMessagingTemplate messagingTemplate,
                            StatsService statsService) {
        this.lobbyRepository = lobbyRepository;
        this.messagingTemplate = messagingTemplate;
        this.statsService = statsService;
    }
    
    public RoomGameState joinRoom(String roomCode, PlayerState player, String sessionId) {
        String normalized = normalizeRoomCode(roomCode);
        GameRoomBundle bundle = activeRooms.computeIfAbsent(normalized, ignored -> {
            GameRoomState state = new GameRoomState(normalized, lobbyMap(normalized), lobbyLimit(normalized));
            GameRoom engine = new GameRoom(normalized, state, messagingTemplate,
                    summary -> statsService.recordCompletedMatchAsync(summary, 300),
                    summary -> finishRoom(normalized, summary));
            engine.start();
            return new GameRoomBundle(state, engine);
        });
        GameRoomState gameRoom = bundle.state();
        gameRoom.addPlayer(player);
        if (sessionId != null && !sessionId.isBlank()) {
            sessions.put(sessionId, new SessionPlayer(normalized, player.playerId()));
        }
        return gameRoom.snapshot();
    }

    @Transactional(readOnly = true)
    public void startMatchIfHost(String roomCode, Long playerId) {
        String normalized = normalizeRoomCode(roomCode);
        com.arenastrike.lobby.model.Lobby lobby = lobbyRepository.findByRoomCode(normalized).orElse(null);
        if (lobby != null && !lobby.getParticipants().isEmpty() && 
            lobby.getParticipants().get(0).getUser().getId().equals(playerId)) {
            
            GameRoomBundle bundle = activeRooms.get(normalized);
            if (bundle != null) {
                bundle.state().manualStart();
                messagingTemplate.convertAndSend("/topic/room/" + normalized + "/start",
                        Map.of("event", "MATCH_START"));
                messagingTemplate.convertAndSend("/topic/room/" + normalized + "/events",
                        Map.of("event", "MATCH_STARTED")); // Keep old one just in case
            }
        }
    }

    public void updatePlayer(String roomCode, UpdatePlayerStateCommand update) {
        GameRoomBundle bundle = activeRooms.get(normalizeRoomCode(roomCode));
        if (bundle == null) {
            throw new GameStateConflictException("Room is not active");
        }
        bundle.state().updatePlayer(update);
    }

    public void enqueueInput(String roomCode, PlayerMovementPacket input) {
        String normalized = normalizeRoomCode(roomCode);
        GameRoomBundle bundle = activeRooms.get(normalized);
        if (bundle == null) {
            throw new GameStateConflictException("Room is not active");
        }
        GameRoomState room = bundle.state();
        if (room.isFinished()) {
            throw new GameStateConflictException("Match is over");
        }
        if (room.player(input.playerId()) == null) {
            throw new GameStateConflictException("Player is not active in this room");
        }
        bundle.engine().enqueueInput(input);
    }

    public Optional<PlayerExitEvent> leaveRoom(String roomCode, Long playerId) {
        String normalizedRoomCode = normalizeRoomCode(roomCode);
        GameRoomBundle bundle = activeRooms.get(normalizedRoomCode);
        if (bundle == null) {
            return Optional.empty();
        }
        GameRoomState room = bundle.state();
        PlayerState removed = room.removePlayerAndReturn(playerId);
        if (removed == null) {
            return Optional.empty();
        }
        sessions.entrySet().removeIf(entry -> entry.getValue().matches(normalizedRoomCode, playerId));
        bundle.engine().removePlayer(playerId);
        if (room.isEmpty()) {
            if (activeRooms.remove(normalizedRoomCode, bundle)) {
                bundle.engine().close();
            }
        } else if (room.snapshot().roomState() == RoomState.ACTIVE) {
            bundle.engine().finishForfeit();
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
        GameRoomBundle bundle = activeRooms.get(normalized);
        if (bundle == null) {
            throw new GameStateConflictException("Room is not active");
        }
        GameRoomState room = bundle.state();

        SessionPlayer session = sessions.get(sessionId);
        if (session == null || !session.roomCode().equals(normalized)) {
            throw new GameStateConflictException("Shooting session is not authorized");
        }
        
        Weapon weapon = weapon(command.weaponId());
        VerifiedShot result = room.shoot(session.playerId(), command, weapon);
        
        if (result.hit() != null && result.hit().currentHp() == 0) {
            Long targetId = Long.parseLong(result.hit().victimId());
            PlayerState target = room.player(targetId);
            PlayerState shooter = room.player(session.playerId());
            if (target != null && shooter != null) {
                messagingTemplate.convertAndSend("/topic/rooms/" + normalized + "/kills",
                        new KillEvent(shooter.playerId(), shooter.displayName(),
                                targetId, target.displayName(), weapon.name()));
            }
            bundle.engine().scheduleRespawn(targetId);
        }
        
        return result;
    }

    public AmmoStateEvent reload(String roomCode, String sessionId, ReloadCommand command) {
        String normalized = normalizeRoomCode(roomCode);
        GameRoomBundle bundle = activeRooms.get(normalized);
        SessionPlayer session = sessions.get(sessionId);
        if (bundle == null || session == null || !session.roomCode().equals(normalized)) {
            throw new GameStateConflictException("Reload session is not authorized");
        }
        GameRoomState room = bundle.state();
        PlayerState player = room.player(session.playerId());
        if (player == null || !player.currentWeapon().equals(command.weaponId())) {
            throw new GameStateConflictException("Weapon is not equipped by the player");
        }
        Weapon weapon = weapon(command.weaponId());
        return room.beginReload(session.playerId(), weapon, System.currentTimeMillis());
    }

    private Weapon weapon(String weaponId) {
        try {
            return WeaponFactory.get(weaponId);
        } catch (IllegalArgumentException exception) {
            throw new GameStateConflictException("Unknown weapon");
        }
    }

    public Collection<GameRoomState> activeRooms() {
        return activeRooms.values().stream().map(GameRoomBundle::state).toList();
    }

    @PreDestroy
    public void shutdownEngines() {
        activeRooms.values().forEach(bundle -> bundle.engine().close());
        activeRooms.clear();
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
        resetLobby(roomCode);
        activeRooms.remove(roomCode);
    }

    @Transactional
    private void resetLobby(String roomCode) {
        lobbyRepository.findByRoomCode(roomCode).ifPresent(lobby -> {
            lobby.resetForNextMatch();
            lobbyRepository.save(lobby);
        });
    }

    private String normalizeRoomCode(String roomCode) {
        if (roomCode == null || !roomCode.trim().matches("[A-Za-z0-9]{6}")) {
            throw new GameStateConflictException("Invalid room code");
        }
        return roomCode.trim().toUpperCase(java.util.Locale.ROOT);
    }

    private record GameRoomBundle(GameRoomState state, GameRoom engine) {
    }

    private record SessionPlayer(String roomCode, Long playerId) {
        boolean matches(String roomCode, Long playerId) {
            return this.roomCode.equals(roomCode) && this.playerId.equals(playerId);
        }
    }
}
