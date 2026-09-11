package com.arenastrike.realtime.dto;

import java.util.List;

public record MatchSummary(
        String type,
        String roomId,
        RoomState state,
        Long winnerId,
        List<Long> winnerIds,
        String winnerName,
        int winnerKills,
        String map,
        List<PlayerResult> results
) {
    public record PlayerResult(Long playerId, String displayName, int kills, int deaths,
                               int headshotKills, int damageDealt, String weaponUsed) {
    }
}
