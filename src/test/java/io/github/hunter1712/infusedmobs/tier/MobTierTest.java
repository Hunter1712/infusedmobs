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
    void allTiersHavePositiveAbilityCount() {
        for (MobTier tier : MobTier.values()) {
            assertTrue(tier.abilityCount() > 0, () -> tier + " abilityCount should be > 0");
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
        assertTrue(MobTier.CINDER.spawnChance() > MobTier.SHADE.spawnChance(),
                "CINDER should be more common than SHADE");
        assertTrue(MobTier.SHADE.spawnChance() > MobTier.DOOM.spawnChance(),
                "SHADE should be more common than DOOM");
    }

    @Test
    void strongerTiersHaveMoreAbilities() {
        assertTrue(MobTier.CINDER.abilityCount() <= MobTier.SHADE.abilityCount(),
                "CINDER abilityCount should not exceed SHADE");
        assertTrue(MobTier.SHADE.abilityCount() <= MobTier.DOOM.abilityCount(),
                "SHADE abilityCount should not exceed DOOM");
    }

    @Test
    void strongerTiersHaveHigherHealthMultiplier() {
        assertTrue(MobTier.CINDER.healthMultiplier() < MobTier.SHADE.healthMultiplier(),
                "CINDER healthMultiplier should be less than SHADE");
        assertTrue(MobTier.SHADE.healthMultiplier() < MobTier.DOOM.healthMultiplier(),
                "SHADE healthMultiplier should be less than DOOM");
    }
}
