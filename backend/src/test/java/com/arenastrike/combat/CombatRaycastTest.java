package com.arenastrike.combat;

import com.arenastrike.realtime.dto.MovementState;
import com.arenastrike.realtime.dto.Vector3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class CombatRaycastTest {

    private PlayerHitbox createTargetHitbox(Long id, Vector3 position) {
        return new PlayerHitbox(
                id,
                position,
                0.0, // facing forward
                MovementState.STANDING,
                position.y(),
                1.8 // base height
        );
    }

    @Test
    void testDirectTorsoHit() {
        Vector3 origin = new Vector3(0, 1.0, 0); // Shooter looking at Y=1.0
        Vector3 direction = new Vector3(0, 0, 1); // Shooting directly along Z
        
        // Target is standing at (0, 0, 10)
        PlayerHitbox target = createTargetHitbox(2L, new Vector3(0, 0, 10));
        
        RaycastHit hit = CombatRaycast.firstHit(origin, direction, 1L, List.of(target), 200.0);
        
        assertNotNull(hit);
        assertEquals(2L, hit.targetId());
        assertEquals(HitLocation.TORSO, hit.hitLocation());
        assertTrue(hit.distance() > 9.0 && hit.distance() < 11.0);
    }

    @Test
    void testMissWhenShootingOverHead() {
        Vector3 origin = new Vector3(0, 2.5, 0); // Shooter is very high
        Vector3 direction = new Vector3(0, 0, 1); // Shooting straight
        
        PlayerHitbox target = createTargetHitbox(2L, new Vector3(0, 0, 10));
        
        RaycastHit hit = CombatRaycast.firstHit(origin, direction, 1L, List.of(target), 200.0);
        
        assertNotNull(hit);
        assertEquals(HitLocation.MISS, hit.hitLocation());
    }

    @Test
    void testObstacleOcclusion() {
        Vector3 origin = new Vector3(0, 1.0, 0);
        Vector3 direction = new Vector3(0, 0, 1);
        
        PlayerHitbox target = createTargetHitbox(2L, new Vector3(0, 0, 10));
        
        // Simulating an obstacle at Z=5 by setting maxDistance to 5.0
        RaycastHit hit = CombatRaycast.firstHit(origin, direction, 1L, List.of(target), 5.0);
        
        assertNotNull(hit);
        assertEquals(HitLocation.MISS, hit.hitLocation());
    }
    
    @Test
    void testHeadshotHit() {
        Vector3 origin = new Vector3(0, 1.6, 0); // Shooter aiming at head height
        Vector3 direction = new Vector3(0, 0, 1);
        
        PlayerHitbox target = createTargetHitbox(2L, new Vector3(0, 0, 10));
        
        RaycastHit hit = CombatRaycast.firstHit(origin, direction, 1L, List.of(target), 200.0);
        
        assertNotNull(hit);
        assertEquals(2L, hit.targetId());
        assertEquals(HitLocation.HEAD, hit.hitLocation());
    }
}
