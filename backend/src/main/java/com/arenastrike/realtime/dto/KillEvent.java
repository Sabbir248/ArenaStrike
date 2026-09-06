package com.arenastrike.realtime.dto;

public record KillEvent(
        Long killerId,
        String killerName,
        Long victimId,
        String victimName,
        String weapon
) {
}
