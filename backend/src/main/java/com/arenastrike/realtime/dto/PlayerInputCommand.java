package com.arenastrike.realtime.dto;

public record PlayerInputCommand(
        Long playerId,
        double forward,
        double strafe,
        boolean jump,
        Rotation rotation,
        String currentWeapon,
        PlayerStance stance
) {
    public PlayerInputCommand {
        if (playerId == null || playerId <= 0) {
            throw new IllegalArgumentException("playerId must be positive");
        }
        if (!Double.isFinite(forward) || !Double.isFinite(strafe)
                || Math.abs(forward) > 1 || Math.abs(strafe) > 1) {
            throw new IllegalArgumentException("movement input must be between -1 and 1");
        }
        if (rotation == null || currentWeapon == null || currentWeapon.isBlank()
                || currentWeapon.length() > 32 || stance == null) {
            throw new IllegalArgumentException("rotation, weapon, and stance are required");
        }
    }
}
