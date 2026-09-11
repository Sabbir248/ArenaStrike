package com.arenastrike.combat;

public abstract class BaseWeapon implements Weapon {
    private final String id;
    private final int headDamage;
    private final int torsoDamage;
    private final int limbDamage;
    private final long cooldownMillis;
    private final boolean instantHeadKill;
    private final int magazineSize;
    private final long reloadDurationMs;
    private final int pelletCount;
    private final double pelletSpreadRadians;

    protected BaseWeapon(String id, int headDamage, int torsoDamage, int limbDamage, long cooldownMillis,
                         int magazineSize, long reloadDurationMs, int pelletCount,
                         double pelletSpreadRadians, boolean instantHeadKill) {
        this.id = id;
        this.headDamage = headDamage;
        this.torsoDamage = torsoDamage;
        this.limbDamage = limbDamage;
        this.cooldownMillis = cooldownMillis;
        this.magazineSize = magazineSize;
        this.reloadDurationMs = reloadDurationMs;
        this.pelletCount = pelletCount;
        this.pelletSpreadRadians = pelletSpreadRadians;
        this.instantHeadKill = instantHeadKill;
    }

    @Override
    public String id() {
        return id;
    }

    @Override
    public String name() {
        return id;
    }

    @Override
    public int damageFor(HitLocation location) {
        return switch (location) {
            case HEAD -> headDamage;
            case TORSO -> torsoDamage;
            case LIMB -> limbDamage;
            case MISS, NONE -> 0;
        };
    }

    @Override
    public long cooldownMillis() {
        return cooldownMillis;
    }

    @Override
    public boolean isInstantKill(HitLocation location) {
        return instantHeadKill && location == HitLocation.HEAD;
    }

    @Override
    public int magazineSize() {
        return magazineSize;
    }

    @Override
    public long reloadDurationMs() {
        return reloadDurationMs;
    }

    @Override
    public int pelletCount() {
        return pelletCount;
    }

    @Override
    public double pelletSpreadRadians() {
        return pelletSpreadRadians;
    }
}
