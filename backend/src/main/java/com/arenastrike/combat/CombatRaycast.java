package com.arenastrike.combat;

import com.arenastrike.realtime.dto.Vector3;

import java.util.Collection;
import java.util.Comparator;

public final class CombatRaycast {
    private static final double EPSILON = 1e-9;

    private CombatRaycast() {
    }

    public static RaycastHit firstHit(Vector3 origin, Vector3 direction,
                                      Long shooterId, Collection<PlayerHitbox> hitboxes,
                                      double maxDistance) {
        double length = Math.sqrt(direction.x() * direction.x()
                + direction.y() * direction.y() + direction.z() * direction.z());
        if (!Double.isFinite(length) || length < EPSILON || maxDistance < 0) {
            return none(maxDistance);
        }
        Vector3 unit = new Vector3(direction.x() / length, direction.y() / length,
                direction.z() / length);
        return hitboxes.stream()
                .filter(hitbox -> !hitbox.playerId().equals(shooterId))
                .map(hitbox -> intersect(origin, unit, hitbox, maxDistance))
                .filter(hit -> hit != null && hit.hitLocation() != HitLocation.MISS
                        && hit.hitLocation() != HitLocation.NONE)
                .min(Comparator.comparingDouble(RaycastHit::distance))
                .orElseGet(() -> none(maxDistance));
    }

    private static RaycastHit intersect(Vector3 origin, Vector3 direction,
                                        PlayerHitbox hitbox, double maxDistance) {
        double cos = Math.cos(hitbox.facingRadians());
        double sin = Math.sin(hitbox.facingRadians());

        // Rotate the ray into the target's local X/Z space by the inverse yaw.
        double relativeX = origin.x() - hitbox.position().x();
        double relativeZ = origin.z() - hitbox.position().z();
        double localOriginX = relativeX * cos - relativeZ * sin;
        double localOriginZ = relativeX * sin + relativeZ * cos;
        double localDirectionX = direction.x() * cos - direction.z() * sin;
        double localDirectionZ = direction.x() * sin + direction.z() * cos;

        double entry = 0;
        double exit = maxDistance;
        double[] slabX = intersectSlab(localOriginX, localDirectionX,
                -PlayerHitbox.HALF_WIDTH, PlayerHitbox.HALF_WIDTH);
        double[] slabZ = intersectSlab(localOriginZ, localDirectionZ,
                -PlayerHitbox.HALF_DEPTH, PlayerHitbox.HALF_DEPTH);
        // Evaluate elevation relative to the target's dynamic local ground plane.
        double[] slabY = intersectSlab(origin.y(), direction.y(), hitbox.groundY(),
                hitbox.top());
        if (slabX == null || slabZ == null || slabY == null) {
            return null;
        }
        entry = Math.max(entry, Math.max(slabX[0], Math.max(slabZ[0], slabY[0])));
        exit = Math.min(exit, Math.min(slabX[1], Math.min(slabZ[1], slabY[1])));
        if (entry > exit || exit < 0 || entry > maxDistance) {
            return null;
        }

        double distance = Math.max(0, entry);
        double hitY = origin.y() + direction.y() * distance;
        double elevation = hitY - hitbox.groundY();
        double localHitZ = localOriginZ + localDirectionZ * distance;
        HitLocation zone = hitbox.hitZone(elevation, localHitZ);
        if (zone == HitLocation.MISS) {
            return null;
        }
        return new RaycastHit(hitbox.playerId(), zone, distance);
    }

    private static double[] intersectSlab(double origin, double direction, double min, double max) {
        if (Math.abs(direction) < EPSILON) {
            return origin >= min && origin <= max ? new double[]{0, Double.POSITIVE_INFINITY} : null;
        }
        double first = (min - origin) / direction;
        double second = (max - origin) / direction;
        return first <= second ? new double[]{first, second} : new double[]{second, first};
    }

    private static RaycastHit none(double distance) {
        return new RaycastHit(null, HitLocation.MISS, Math.max(0, distance));
    }
}
