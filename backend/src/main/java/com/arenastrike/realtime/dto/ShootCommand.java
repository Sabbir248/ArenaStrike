package com.arenastrike.realtime.dto;

public record ShootCommand(
        String action,
        long timestamp,
        float angle,
        String weaponId
) {
    public ShootCommand {
        if (!"FIRE".equals(action)) {
            throw new IllegalArgumentException("Only FIRE actions are supported");
        }
        if (timestamp <= 0) {
            throw new IllegalArgumentException("timestamp must be positive");
        }
        if (!Float.isFinite(angle)) {
            throw new IllegalArgumentException("angle must be finite");
        }
        if (weaponId == null || weaponId.isBlank() || weaponId.length() > 32) {
            throw new IllegalArgumentException("weaponId must contain 1-32 characters");
        }
    }
}
