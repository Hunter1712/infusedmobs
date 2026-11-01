package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.config.ModConfig;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coordinator-adjacent tests for {@link MobTierManager}.
 * <p>
 * The full assign/restore paths need Minecraft bootstrap, so this pins the
 * pure Mob Blacklist decision behind the natural-roll gate — no {@code Mob}
 * or level required.
 */
class MobTierManagerTest {

    @Test
    void listedEntityKeyIsBlacklisted() {
        ModConfig.Instance config = ModConfig.Instance.defaults()
                .withMobBlacklist(List.of("minecraft:spider"));

        assertTrue(MobTierManager.isMobBlacklisted("minecraft:spider", config));
    }

    @Test
    void unlistedEntityKeyIsNotBlacklisted() {
        ModConfig.Instance config = ModConfig.Instance.defaults()
                .withMobBlacklist(List.of("minecraft:spider"));

        assertFalse(MobTierManager.isMobBlacklisted("minecraft:zombie", config));
    }

    @Test
    void nullEntityKeyNeverMatches() {
        ModConfig.Instance config = ModConfig.Instance.defaults()
                .withMobBlacklist(List.of("minecraft:spider"));

        assertFalse(MobTierManager.isMobBlacklisted(null, config));
    }

    @Test
    void emptyBlacklistMatchesNothing() {
        ModConfig.Instance config = ModConfig.Instance.defaults();

        assertFalse(MobTierManager.isMobBlacklisted("minecraft:spider", config));
    }

    // ========================================
    // decideTier — roll-to-Tier mapping from config chances
    // ========================================

    @Test
    void decideTierFollowsDefaultIntervals() {
        ModConfig.Instance config = ModConfig.Instance.defaults();

        assertEquals(MobTier.VOID, MobTierManager.decideTier(0.01, config));
        assertEquals(MobTier.DOOM, MobTierManager.decideTier(0.05, config));
        assertEquals(MobTier.SHADE, MobTierManager.decideTier(0.1, config));
        assertEquals(MobTier.CINDER, MobTierManager.decideTier(0.3, config));
        assertNull(MobTierManager.decideTier(0.9, config));
    }

    @Test
    void decideTierIntervalBoundaries() {
        ModConfig.Instance config = ModConfig.Instance.defaults();

        // Void [0,0.025), Doom [0.025,0.075), Shade [0.075,0.175), Cinder [0.175,0.375), nothing [0.375,1).
        // Probes sit a hair inside each interval — exact decimal edges can land
        // within one double ulp of the accumulated sums and flip buckets.
        assertEquals(MobTier.DOOM, MobTierManager.decideTier(0.025001, config));
        assertEquals(MobTier.SHADE, MobTierManager.decideTier(0.075001, config));
        assertEquals(MobTier.CINDER, MobTierManager.decideTier(0.175001, config));
        assertNull(MobTierManager.decideTier(0.375001, config));
    }

    @Test
    void decideTierZeroChancesRollNothing() {
        ModConfig.Instance config = new ModConfig.Instance(
                new ModConfig.TierConfig(0.0, 1, 1.0, 1.0),
                new ModConfig.TierConfig(0.0, 1, 1.0, 1.0),
                new ModConfig.TierConfig(0.0, 1, 1.0, 1.0),
                new ModConfig.TierConfig(0.0, 1, 1.0, 1.0),
                60, 0, 60, 0, 5, 4, 4.0f, true, List.of(), List.of(), 5);

        assertNull(MobTierManager.decideTier(0.0, config));
        assertNull(MobTierManager.decideTier(0.5, config));
    }

    @Test
    void decideTierEffectiveSharesMatchContractByEnumeration() {
        ModConfig.Instance config = ModConfig.Instance.defaults();
        int voidTier = 0;
        int doom = 0;
        int shade = 0;
        int cinder = 0;
        int nothing = 0;
        int buckets = 1000;
        for (int i = 0; i < buckets; i++) {
            double roll = (i + 0.5) / buckets;
            MobTier tier = MobTierManager.decideTier(roll, config);
            if (tier == MobTier.VOID) voidTier++;
            else if (tier == MobTier.DOOM) doom++;
            else if (tier == MobTier.SHADE) shade++;
            else if (tier == MobTier.CINDER) cinder++;
            else nothing++;
        }
        assertEquals(25, voidTier, "Void effective share drifted from 2.5%");
        assertEquals(50, doom, "Doom effective share drifted from 5%");
        assertEquals(100, shade, "Shade effective share drifted from 10%");
        assertEquals(200, cinder, "Cinder effective share drifted from 20%");
        assertEquals(625, nothing, "Nothing share drifted from 62.5%");
    }
}
