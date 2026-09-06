package com.arenastrike.combat;

public enum WeaponType {
    PISTOL(35, 20, 15, 500),
    ASSAULT_RIFLE(45, 25, 18, 100),
    SMG(30, 18, 12, 80),
    SHOTGUN(90, 60, 40, 900),
    BOLT_ACTION_RIFLE(110, 70, 50, 1200);

    private final int headDamage;
    private final int torsoDamage;
    private final int limbDamage;
    private final long cooldownMillis;

    WeaponType(int headDamage, int torsoDamage, int limbDamage, long cooldownMillis) {
        this.headDamage = headDamage;
        this.torsoDamage = torsoDamage;
        this.limbDamage = limbDamage;
        this.cooldownMillis = cooldownMillis;
    }

    public int damageFor(HitLocation location) {
        return switch (location) {
            case HEAD -> headDamage;
            case TORSO -> torsoDamage;
            case LIMB -> limbDamage;
        };
    }

    public long cooldownMillis() {
        return cooldownMillis;
    }
}
