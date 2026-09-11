package com.arenastrike.realtime.dto;

public record PlayerInputEvent(PlayerInputCommand input) {
    public PlayerInputEvent {
        if (input == null) {
            throw new IllegalArgumentException("input is required");
        }
    }

    public Long playerId() {
        return input.playerId();
    }
}
