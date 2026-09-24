package com.arenastrike.lobby.controller;

import com.arenastrike.lobby.dto.*;
import com.arenastrike.lobby.service.LobbyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api")
@Validated
public class LobbyController {
    private static final Logger log = LoggerFactory.getLogger(LobbyController.class);
    private final LobbyService lobbyService;
    private final com.arenastrike.auth.JwtService jwtService;

    public LobbyController(LobbyService lobbyService, com.arenastrike.auth.JwtService jwtService) {
        this.lobbyService = lobbyService;
        this.jwtService = jwtService;
    }

    private Long getPlayerIdOrThrow(String authHeader) {
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            if (jwtService.isTokenValid(token)) {
                return jwtService.extractPlayerId(token);
            }
        }
        throw new org.springframework.web.server.ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing token");
    }

    @PostMapping("/lobby/create")
    @ResponseStatus(HttpStatus.CREATED)
    public LobbyResponse createLobby(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody CreateLobbyRequest request) {
        return lobbyService.createLobby(request, getPlayerIdOrThrow(authHeader));
    }

    @GetMapping("/lobby/{roomCode}")
    public LobbyResponse getLobby(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9]{6}") String roomCode) {
        return lobbyService.getLobby(roomCode);
    }

    @PostMapping("/lobby/{roomCode}/join")
    public org.springframework.http.ResponseEntity<?> joinLobby(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9]{6}") String roomCode,
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @Valid @RequestBody JoinLobbyRequest request) {
        try {
            Long playerId = getPlayerIdOrThrow(authHeader);
            LobbyResponse response = lobbyService.joinLobby(roomCode, request, playerId);
            return org.springframework.http.ResponseEntity.ok(response);
        } catch (com.arenastrike.lobby.service.LobbyNotFoundException ex) {
            return org.springframework.http.ResponseEntity.status(HttpStatus.NOT_FOUND).body(java.util.Map.of("error", "Room not found"));
        } catch (com.arenastrike.lobby.service.LobbyConflictException | IllegalStateException ex) {
            return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("error", ex.getMessage()));
        } catch (org.springframework.web.server.ResponseStatusException ex) {
            return org.springframework.http.ResponseEntity.status(ex.getStatusCode()).body(java.util.Map.of("error", ex.getReason()));
        } catch (IllegalArgumentException ex) {
            return org.springframework.http.ResponseEntity.status(HttpStatus.BAD_REQUEST).body(java.util.Map.of("error", "Invalid request parameters"));
        } catch (Exception ex) {
            log.error("Join room failed", ex);
            return org.springframework.http.ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(java.util.Map.of("error", "An unexpected error occurred"));
        }
    }
}
