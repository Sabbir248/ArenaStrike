package com.arenastrike.lobby.service;

import com.arenastrike.lobby.dto.*;
import com.arenastrike.lobby.model.*;
import com.arenastrike.lobby.repository.LobbyRepository;
import com.arenastrike.player.model.Player;
import com.arenastrike.player.service.PlayerService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import java.security.SecureRandom;
import java.util.Locale;

@Service
public class LobbyService {
    private static final int ROOM_CODE_LENGTH = 6;
    private static final String ROOM_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final LobbyRepository lobbyRepository;
    private final PlayerService playerService;
    private final SimpMessagingTemplate messagingTemplate;
    private final SecureRandom secureRandom = new SecureRandom();

    public LobbyService(LobbyRepository lobbyRepository, PlayerService playerService, SimpMessagingTemplate messagingTemplate) {
        this.lobbyRepository = lobbyRepository;
        this.playerService = playerService;
        this.messagingTemplate = messagingTemplate;
    }

    @Transactional
    public LobbyResponse createLobby(CreateLobbyRequest request) {
        Player player = playerService.createGuest(request.displayName());
        Lobby lobby = new Lobby(nextAvailableRoomCode(), request.roomName(), request.map(), request.playerLimit());
        lobby.addParticipant(new LobbyParticipant(player));
        return LobbyResponse.from(lobbyRepository.save(lobby));
    }

    /**
     * The pessimistic database lock serializes all joins for a room. The
     * capacity check and participant insert therefore happen atomically.
     */
    @Transactional
    public LobbyResponse joinLobby(String rawRoomCode, JoinLobbyRequest request) {
        String roomCode = normalizeRoomCode(rawRoomCode);
        Lobby lobby = lobbyRepository.findByRoomCodeForUpdate(roomCode)
                .orElseThrow(() -> new LobbyNotFoundException(roomCode));
        Player player = playerService.createGuest(request.displayName());
        if (lobby.containsUser(player.getId())) {
            throw new LobbyConflictException("Player is already in this lobby");
        }
        lobby.addParticipant(new LobbyParticipant(player));
        Lobby saved = lobbyRepository.save(lobby);
        broadcastLobbySync(saved);
        return LobbyResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public LobbyResponse getLobby(String rawRoomCode) {
        Lobby lobby = lobbyRepository.findByRoomCode(normalizeRoomCode(rawRoomCode))
                .orElseThrow(() -> new LobbyNotFoundException(rawRoomCode));
        return LobbyResponse.from(lobby);
    }

    @Transactional
    public void toggleReady(String rawRoomCode, Long playerId, boolean isReady) {
        Lobby lobby = lobbyRepository.findByRoomCodeForUpdate(normalizeRoomCode(rawRoomCode))
                .orElseThrow(() -> new LobbyNotFoundException(rawRoomCode));
        lobby.getParticipants().stream()
                .filter(p -> p.getUser().getId().equals(playerId))
                .findFirst()
                .ifPresent(p -> p.setReady(isReady));
        broadcastLobbySync(lobbyRepository.save(lobby));
    }

    @Transactional
    public void hostLaunch(String rawRoomCode, Long hostId) {
        Lobby lobby = lobbyRepository.findByRoomCodeForUpdate(normalizeRoomCode(rawRoomCode))
                .orElseThrow(() -> new LobbyNotFoundException(rawRoomCode));
        LobbyParticipant host = lobby.getParticipants().isEmpty() ? null : lobby.getParticipants().get(0);
        if (host == null || !host.getUser().getId().equals(hostId)) {
            throw new LobbyConflictException("Only the host can launch the game.");
        }
        messagingTemplate.convertAndSend("/topic/lobby/" + lobby.getRoomCode() + "/launch", new LobbyLaunchCommand());
    }

    public void broadcastLobbySync(Lobby lobby) {
        var players = lobby.getParticipants().stream()
                .map(p -> new LobbySyncEvent.PlayerStatus(p.getUser().getId(), p.getUser().getDisplayName(), p.isReady()))
                .toList();
        LobbySyncEvent event = new LobbySyncEvent("LOBBY_SYNC", lobby.getRoomCode(), players.size(), lobby.getPlayerLimit(), players);
        messagingTemplate.convertAndSend("/topic/lobby/" + lobby.getRoomCode(), event);
    }

    private String nextAvailableRoomCode() {
        for (int attempt = 0; attempt < 10; attempt++) {
            StringBuilder code = new StringBuilder(ROOM_CODE_LENGTH);
            for (int i = 0; i < ROOM_CODE_LENGTH; i++) {
                code.append(ROOM_ALPHABET.charAt(secureRandom.nextInt(ROOM_ALPHABET.length())));
            }
            String candidate = code.toString();
            if (!lobbyRepository.existsByRoomCode(candidate)) {
                return candidate;
            }
        }
        throw new LobbyConflictException("Could not allocate a unique room code");
    }

    private String normalizeRoomCode(String roomCode) {
        if (roomCode == null || !roomCode.trim().matches("[A-Za-z0-9]{6}")) {
            throw new LobbyNotFoundException(roomCode);
        }
        return roomCode.trim().toUpperCase(Locale.ROOT);
    }
}
