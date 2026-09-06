package com.arenastrike.realtime.dto;

public record PlayerExitEvent(String roomCode, Long playerId, String displayName) {
}
