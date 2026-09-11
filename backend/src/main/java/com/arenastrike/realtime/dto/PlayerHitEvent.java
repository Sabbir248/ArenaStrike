package com.arenastrike.realtime.dto;

public record PlayerHitEvent(
        String event,
        String victimId,
        String attackerId,
        int damage,
        String hitZone,
        int currentHp,
        int currentAmmo
) {
    public PlayerHitEvent {
        if (!"PLAYER_HIT".equals(event)) {
            throw new IllegalArgumentException("event must be PLAYER_HIT");
        }
    }
}
