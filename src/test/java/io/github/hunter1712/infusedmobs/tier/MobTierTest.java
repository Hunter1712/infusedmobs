package io.github.hunter1712.infusedmobs.tier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic tests for {@link MobTier} enum values.
 */
class MobTierTest {

    @Test
    void allTiersHavePositiveSpawnChance() {
        for (MobTier tier : MobTier.values()) {
            assertTrue(tier.spawnChance() > 0,
                    () -> tier + " spawnChance should be positive");
        }
    }

    @Test
    void allTiersHavePositiveHealthMultiplier() {
        for (MobTier tier : MobTier.values()) {
            assertTrue(tier.healthMultiplier() >= 1.0,
                    () -> tier + " healthMultiplier should be >= 1.0");
        }
    }

    @Test
    void allTiersHavePositiveXpMultiplier() {
        for (MobTier tier : MobTier.values()) {
            assertTrue(tier.xpMultiplier() >= 1.0,
                    () -> tier + " xpMultiplier should be >= 1.0");
        }
    }

    @Test
    void allTiersHaveNonNegativeAbilityCounts() {
        for (MobTier tier : MobTier.values()) {
            assertTrue(tier.hurtAbilities() >= 0, () -> tier + " hurtAbilities should be >= 0");
            assertTrue(tier.tickAbilities() >= 0, () -> tier + " tickAbilities should be >= 0");
            assertTrue(tier.deathAbilities() >= 0, () -> tier + " deathAbilities should be >= 0");
        }
    }

    @Test
    void tierOrderIsEmberSurgeTempest() {
        MobTier[] values = MobTier.values();
        assertEquals(3, values.length);
        assertEquals(MobTier.EMBER, values[0]);
        assertEquals(MobTier.SURGE, values[1]);
        assertEquals(MobTier.TEMPEST, values[2]);
    }

    @Test
    void strongerTiersAreRarer() {
        assertTrue(MobTier.EMBER.spawnChance() >= MobTier.SURGE.spawnChance(),
                "EMBER should be at least as common as SURGE");
        assertTrue(MobTier.SURGE.spawnChance() >= MobTier.TEMPEST.spawnChance(),
                "SURGE should be at least as common as TEMPEST");
    }

    @Test
    void strongerTiersHaveHigherHealthMultiplier() {
        assertTrue(MobTier.EMBER.healthMultiplier() <= MobTier.SURGE.healthMultiplier(),
                "EMBER healthMultiplier should not exceed SURGE");
        assertTrue(MobTier.SURGE.healthMultiplier() <= MobTier.TEMPEST.healthMultiplier(),
                "SURGE healthMultiplier should not exceed TEMPEST");
    }
}
