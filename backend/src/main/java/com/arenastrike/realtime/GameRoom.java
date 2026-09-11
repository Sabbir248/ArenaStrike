package com.arenastrike.realtime;

import com.arenastrike.realtime.dto.*;
import com.arenastrike.realtime.service.GameRoomState;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.messaging.MessagingException;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public final class GameRoom implements AutoCloseable {
    private static final long TICK_MILLIS = 50L;
    private static final double TICK_SECONDS = TICK_MILLIS / 1000.0;
    private static final double WALK_SPEED = 7.0;
    private static final double GRAVITY = 18.0;
    private static final Logger LOGGER = LoggerFactory.getLogger(GameRoom.class);

    private final String roomId;
    private final GameRoomState room;
    private final SimpMessagingTemplate messagingTemplate;
    private final ConcurrentLinkedQueue<PlayerMovementEvent> inputQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentHashMap<Long, Double> verticalVelocity = new ConcurrentHashMap<>();
    private final ScheduledExecutorService executor;
    private ScheduledFuture<?> task;
    private final AtomicInteger countdown = new AtomicInteger(300);
    private int tickCounter = 0;
    private final java.util.function.Consumer<MatchSummary> finishCallback;
    private final AtomicBoolean isMatchTerminated = new AtomicBoolean(false);

    public GameRoom(String roomId, GameRoomState room, SimpMessagingTemplate messagingTemplate,
                          java.util.function.Consumer<MatchSummary> finishCallback) {
        this.roomId = roomId;
        this.room = room;
        this.messagingTemplate = messagingTemplate;
        this.finishCallback = finishCallback;
        this.executor = Executors.newSingleThreadScheduledExecutor(runnable -> {
            Thread thread = new Thread(runnable, "arena-room-" + roomId);
            thread.setDaemon(false);
            return thread;
        });
    }

    public synchronized void start() {
        if (task == null || task.isCancelled()) {
            task = executor.scheduleAtFixedRate(this::tick, 0, TICK_MILLIS, TimeUnit.MILLISECONDS);
        }
    }

    public void enqueueInput(PlayerMovementPacket input) {
        if (isMatchTerminated.get() || room.isFinished()) {
            return;
        }
        inputQueue.offer(new PlayerMovementEvent(input));
    }

    public void removePlayer(Long playerId) {
        inputQueue.removeIf(event -> event.playerId().equals(playerId));
        verticalVelocity.remove(playerId);
    }

    private void tick() {
        try {
            if (isMatchTerminated.get() || room.isFinished()) {
                cancelTick();
                return;
            }
            PlayerMovementEvent action;
            while ((action = inputQueue.poll()) != null) {
                PlayerMovementPacket input = action.packet();
                Long playerId = input.playerId();
                PlayerState current = room.player(playerId);
                if (current != null && current.health() > 0) {
                    room.applyMovement(playerId, input, TICK_SECONDS, verticalVelocity);
                }
            }
            room.completeReloads(System.currentTimeMillis()).forEach(event ->
                    messagingTemplate.convertAndSend("/topic/room/" + roomId + "/ammo", event));
            room.activateIfStarting();
            RoomGameState snapshot = room.snapshot(countdown.get());
            messagingTemplate.convertAndSend("/topic/room/" + roomId, snapshot);
            if (snapshot.roomState() == RoomState.ACTIVE) {
                if (++tickCounter >= 20) {
                    tickCounter = 0;
                    int currentTimer = countdown.decrementAndGet();
                    messagingTemplate.convertAndSend("/topic/room/" + roomId,
                            new RoomTimerEvent(roomId, currentTimer));
                    
                    if (currentTimer <= 0) {
                        finishMatch(room.forceFinishMatch());
                    }
                }
            } else {
                finishMatch(room.finishIfForfeit());
            }
        } catch (MessagingException exception) {
            LOGGER.warn("Could not publish state for room {}", roomId, exception);
        } catch (RuntimeException exception) {
            LOGGER.error("Authoritative tick failed for room {}", roomId, exception);
        }
    }

    public void finishForfeit() {
        finishMatch(room.finishIfForfeit());
    }

    private void finishMatch(MatchSummary summary) {
        if (summary == null || !isMatchTerminated.compareAndSet(false, true)) {
            return;
        }
        cancelTick();
        inputQueue.clear();
        try {
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/game-over", summary);
        } catch (MessagingException exception) {
            LOGGER.warn("Could not publish game-over for room {}", roomId, exception);
        } finally {
            try {
                finishCallback.accept(summary);
            } finally {
                close();
            }
        }
    }

    private synchronized void cancelTick() {
        if (task != null) {
            task.cancel(false);
        }
    }

    @Override
    public synchronized void close() {
        isMatchTerminated.set(true);
        cancelTick();
        executor.shutdown();
        if (Thread.currentThread().getName().equals("arena-room-" + roomId)) {
            return;
        }
        try {
            if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException exception) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
