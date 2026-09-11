package com.arenastrike.lobby.controller;

import com.arenastrike.lobby.dto.*;
import com.arenastrike.lobby.service.LobbyService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
@Validated
public class LobbyController {
    private final LobbyService lobbyService;

    public LobbyController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @PostMapping("/lobby/create")
    @ResponseStatus(HttpStatus.CREATED)
    public LobbyResponse createLobby(@Valid @RequestBody CreateLobbyRequest request) {
        return lobbyService.createLobby(request);
    }

    @GetMapping("/lobby/{roomCode}")
    public LobbyResponse getLobby(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9]{6}") String roomCode) {
        return lobbyService.getLobby(roomCode);
    }

    @PostMapping("/lobby/{roomCode}/join")
    public LobbyResponse joinLobby(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9]{6}") String roomCode,
            @Valid @RequestBody JoinLobbyRequest request) {
        return lobbyService.joinLobby(roomCode, request);
    }
}
