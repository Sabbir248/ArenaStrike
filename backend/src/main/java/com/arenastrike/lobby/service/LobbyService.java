package com.arenastrike.lobby.service;

import com.arenastrike.lobby.dto.*;
import com.arenastrike.lobby.model.*;
import com.arenastrike.lobby.repository.LobbyRepository;
import com.arenastrike.player.model.User;
import com.arenastrike.player.service.UserService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.security.SecureRandom;
import java.util.Locale;

@Service
public class LobbyService {
    private static final int ROOM_CODE_LENGTH = 6;
    private static final String ROOM_ALPHABET = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";

    private final LobbyRepository lobbyRepository;
    private final UserService userService;
    private final SecureRandom secureRandom = new SecureRandom();

    public LobbyService(LobbyRepository lobbyRepository, UserService userService) {
        this.lobbyRepository = lobbyRepository;
        this.userService = userService;
    }

    @Transactional
    public LobbyResponse createLobby(CreateLobbyRequest request) {
        User user = userService.createGuest(request.displayName());
        Lobby lobby = new Lobby(nextAvailableRoomCode(), request.map(), request.playerLimit());
        lobby.addParticipant(new LobbyParticipant(user));
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
        User user = userService.createGuest(request.displayName());
        if (lobby.containsUser(user.getId())) {
            throw new LobbyConflictException("Player is already in this lobby");
        }
        lobby.addParticipant(new LobbyParticipant(user));
        return LobbyResponse.from(lobby);
    }

    @Transactional(readOnly = true)
    public LobbyResponse getLobby(String rawRoomCode) {
        Lobby lobby = lobbyRepository.findByRoomCode(normalizeRoomCode(rawRoomCode))
                .orElseThrow(() -> new LobbyNotFoundException(rawRoomCode));
        return LobbyResponse.from(lobby);
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
