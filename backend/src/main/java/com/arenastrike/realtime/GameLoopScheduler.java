package com.arenastrike.realtime;

import com.arenastrike.realtime.dto.*;
import com.arenastrike.realtime.service.GameStateService;
import org.springframework.context.SmartLifecycle;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.concurrent.*;

/**
 * Publishes authoritative room snapshots at 20 ticks per second.
 *
 * <p>The loop only reads immutable snapshots from the concurrent state store;
 * it never performs blocking database work on the tick thread.</p>
 */
@Component
public class GameLoopScheduler implements SmartLifecycle {
    private static final long TICK_INTERVAL_MILLIS = 50L;
    private static final Logger LOGGER = LoggerFactory.getLogger(GameLoopScheduler.class);

    private final GameStateService gameStateService;
    private final SimpMessagingTemplate messagingTemplate;
    private final ScheduledExecutorService executor;
    private volatile boolean running;

    public GameLoopScheduler(
            GameStateService gameStateService,
            SimpMessagingTemplate messagingTemplate) {
        this.gameStateService = gameStateService;
        this.messagingTemplate = messagingTemplate;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "arena-strike-game-loop");
            thread.setDaemon(false);
            return thread;
        });
    }

    @Override
    public synchronized void start() {
        if (running) {
            return;
        }
        running = true;
        executor.scheduleAtFixedRate(this::broadcastSnapshots, 0L,
                TICK_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
    }

    private void broadcastSnapshots() {
        gameStateService.activeRooms().forEach(room -> {
            try {
                MatchOverEvent matchOver = room.advanceMatch();
                if (matchOver != null) {
                    messagingTemplate.convertAndSend(
                            "/topic/rooms/" + room.snapshot().roomCode() + "/match-over", matchOver);
                }
                RoomGameState snapshot = room.snapshot();
                messagingTemplate.convertAndSend(
                        "/topic/rooms/" + snapshot.roomCode() + "/state", snapshot);
            } catch (MessagingException exception) {
                // A broker failure must not terminate the fixed-rate scheduler.
                LOGGER.warn("Could not publish state for room {}", room, exception);
            }
        });
    }

    @Override
    public synchronized void stop() {
        running = false;
        executor.shutdown();
    }

    @Override
    public boolean isRunning() {
        return running;
    }

    @Override
    public int getPhase() {
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
