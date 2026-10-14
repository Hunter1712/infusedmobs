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

        assertEquals(MobTier.DOOM, MobTierManager.decideTier(0.05, config));
        assertEquals(MobTier.SHADE, MobTierManager.decideTier(0.2, config));
        assertEquals(MobTier.CINDER, MobTierManager.decideTier(0.5, config));
        assertNull(MobTierManager.decideTier(0.9, config));
    }

    @Test
    void decideTierIntervalBoundaries() {
        ModConfig.Instance config = ModConfig.Instance.defaults();

        // Doom [0,0.1), Shade [0.1,0.3), Cinder [0.3,0.7), nothing [0.7,1).
        assertEquals(MobTier.SHADE, MobTierManager.decideTier(0.15, config));
        assertEquals(MobTier.CINDER, MobTierManager.decideTier(0.35, config));
        assertNull(MobTierManager.decideTier(0.75, config));
    }

    @Test
    void decideTierZeroChancesRollNothing() {
        ModConfig.Instance config = new ModConfig.Instance(
                new ModConfig.TierConfig(0.0, 1, 1.0, 1.0),
                new ModConfig.TierConfig(0.0, 1, 1.0, 1.0),
                new ModConfig.TierConfig(0.0, 1, 1.0, 1.0),
                60, 0, 60, 0, 5, 4, 4.0f, true, List.of(), List.of(), 4);

        assertNull(MobTierManager.decideTier(0.0, config));
        assertNull(MobTierManager.decideTier(0.5, config));
    }

    // ========================================
    // nametagText — roll-to-text mapping
    // ========================================

    @Test
    void nametagTextUsesTierColourForTieredRoll() {
        Rolled rolled = new Rolled.Tiered(MobTier.DOOM, List.of("bane"));

        assertEquals("§cBane §fZombie",
                MobTierManager.nametagText(rolled, List.of("Bane"), "Zombie"));
    }

    @Test
    void nametagTextIsGreyscaleForSplitRoll() {
        Rolled rolled = new Rolled.Split(List.of("ward"));

        assertEquals("§7Ward §fZombie",
                MobTierManager.nametagText(rolled, List.of("Ward"), "Zombie"));
    }

    @Test
    void nametagTextOmitsEmptyAbilityNames() {
        assertEquals("§aZombie",
                MobTierManager.nametagText(
                        new Rolled.Tiered(MobTier.CINDER, List.of()), List.of(), "Zombie"));
        assertEquals("§7Zombie",
                MobTierManager.nametagText(
                        new Rolled.Split(List.of()), List.of(), "Zombie"));
    }

    @Test
    void decideTierEffectiveSharesMatchContractByEnumeration() {
        ModConfig.Instance config = ModConfig.Instance.defaults();
        int doom = 0;
        int shade = 0;
        int cinder = 0;
        int nothing = 0;
        int buckets = 1000;
        for (int i = 0; i < buckets; i++) {
            double roll = (i + 0.5) / buckets;
            MobTier tier = MobTierManager.decideTier(roll, config);
            if (tier == MobTier.DOOM) doom++;
            else if (tier == MobTier.SHADE) shade++;
            else if (tier == MobTier.CINDER) cinder++;
            else nothing++;
        }
        assertEquals(100, doom, "Doom effective share drifted from 10%");
        assertEquals(200, shade, "Shade effective share drifted from 20%");
        assertEquals(400, cinder, "Cinder effective share drifted from 40%");
        assertEquals(300, nothing, "Nothing share drifted from 30%");
    }
}
