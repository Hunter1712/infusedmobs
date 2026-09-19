package io.github.hunter1712.infusedmobs.ability.trigger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the HURT trigger's attacker-resolution truth table: direct melee and
 * mob-owned projectiles count as Infused Mob attacks; anything else does
 * not. Runs on the pure boolean seam — no entities, no bootstrap.
 */
class MobHurtTriggerTest {

    @Test
    void directMeleeCounts() {
        assertTrue(MobHurtTrigger.isMobAttack(true, false, false));
    }

    @Test
    void mobOwnedProjectileCounts() {
        assertTrue(MobHurtTrigger.isMobAttack(false, true, true));
    }

    @Test
    void nonMobOwnerDoesNotCount() {
        assertFalse(MobHurtTrigger.isMobAttack(false, true, false));
    }

    @Test
    void nullAttackerDoesNotCount() {
        assertFalse(MobHurtTrigger.isMobAttack(false, false, false));
    }

    @Test
    void nonMobNonProjectileDoesNotCount() {
        assertFalse(MobHurtTrigger.isMobAttack(false, false, true));
    }
}
