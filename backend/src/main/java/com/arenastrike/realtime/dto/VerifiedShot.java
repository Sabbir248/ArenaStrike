package com.arenastrike.realtime.dto;

public record VerifiedShot(
        String event,
        String attackerId,
        String weaponId,
        int currentAmmo,
        boolean reloading,
        PlayerHitEvent hit
) {
    public VerifiedShot {
        if (!"SHOT_VERIFIED".equals(event)) {
            throw new IllegalArgumentException("event must be SHOT_VERIFIED");
        }
    }
}
