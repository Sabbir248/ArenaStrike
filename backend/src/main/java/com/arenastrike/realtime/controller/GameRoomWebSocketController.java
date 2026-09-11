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

    @MessageMapping("/rooms/{roomCode}/input")
    public void updateState(
            @DestinationVariable String roomCode,
            PlayerInputCommand command) {
        gameStateService.enqueueInput(roomCode, command);
    }

    @MessageMapping("/rooms/{roomCode}/shoot")
    public void shoot(
            @DestinationVariable String roomCode,
            ShootCommand command,
            SimpMessageHeaderAccessor headers) {
        VerifiedShot result = gameStateService.shoot(roomCode, headers.getSessionId(), command);
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/events", result);
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/ammo",
                new AmmoStateEvent("AMMO_STATE", result.attackerId(), result.weaponId(),
                        result.currentAmmo(), result.reloading()));
        if (result.hit() != null) {
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/hit", result.hit());
        }
    }

    @MessageMapping("/rooms/{roomCode}/reload")
    public void reload(
            @DestinationVariable String roomCode,
            ReloadCommand command,
            SimpMessageHeaderAccessor headers) {
        AmmoStateEvent event = gameStateService.reload(roomCode, headers.getSessionId(), command);
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/ammo", event);
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
