package com.arenastrike.combat;

public enum HitLocation {
    HEAD,
    TORSO,
    LIMB,
    MISS,
    /** @deprecated Use {@link #MISS}. */
    @Deprecated
    NONE
}
