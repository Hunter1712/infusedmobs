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

        // Tier fields are positive
        for (var tc : new ModConfig.TierConfig[]{defaults.cinder(), defaults.shade(), defaults.doom()}) {
            assertTrue(tc.spawnChance() > 0, "spawnChance should be > 0");
            assertTrue(tc.abilityCount() > 0, "abilityCount should be > 0");
            assertTrue(tc.healthMultiplier() >= 1.0, "healthMultiplier should be >= 1.0");
            assertTrue(tc.xpMultiplier() >= 1.0, "xpMultiplier should be >= 1.0");
        }

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
    void isValidRejectsNullTierConfigs() {
        var validTier = new ModConfig.TierConfig(0.1, 1, 1.0, 1.0);
        var invalid = new ModConfig.Instance(
                null, validTier, validTier,
                60, 0, 60, 0, 5, 4, 4.0f, true);
        assertFalse(invalid.isValid());
    }

    @Test
    void isValidRejectsZeroAbilityCount() {
        var badTier = new ModConfig.TierConfig(0.1, 0, 1.0, 1.0);
        var validTier = new ModConfig.TierConfig(0.1, 1, 1.0, 1.0);
        var invalid = new ModConfig.Instance(
                badTier, validTier, validTier,
                60, 0, 60, 0, 5, 4, 4.0f, true);
        assertFalse(invalid.isValid());
    }

    @Test
    void isValidRejectsZeroSpawnChance() {
        var badTier = new ModConfig.TierConfig(0.0, 1, 1.0, 1.0);
        var validTier = new ModConfig.TierConfig(0.1, 1, 1.0, 1.0);
        var invalid = new ModConfig.Instance(
                validTier, badTier, validTier,
                60, 0, 60, 0, 5, 4, 4.0f, true);
        assertFalse(invalid.isValid());
    }

    @Test
    void isValidAcceptsValidDefaults() {
        assertTrue(ModConfig.Instance.defaults().isValid());
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

    @Test
    void showNametagsDefaultsToTrue() {
        assertTrue(ModConfig.Instance.defaults().showNametags());
    }

    @Test
    void withShowNametagsCreatesCopy() {
        ModConfig.Instance original = ModConfig.Instance.defaults();
        assertTrue(original.showNametags());

        ModConfig.Instance toggled = original.withShowNametags(false);
        assertFalse(toggled.showNametags());
        assertEquals(original.cinder(), toggled.cinder());
    }
}
