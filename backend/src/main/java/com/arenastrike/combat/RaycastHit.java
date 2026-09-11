package com.arenastrike.combat;

public record RaycastHit(Long targetId, HitLocation hitLocation, double distance) {
}
