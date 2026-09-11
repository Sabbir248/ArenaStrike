package com.arenastrike.combat;

public final class PlayerWeaponState {
    private final Weapon weapon;
    private final long reloadDurationMs;
    private long lastShotTimestamp;
    private long reloadStartTime;
    private int currentAmmo;
    private boolean reloading;

    public PlayerWeaponState(Weapon weapon) {
        this.weapon = weapon;
        this.reloadDurationMs = weapon.reloadDurationMs();
        this.currentAmmo = weapon.magazineSize();
    }

    public Weapon weapon() {
        return weapon;
    }

    public long lastShotTimestamp() {
        return lastShotTimestamp;
    }

    public int currentAmmo() {
        return currentAmmo;
    }

    public boolean isReloading() {
        return reloading;
    }

    public void markShot(long timestamp) {
        lastShotTimestamp = timestamp;
        currentAmmo--;
    }

    public void beginReload() {
        beginReload(System.currentTimeMillis());
    }

    public void beginReload(long serverTimestamp) {
        if (currentAmmo < weapon.magazineSize() && !isReloading()) {
            reloading = true;
            reloadStartTime = serverTimestamp;
        }
    }

    public long reloadStartTime() {
        return reloadStartTime;
    }

    public long reloadStartTimestamp() {
        return reloadStartTime;
    }

    public long reloadDurationMs() {
        return reloadDurationMs;
    }

    public boolean reloadInProgress(long now) {
        return reloading && now - reloadStartTime < reloadDurationMs;
    }

    public boolean completeReloadIfReady(long now) {
        if (!reloading || now - reloadStartTime < reloadDurationMs) {
            return false;
        }
        currentAmmo = weapon.magazineSize();
        reloading = false;
        return true;
    }
}
