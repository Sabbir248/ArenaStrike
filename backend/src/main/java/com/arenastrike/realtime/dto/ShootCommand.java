package com.arenastrike.realtime.dto;

import com.arenastrike.combat.*;

public record ShootCommand(
        Long shooterId,
        Long targetId,
        HitLocation hitLocation,
        WeaponType weapon
) {
    public ShootCommand {
        if (shooterId == null || targetId == null || shooterId.equals(targetId)) {
            throw new IllegalArgumentException("Shooter and target must be different players");
        }
        if (hitLocation == null || weapon == null) {
            throw new IllegalArgumentException("Weapon and hit location are required");
        }
    }
}
