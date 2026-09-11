package com.arenastrike.combat;

public enum WeaponType {
    PISTOL(35, 20, 15, 500, 12, 1200, 1, 0),
    ASSAULT_RIFLE(45, 25, 18, 100, 30, 1800, 1, 0),
    SMG(30, 18, 12, 80, 25, 1600, 1, 0),
    SHOTGUN(90, 60, 40, 900, 2, 2200, 8, Math.toRadians(15)),
    BOLT_ACTION_RIFLE(100, 70, 50, 1200, 5, 2400, 1, 0, true);

    private final int headDamage;
    private final int torsoDamage;
    private final int limbDamage;
    private final long cooldownMillis;
    private final boolean instantHeadKill;
    private final int magazineSize;
    private final long reloadDurationMs;
    private final int pelletCount;
    private final double pelletSpreadRadians;

    WeaponType(int headDamage, int torsoDamage, int limbDamage, long cooldownMillis) {
        this(headDamage, torsoDamage, limbDamage, cooldownMillis, 30, 1800, 1, 0, false);
    }

    WeaponType(int headDamage, int torsoDamage, int limbDamage, long cooldownMillis, int magazineSize) {
        this(headDamage, torsoDamage, limbDamage, cooldownMillis, magazineSize, 1800, 1, 0, false);
    }

    WeaponType(int headDamage, int torsoDamage, int limbDamage, long cooldownMillis,
               int magazineSize, long reloadDurationMs, int pelletCount, double pelletSpreadRadians) {
        this(headDamage, torsoDamage, limbDamage, cooldownMillis, magazineSize,
                reloadDurationMs, pelletCount, pelletSpreadRadians, false);
    }

    WeaponType(int headDamage, int torsoDamage, int limbDamage, long cooldownMillis,
               int magazineSize, long reloadDurationMs, int pelletCount,
               double pelletSpreadRadians, boolean instantHeadKill) {
        this.headDamage = headDamage;
        this.torsoDamage = torsoDamage;
        this.limbDamage = limbDamage;
        this.cooldownMillis = cooldownMillis;
        this.instantHeadKill = instantHeadKill;
        this.magazineSize = magazineSize;
        this.reloadDurationMs = reloadDurationMs;
        this.pelletCount = pelletCount;
        this.pelletSpreadRadians = pelletSpreadRadians;
    }

    public int damageFor(HitLocation location) {
        return switch (location) {
            case HEAD -> headDamage;
            case TORSO -> torsoDamage;
            case LIMB -> limbDamage;
            case MISS, NONE -> 0;
        };
    }

    public long cooldownMillis() {
        return cooldownMillis;
    }

    public boolean isInstantKill(HitLocation location) {
        return instantHeadKill && location == HitLocation.HEAD;
    }

    public int magazineSize() {
        return magazineSize;
    }

    public long reloadDurationMs() {
        return reloadDurationMs;
    }

    public int pelletCount() {
        return pelletCount;
    }

    public double pelletSpreadRadians() {
        return pelletSpreadRadians;
    }
}
