package com.arenastrike.realtime.dto;

import java.util.List;

public record RoomGameState(
        String roomCode,
        long serverTick,
        List<PlayerState> players,
        RoomState roomState,
        String map,
        int maxPlayers,
        long remainingSeconds
) {
    public RoomGameState {
        players = List.copyOf(players);
    }
}
