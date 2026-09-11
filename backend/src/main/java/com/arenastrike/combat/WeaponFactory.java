package com.arenastrike.combat;

import java.util.Map;

public class WeaponFactory {
    private static final Map<String, Weapon> WEAPONS = Map.of(
        "PISTOL", new Pistol(),
        "ASSAULT_RIFLE", new AssaultRifle(),
        "SMG", new Smg(),
        "SHOTGUN", new Shotgun(),
        "BOLT_ACTION_RIFLE", new BoltActionRifle()
    );

    public static Weapon get(String id) {
        Weapon weapon = WEAPONS.get(id);
        if (weapon == null) {
            throw new IllegalArgumentException("Unknown weapon: " + id);
        }
        return weapon;
    }
}
