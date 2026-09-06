package com.arenastrike.realtime.dto;

public record PlayerState(
        Long playerId,
        String displayName,
        Vector3 position,
        Rotation rotation,
        int health,
        String currentWeapon,
        PlayerStance stance
) {
    public PlayerState {
        if (playerId == null || playerId <= 0) {
            throw new IllegalArgumentException("playerId must be positive");
        }
        if (displayName == null || displayName.isBlank() || displayName.length() > 32) {
            throw new IllegalArgumentException("displayName must contain 1-32 characters");
        }
        if (position == null || rotation == null) {
            throw new IllegalArgumentException("position and rotation are required");
        }
        if (health < 0 || health > 100) {
            throw new IllegalArgumentException("health must be between 0 and 100");
        }
        if (currentWeapon == null || currentWeapon.isBlank() || currentWeapon.length() > 32) {
            throw new IllegalArgumentException("currentWeapon must contain 1-32 characters");
        }
        if (stance == null) {
            throw new IllegalArgumentException("stance is required");
        }
    }
}
