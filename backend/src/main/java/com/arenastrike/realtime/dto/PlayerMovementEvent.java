package com.arenastrike.realtime.dto;

public record PlayerMovementEvent(PlayerMovementPacket packet) {
    public PlayerMovementEvent {
        if (packet == null) {
            throw new IllegalArgumentException("packet is required");
        }
    }

    public Long playerId() {
        return packet.playerId();
    }
}
