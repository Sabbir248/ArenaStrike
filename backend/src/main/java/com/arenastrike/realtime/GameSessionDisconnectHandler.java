package com.arenastrike.realtime;

import com.arenastrike.realtime.service.GameStateService;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

@Component
public class GameSessionDisconnectHandler {
    private final GameStateService gameStateService;
    private final SimpMessagingTemplate messagingTemplate;

    public GameSessionDisconnectHandler(
            GameStateService gameStateService,
            SimpMessagingTemplate messagingTemplate) {
        this.gameStateService = gameStateService;
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onDisconnect(SessionDisconnectEvent event) {
        gameStateService.disconnectSession(event.getSessionId()).ifPresent(exit -> {
            messagingTemplate.convertAndSend(
                    "/topic/room/" + exit.roomCode() + "/events", exit);
        });
    }
}
