package io.github.hunter1712.infusedmobs.config;

import io.github.hunter1712.infusedmobs.tier.MobTier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic tests for {@link ModConfig} records and defaults.
 * <p>
 * These tests do not load from disk — they verify the default values
 * and record behaviour only.
 */
class ModConfigTest {

    @Test
    void defaultsHaveAllTiersConfigured() {
        ModConfig.Instance defaults = ModConfig.Instance.defaults();
        assertNotNull(defaults.cinder());
        assertNotNull(defaults.shade());
        assertNotNull(defaults.doom());
    }

    @Test
    void forTierReturnsCorrectConfig() {
        ModConfig.Instance defaults = ModConfig.Instance.defaults();
        assertEquals(defaults.cinder(), defaults.forTier(MobTier.CINDER));
        assertEquals(defaults.shade(), defaults.forTier(MobTier.SHADE));
        assertEquals(defaults.doom(), defaults.forTier(MobTier.DOOM));
    }

    @Test
    void defaultValueRanges() {
        ModConfig.Instance defaults = ModConfig.Instance.defaults();

        // Tier spawn chances
        assertTrue(defaults.cinder().spawnChance() > 0);
        assertTrue(defaults.shade().spawnChance() > 0);
        assertTrue(defaults.doom().spawnChance() > 0);

        // Effect durations are positive
        assertTrue(defaults.hurtEffectDuration() > 0);
        assertTrue(defaults.tickEffectDuration() > 0);
        assertTrue(defaults.infernoFireSeconds() > 0);

        // Armor damage is positive
        assertTrue(defaults.acidArmorDamage() > 0);

        // Explosion power is positive
        assertTrue(defaults.combustExplosionPower() > 0);
    }

    @Test
    void configConstantsAreSane() {
        ModConfig.Instance defaults = ModConfig.Instance.defaults();

        // HURT effects should last longer than TICK effects (different balance)
        // Not a strict rule, but a sanity check
        assertTrue(defaults.hurtEffectDuration() >= defaults.tickEffectDuration(),
                "HURT duration should be >= TICK duration");

        // Inferno should be a reasonable number of seconds
        assertTrue(defaults.infernoFireSeconds() >= 1 && defaults.infernoFireSeconds() <= 30);
    }
}
