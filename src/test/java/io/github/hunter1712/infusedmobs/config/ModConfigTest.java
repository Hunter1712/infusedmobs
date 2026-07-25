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
        assertNotNull(defaults.ember());
        assertNotNull(defaults.surge());
        assertNotNull(defaults.tempest());
    }

    @Test
    void forTierReturnsCorrectConfig() {
        ModConfig.Instance defaults = ModConfig.Instance.defaults();
        assertEquals(defaults.ember(), defaults.forTier(MobTier.EMBER));
        assertEquals(defaults.surge(), defaults.forTier(MobTier.SURGE));
        assertEquals(defaults.tempest(), defaults.forTier(MobTier.TEMPEST));
    }

    @Test
    void defaultValueRanges() {
        ModConfig.Instance defaults = ModConfig.Instance.defaults();

        // Tier spawn chances
        assertTrue(defaults.ember().spawnChance() > 0);
        assertTrue(defaults.surge().spawnChance() > 0);
        assertTrue(defaults.tempest().spawnChance() > 0);

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
