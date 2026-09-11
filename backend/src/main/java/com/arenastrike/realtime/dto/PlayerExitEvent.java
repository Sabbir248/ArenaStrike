package com.arenastrike.realtime.dto;

public record PlayerExitEvent(String event, String roomCode, Long playerId, String displayName) {
    public PlayerExitEvent(String roomCode, Long playerId, String displayName) {
        this("PLAYER_LEFT", roomCode, playerId, displayName);
    }
}
