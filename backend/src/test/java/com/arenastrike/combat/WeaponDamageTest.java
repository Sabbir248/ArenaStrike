package com.arenastrike.combat;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WeaponDamageTest {

    @Test
    void testAssaultRifleDamage() {
        Weapon weapon = WeaponFactory.get("ASSAULT_RIFLE");
        assertEquals(25, weapon.damageFor(HitLocation.TORSO));
        assertEquals(45, weapon.damageFor(HitLocation.HEAD));
        assertEquals(18, weapon.damageFor(HitLocation.LIMB));
        assertFalse(weapon.isInstantKill(HitLocation.HEAD));
    }

    @Test
    void testBoltActionRifleInstantKill() {
        Weapon weapon = WeaponFactory.get("BOLT_ACTION_RIFLE");
        assertTrue(weapon.isInstantKill(HitLocation.HEAD));
        assertEquals(70, weapon.damageFor(HitLocation.TORSO));
        assertEquals(0, weapon.damageFor(HitLocation.MISS));
    }

    @Test
    void testSmgDamage() {
        Weapon weapon = WeaponFactory.get("SMG");
        assertEquals(18, weapon.damageFor(HitLocation.TORSO));
        assertEquals(30, weapon.damageFor(HitLocation.HEAD));
        assertFalse(weapon.isInstantKill(HitLocation.HEAD));
    }

    @Test
    void testPistolDamage() {
        Weapon weapon = WeaponFactory.get("PISTOL");
        assertEquals(20, weapon.damageFor(HitLocation.TORSO));
        assertEquals(35, weapon.damageFor(HitLocation.HEAD));
    }

    @Test
    void testShotgunDamage() {
        Weapon weapon = WeaponFactory.get("SHOTGUN");
        // Shotgun logic usually calculates pellet damage, but let's test base damageFor
        assertEquals(60, weapon.damageFor(HitLocation.TORSO));
        assertEquals(90, weapon.damageFor(HitLocation.HEAD));
    }
}
