package com.arenastrike.lobby.controller;

import com.arenastrike.lobby.dto.LobbyReadyCommand;
import com.arenastrike.lobby.service.LobbyService;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.stereotype.Controller;

@Controller
public class LobbyWebSocketController {
    private final LobbyService lobbyService;

    public LobbyWebSocketController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @MessageMapping("/lobby/{roomCode}/ready")
    public void toggleReady(
            @DestinationVariable String roomCode,
            @Payload LobbyReadyCommand command,
            SimpMessageHeaderAccessor headers) {
        lobbyService.toggleReady(roomCode, command.userId(), command.isReady());
    }

    @MessageMapping("/lobby/{roomCode}/launch")
    public void launch(
            @DestinationVariable String roomCode,
            @Payload Long hostId,
            SimpMessageHeaderAccessor headers) {
        lobbyService.hostLaunch(roomCode, hostId);
    }
}
