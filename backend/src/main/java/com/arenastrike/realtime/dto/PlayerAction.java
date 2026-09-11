package com.arenastrike.realtime.dto;

public record PlayerAction(PlayerInputCommand input) {
    public PlayerAction {
        if (input == null) {
            throw new IllegalArgumentException("input is required");
        }
    }

    public Long playerId() {
        return input.playerId();
    }
}
