package com.arenastrike.lobby.dto;

import java.util.List;

public record LobbySyncEvent(
        String type,
        String roomCode,
        int currentPlayers,
        int maxPlayers,
        List<PlayerStatus> players
) {
    public record PlayerStatus(Long userId, String displayName, boolean isReady) {}
}
