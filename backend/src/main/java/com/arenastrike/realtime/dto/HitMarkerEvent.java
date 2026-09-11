package com.arenastrike.realtime.dto;

import com.arenastrike.combat.HitLocation;

public record HitMarkerEvent(
        Long shooterId,
        Long targetId,
        HitLocation hitLocation,
        int damage,
        boolean killed,
        boolean instantKill
) {
}
