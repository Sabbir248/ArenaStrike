package com.arenastrike.lobby.dto;

import com.arenastrike.lobby.model.*;
import java.time.Instant;
import java.util.List;

public record LobbyResponse(
        String roomCode,
        String roomName,
        GameMap map,
        int maxPlayers,
        int currentPlayers,
        LobbyStatus status,
        Instant createdAt,
        List<PlayerResponse> players,
        /**
         * The database-assigned ID of the player who just created or joined this lobby.
         * Null when the response is returned by a read-only endpoint (e.g. GET /lobby/{code}).
         * The client MUST use this value as its authoritative playerId and must NOT
         * fall back to a locally-generated random number.
         */
        Long myPlayerId,
        Long hostId
) {
    public record PlayerResponse(Long userId, String displayName, Instant joinedAt, boolean isReady) {}

    /**
     * Builds a response that includes the DB-assigned ID of the acting player.
     * Used by createLobby and joinLobby so the client can track its own identity.
     */
    public static LobbyResponse from(Lobby lobby, Long myPlayerId) {
        List<PlayerResponse> players = lobby.getParticipants().stream()
                .map(p -> new PlayerResponse(
                        p.getUser().getId(),
                        p.getUser().getDisplayName(),
                        p.getJoinedAt(),
                        p.isReady()))
                .toList();
        Long hostId = players.isEmpty() ? null : players.get(0).userId();
        return new LobbyResponse(
                lobby.getRoomCode(), lobby.getRoomName(), lobby.getMap(), lobby.getPlayerLimit(),
                players.size(), lobby.getStatus(), lobby.getCreatedAt(), players, myPlayerId, hostId);
    }

    /**
     * Convenience overload for read-only endpoints that do not have a current actor
     * (e.g. GET /lobby/{code}). myPlayerId is null in the response.
     */
    public static LobbyResponse from(Lobby lobby) {
        return from(lobby, null);
    }
}
