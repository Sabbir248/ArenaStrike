package com.arenastrike.realtime.controller;

import com.arenastrike.realtime.service.GameStateService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class WebSocketEventListener {

    private final GameStateService gameStateService;
    private final SimpMessagingTemplate messagingTemplate;

    public WebSocketEventListener(GameStateService gameStateService, SimpMessagingTemplate messagingTemplate) {
        this.gameStateService = gameStateService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void handleWebSocketDisconnectListener(SessionDisconnectEvent event) {
        SimpMessageHeaderAccessor headers = SimpMessageHeaderAccessor.wrap(event.getMessage());
        String sessionId = headers.getSessionId();

        gameStateService.disconnectSession(sessionId).ifPresent(playerExitEvent -> {
            messagingTemplate.convertAndSend("/topic/rooms/" + playerExitEvent.roomCode() + "/exits", playerExitEvent);
            // Additionally broadcast to /topic/room/{roomId} if necessary to clear state, but /exits is standard here.
        });
    }
}
