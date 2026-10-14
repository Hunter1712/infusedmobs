package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.config.ModConfig;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
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
}
