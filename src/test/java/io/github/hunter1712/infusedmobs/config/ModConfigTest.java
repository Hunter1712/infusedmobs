package io.github.hunter1712.infusedmobs.config;

import io.github.hunter1712.infusedmobs.tier.MobTier;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic tests for {@link ModConfig} records and defaults.
 * <p>
 * These tests do not load from disk — they verify the default values
 * and record behaviour only.
 */
class ModConfigTest {

    /** A valid tier config used as filler for tiers not under test. */
    private static final ModConfig.TierConfig VALID_TIER =
            new ModConfig.TierConfig(0.1, 1, 1.0, 1.0);

    // ========================================
    // Existing tests (updated for new Instance fields)
    // ========================================

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
        var invalid = new ModConfig.Instance(
                null, VALID_TIER, VALID_TIER,
                60, 0, 60, 0, 5, 4, 4.0f, true, List.of(), 2);
        assertFalse(invalid.isValid());
    }

    @Test
    void isValidRejectsZeroAbilityCount() {
        var badTier = new ModConfig.TierConfig(0.1, 0, 1.0, 1.0);
        var invalid = new ModConfig.Instance(
                badTier, VALID_TIER, VALID_TIER,
                60, 0, 60, 0, 5, 4, 4.0f, true, List.of(), 2);
        assertFalse(invalid.isValid());
    }

    @Test
    void isValidRejectsZeroSpawnChance() {
        var badTier = new ModConfig.TierConfig(0.0, 1, 1.0, 1.0);
        var invalid = new ModConfig.Instance(
                VALID_TIER, badTier, VALID_TIER,
                60, 0, 60, 0, 5, 4, 4.0f, true, List.of(), 2);
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

    // ========================================
    // worldBlacklist
    // ========================================

    @Test
    void worldBlacklistDefaultsToEmpty() {
        ModConfig.Instance defaults = ModConfig.Instance.defaults();
        assertNotNull(defaults.worldBlacklist());
        assertTrue(defaults.worldBlacklist().isEmpty());
    }

    @Test
    void isValidAcceptsNullWorldBlacklist() {
        // A 2.6.0 config file has no worldBlacklist field → Gson deserialises it as null.
        // isValid() must accept this so the config can be backfilled rather than discarded.
        var valid = new ModConfig.Instance(
                VALID_TIER, VALID_TIER, VALID_TIER,
                60, 0, 60, 0, 5, 4, 4.0f, true, null, 1);
        assertTrue(valid.isValid());
    }

    @Test
    void isValidRejectsMalformedWorldId() {
        // Missing colon → not a valid resource id
        var noColon = new ModConfig.Instance(
                VALID_TIER, VALID_TIER, VALID_TIER,
                60, 0, 60, 0, 5, 4, 4.0f, true, List.of("overworld"), 2);
        assertFalse(noColon.isValid());

        // Colon at start → empty namespace
        var emptyNs = new ModConfig.Instance(
                VALID_TIER, VALID_TIER, VALID_TIER,
                60, 0, 60, 0, 5, 4, 4.0f, true, List.of(":overworld"), 2);
        assertFalse(emptyNs.isValid());

        // Colon at end → empty path
        var emptyPath = new ModConfig.Instance(
                VALID_TIER, VALID_TIER, VALID_TIER,
                60, 0, 60, 0, 5, 4, 4.0f, true, List.of("minecraft:"), 2);
        assertFalse(emptyPath.isValid());

        // Null entry in the list — Arrays.asList allows nulls (List.of throws).
        var nullEntry = new ModConfig.Instance(
                VALID_TIER, VALID_TIER, VALID_TIER,
                60, 0, 60, 0, 5, 4, 4.0f, true, Arrays.asList((String) null), 2);
        assertFalse(nullEntry.isValid());
    }

    @Test
    void isValidAcceptsValidWorldBlacklist() {
        var valid = new ModConfig.Instance(
                VALID_TIER, VALID_TIER, VALID_TIER,
                60, 0, 60, 0, 5, 4, 4.0f, true,
                List.of("minecraft:overworld", "minecraft:the_nether"), 2);
        assertTrue(valid.isValid());
    }

    @Test
    void isWorldBlacklistedMatchesExactEntry() {
        ModConfig.Instance cfg = ModConfig.Instance.defaults()
                .withWorldBlacklist(List.of("minecraft:overworld"));
        assertTrue(cfg.isWorldBlacklisted("minecraft:overworld"));
        assertFalse(cfg.isWorldBlacklisted("minecraft:the_nether"));
    }

    @Test
    void isWorldBlacklistedTrimsInput() {
        ModConfig.Instance cfg = ModConfig.Instance.defaults()
                .withWorldBlacklist(List.of("minecraft:overworld"));
        assertTrue(cfg.isWorldBlacklisted("  minecraft:overworld  "));
    }

    @Test
    void isWorldBlacklistedHandlesNullAndEmpty() {
        ModConfig.Instance cfg = ModConfig.Instance.defaults();
        assertFalse(cfg.isWorldBlacklisted(null));
        assertFalse(cfg.isWorldBlacklisted("minecraft:overworld"));
    }

    @Test
    void withWorldBlacklistNormalisesEntries() {
        // Arrays.asList allows nulls (List.of throws on null elements).
        ModConfig.Instance cfg = ModConfig.Instance.defaults()
                .withWorldBlacklist(Arrays.asList(
                        "  minecraft:overworld  ",  // trimmed
                        "minecraft:the_nether",
                        "minecraft:overworld",      // duplicate → dropped
                        "",                          // blank → dropped
                        null                         // null → dropped
                ));
        List<String> blacklist = cfg.worldBlacklist();
        assertEquals(2, blacklist.size());
        assertEquals("minecraft:overworld", blacklist.get(0));
        assertEquals("minecraft:the_nether", blacklist.get(1));
    }

    @Test
    void withWorldBlacklistNullReturnsEmptyList() {
        ModConfig.Instance cfg = ModConfig.Instance.defaults()
                .withWorldBlacklist(null);
        assertNotNull(cfg.worldBlacklist());
        assertTrue(cfg.worldBlacklist().isEmpty());
    }

    @Test
    void withWorldBlacklistPreservesOtherFields() {
        ModConfig.Instance original = ModConfig.Instance.defaults();
        ModConfig.Instance updated = original.withWorldBlacklist(List.of("minecraft:overworld"));
        assertEquals(original.cinder(), updated.cinder());
        assertEquals(original.showNametags(), updated.showNametags());
    }

    @Test
    void withWorldBlacklistReturnsImmutableList() {
        ModConfig.Instance cfg = ModConfig.Instance.defaults()
                .withWorldBlacklist(List.of("minecraft:overworld"));
        assertThrows(UnsupportedOperationException.class,
                () -> cfg.worldBlacklist().add("minecraft:the_end"));
    }

    // ========================================
    // backfillFromDefaults (upgrade path)
    // ========================================

    @Test
    void backfillFromDefaultsUpgradesOldConfig() {
        // Simulate a 2.6.0 config: configVersion=1, no worldBlacklist (null).
        var oldConfig = new ModConfig.Instance(
                VALID_TIER, VALID_TIER, VALID_TIER,
                60, 0, 60, 0, 5, 4, 4.0f, true, null, 1);

        assertTrue(oldConfig.isValid(), "old config should still be valid");

        ModConfig.Instance upgraded = oldConfig.backfillFromDefaults();
        assertNotNull(upgraded.worldBlacklist());
        assertTrue(upgraded.worldBlacklist().isEmpty());
        assertEquals(3, upgraded.configVersion());
        // Tier settings preserved
        assertEquals(VALID_TIER, upgraded.cinder());
    }

    @Test
    void backfillFromDefaultsPreservesCurrentConfig() {
        // A current-version config should pass through unchanged (except null blacklist → empty).
        ModConfig.Instance current = ModConfig.Instance.defaults()
                .withWorldBlacklist(List.of("minecraft:overworld"));

        ModConfig.Instance backfilled = current.backfillFromDefaults();
        assertEquals(List.of("minecraft:overworld"), backfilled.worldBlacklist());
        assertEquals(3, backfilled.configVersion());
    }

    @Test
    void backfillFromDefaultsFillsNullBlacklistOnCurrentVersion() {
        // Edge case: configVersion is current but worldBlacklist is null
        // (e.g. hand-edited JSON missing the field). Should be filled to empty.
        var cfg = new ModConfig.Instance(
                VALID_TIER, VALID_TIER, VALID_TIER,
                60, 0, 60, 0, 5, 4, 4.0f, true, null, 3);

        ModConfig.Instance backfilled = cfg.backfillFromDefaults();
        assertNotNull(backfilled.worldBlacklist());
        assertTrue(backfilled.worldBlacklist().isEmpty());
    }
}
