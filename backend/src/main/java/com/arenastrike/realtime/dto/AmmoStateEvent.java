package com.arenastrike.realtime.dto;

public record AmmoStateEvent(
        String event,
        String playerId,
        String weaponId,
        int currentAmmo,
        boolean isReloading
) {
    public AmmoStateEvent {
        if (!"AMMO_STATE".equals(event)) {
            throw new IllegalArgumentException("event must be AMMO_STATE");
        }
    }
}
