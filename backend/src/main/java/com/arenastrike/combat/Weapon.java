package com.arenastrike.combat;

public interface Weapon {
    String id();
    String name();
    int damageFor(HitLocation location);
    long cooldownMillis();
    boolean isInstantKill(HitLocation location);
    int magazineSize();
    long reloadDurationMs();
    int pelletCount();
    double pelletSpreadRadians();
}
