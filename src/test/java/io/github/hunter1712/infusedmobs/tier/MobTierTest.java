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
    void tierOrderIsCinderShadeDoom() {
        MobTier[] values = MobTier.values();
        assertEquals(3, values.length);
        assertEquals(MobTier.CINDER, values[0]);
        assertEquals(MobTier.SHADE, values[1]);
        assertEquals(MobTier.DOOM, values[2]);
    }

    @Test
    void strongerTiersAreRarer() {
        assertTrue(MobTier.CINDER.spawnChance() >= MobTier.SHADE.spawnChance(),
                "CINDER should be at least as common as SHADE");
        assertTrue(MobTier.SHADE.spawnChance() >= MobTier.DOOM.spawnChance(),
                "SHADE should be at least as common as DOOM");
    }

    @Test
    void strongerTiersHaveHigherHealthMultiplier() {
        assertTrue(MobTier.CINDER.healthMultiplier() <= MobTier.SHADE.healthMultiplier(),
                "CINDER healthMultiplier should not exceed SHADE");
        assertTrue(MobTier.SHADE.healthMultiplier() <= MobTier.DOOM.healthMultiplier(),
                "SHADE healthMultiplier should not exceed DOOM");
    }
}
