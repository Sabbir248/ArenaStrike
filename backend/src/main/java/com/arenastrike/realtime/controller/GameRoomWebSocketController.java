package com.arenastrike.realtime.controller;

import com.arenastrike.realtime.dto.*;
import com.arenastrike.realtime.service.GameStateService;
import org.springframework.messaging.handler.annotation.*;
import org.springframework.stereotype.Controller;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;

@Controller
public class GameRoomWebSocketController {
    private final GameStateService gameStateService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameRoomWebSocketController(GameStateService gameStateService, SimpMessagingTemplate messagingTemplate) {
        this.gameStateService = gameStateService;
        this.messagingTemplate = messagingTemplate;
    }

    @MessageMapping("/rooms/{roomCode}/join")
    public void join(
            @DestinationVariable String roomCode,
            JoinRoomCommand command,
            SimpMessageHeaderAccessor headers) {
        gameStateService.joinRoom(roomCode, command.player(), headers.getSessionId());
    }

    @MessageMapping("/rooms/{roomCode}/state")
    public void updateState(
            @DestinationVariable String roomCode,
            UpdatePlayerStateCommand command) {
        gameStateService.updatePlayer(roomCode, command);
    }

    @MessageMapping("/rooms/{roomCode}/shoot")
    public void shoot(
            @DestinationVariable String roomCode,
            ShootCommand command) {
        gameStateService.shoot(roomCode, command).ifPresent(event ->
                messagingTemplate.convertAndSend("/topic/rooms/" + roomCode + "/kills", event));
    }

    @MessageMapping("/rooms/{roomCode}/leave")
    public void leave(
            @DestinationVariable String roomCode,
            @Payload Long playerId,
            SimpMessageHeaderAccessor headers) {
        gameStateService.leaveRoom(roomCode, playerId).ifPresent(event ->
                messagingTemplate.convertAndSend("/topic/rooms/" + roomCode + "/exits", event));
    }
}
