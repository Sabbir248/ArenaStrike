package com.arenastrike.combat;

import com.arenastrike.realtime.dto.PlayerState;
import com.arenastrike.realtime.dto.MovementState;
import com.arenastrike.realtime.dto.Vector3;

public record PlayerHitbox(
        Long playerId,
        Vector3 position,
        double facingRadians,
        MovementState state,
        double groundY,
        double height
) {
    public static final double BASE_HEIGHT = PlayerState.BASE_HEIGHT;
    public static final double HALF_WIDTH = 0.35;
    public static final double HALF_DEPTH = 0.25;

    public PlayerHitbox {
        if (playerId == null || position == null || state == null
                || !Double.isFinite(facingRadians) || !Double.isFinite(groundY)
                || !Double.isFinite(height) || height <= 0) {
            throw new IllegalArgumentException("Invalid player hitbox");
        }
    }

    public static PlayerHitbox from(PlayerState player) {
        double groundY = player.position().y();
        double height = effectiveHeight(player.state(), BASE_HEIGHT);
        return new PlayerHitbox(player.playerId(), player.position(), player.rotation().y(),
                player.state(), groundY, height);
    }

    public static double effectiveHeight(MovementState state, double baseHeight) {
        return baseHeight * state.heightScale();
    }

    public double top() {
        return groundY + height;
    }

    public double headBottom() {
        return top() - height * 0.2;
    }

    public double torsoBottom() {
        return top() - height * 0.6;
    }

    public HitLocation hitZone(double elevation, double localZ) {
        if (elevation < 0 || elevation > height) {
            return HitLocation.MISS;
        }
        if (state == MovementState.PRONE && localZ <= 0) {
            return HitLocation.HEAD;
        }
        if (state == MovementState.PRONE) {
            return elevation >= 0.30 ? HitLocation.TORSO : HitLocation.LIMB;
        }
        return elevation >= height * 0.8 ? HitLocation.HEAD
                : elevation >= height * 0.4 ? HitLocation.TORSO : HitLocation.LIMB;
    }
}
