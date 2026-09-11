package com.arenastrike.realtime.dto;

public record PlayerMovementPacket(
        Long playerId,
        double x,
        double y,
        double z,
        double velocityX,
        double velocityY,
        MovementState state,
        boolean isJumping,
        Rotation rotation,
        String currentWeapon
) {
    public PlayerMovementPacket {
        if (playerId == null || playerId <= 0) {
            throw new IllegalArgumentException("playerId must be positive");
        }
        if (!Double.isFinite(x) || !Double.isFinite(y) || !Double.isFinite(z)) {
            throw new IllegalArgumentException("position must be finite");
        }
        if (rotation == null || currentWeapon == null || currentWeapon.isBlank()
                || currentWeapon.length() > 32 || state == null) {
            throw new IllegalArgumentException("rotation, weapon, and state are required");
        }
    }
}
