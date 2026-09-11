package com.arenastrike.realtime.dto;

public enum PlayerStance {
    STANDING(1.0, -1.0),
    CROUCHING(1.1 / 1.8, -0.6),
    PRONE(0.45 / 1.8, -0.3),
    JUMPING(1.0, -1.0);

    private final double heightScale;
    private final double hitboxOffset;

    PlayerStance(double hitboxHeight, double hitboxOffset) {
        this.heightScale = hitboxHeight;
        this.hitboxOffset = hitboxOffset;
    }

    public double hitboxHeight() {
        return heightScale;
    }

    public double heightScale() {
        return heightScale;
    }

    public double hitboxOffset() {
        return hitboxOffset;
    }
}
