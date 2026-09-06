package com.arenastrike.lobby.dto;

import com.arenastrike.lobby.model.*;
import java.time.Instant;
import java.util.List;

public record LobbyResponse(
        String roomCode,
        GameMap map,
        int playerLimit,
        int currentPlayers,
        LobbyStatus status,
        Instant createdAt,
        List<PlayerResponse> players
) {
    public record PlayerResponse(Long userId, String displayName, Instant joinedAt) {}

    public static LobbyResponse from(Lobby lobby) {
        List<PlayerResponse> players = lobby.getParticipants().stream()
                .map(player -> new PlayerResponse(
                        player.getUser().getId(),
                        player.getUser().getDisplayName(),
                        player.getJoinedAt()))
                .toList();
        return new LobbyResponse(
                lobby.getRoomCode(), lobby.getMap(), lobby.getPlayerLimit(),
                players.size(), lobby.getStatus(), lobby.getCreatedAt(), players);
    }
}
