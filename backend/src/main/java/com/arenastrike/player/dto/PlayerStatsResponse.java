package com.arenastrike.player.dto;

public record PlayerStatsResponse(
        Long userId,
        String username,
        long totalMatches,
        long wins,
        long totalKills,
        long totalDeaths,
        double kdr,
        long headshotKills
) {
}
