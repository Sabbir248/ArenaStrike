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

    @MessageMapping("/lobby/{roomCode}/start")
    public void startMatch(
            @DestinationVariable String roomCode,
            SimpMessageHeaderAccessor headers) {
        Long authenticatedPlayerId = (Long) headers.getSessionAttributes().get("playerId");
        if (authenticatedPlayerId == null) return;
        
        gameStateService.startMatchIfHost(roomCode, authenticatedPlayerId);
    }

    @MessageMapping("/rooms/{roomCode}/join")
    public void join(
            @DestinationVariable String roomCode,
            JoinRoomCommand command,
            SimpMessageHeaderAccessor headers) {
        Long authenticatedPlayerId = (Long) headers.getSessionAttributes().get("playerId");
        if (authenticatedPlayerId == null) return;
        
        // Override client spoofable ID with authenticated ID
        PlayerState securePlayer = new PlayerState(authenticatedPlayerId, command.player().displayName(), 
                command.player().position(), command.player().rotation(), command.player().health(), 
                command.player().currentWeapon(), command.player().state());
        
        RoomGameState snapshot = gameStateService.joinRoom(roomCode, securePlayer, headers.getSessionId());
        messagingTemplate.convertAndSend("/topic/room/" + roomCode, snapshot);
    }

    @MessageMapping("/rooms/{roomCode}/input")
    public void updateState(
            @DestinationVariable String roomCode,
            PlayerMovementPacket command,
            SimpMessageHeaderAccessor headers) {
        Long authenticatedPlayerId = (Long) headers.getSessionAttributes().get("playerId");
        if (authenticatedPlayerId == null) return;

        PlayerMovementPacket secureCommand = new PlayerMovementPacket(
                authenticatedPlayerId, command.x(), command.y(), command.z(),
                command.velocityX(), command.velocityY(), command.state(),
                command.isJumping(), command.rotation(), command.currentWeapon());
        
        gameStateService.enqueueInput(roomCode, secureCommand);
    }

    @MessageMapping("/rooms/{roomCode}/shoot")
    public void shoot(
            @DestinationVariable String roomCode,
            ShootCommand command,
            SimpMessageHeaderAccessor headers) {
        Long authenticatedPlayerId = (Long) headers.getSessionAttributes().get("playerId");
        if (authenticatedPlayerId == null) return;

        // ShootCommand does not contain playerId, the server looks it up by sessionId inside gameStateService!
        // But to be completely secure, we should enforce the authenticatedPlayerId if the service required it.
        VerifiedShot result = gameStateService.shoot(roomCode, headers.getSessionId(), command);
        if (result == null) return;
        
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/events", result);
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/ammo",
                new AmmoStateEvent("AMMO_STATE", result.attackerId(), result.weaponId(),
                        result.currentAmmo(), result.reloading()));
    }

    @MessageMapping("/rooms/{roomCode}/reload")
    public void reload(
            @DestinationVariable String roomCode,
            ReloadCommand command,
            SimpMessageHeaderAccessor headers) {
        Long authenticatedPlayerId = (Long) headers.getSessionAttributes().get("playerId");
        if (authenticatedPlayerId == null) return;

        AmmoStateEvent event = gameStateService.reload(roomCode, headers.getSessionId(), command);
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/ammo", event);
    }

    @MessageMapping("/rooms/{roomCode}/leave")
    public void leave(
            @DestinationVariable String roomCode,
            SimpMessageHeaderAccessor headers) {
        Long authenticatedPlayerId = (Long) headers.getSessionAttributes().get("playerId");
        if (authenticatedPlayerId == null) return;

        gameStateService.leaveRoom(roomCode, authenticatedPlayerId).ifPresent(event ->
                messagingTemplate.convertAndSend("/topic/rooms/" + roomCode + "/exits", event));
    }
}
