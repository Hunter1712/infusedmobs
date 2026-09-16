package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.config.ModConfig;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic tests for {@link InfusionGate}.
 * <p>
 * Covers the Gamerule Gate + World Blacklist decision per dimension:
 * blacklist dominates, otherwise the per-save gamerule decides.
 * No Minecraft bootstrap needed.
 */
class InfusionGateTest {

    // ========================================
    // status — blacklist + enabled rule → Status
    // ========================================

    @Test
    void activeWhenNotBlacklistedAndRuleOn() {
        assertEquals(InfusionGate.Status.ACTIVE,
                InfusionGate.status(false, Boolean.TRUE));
    }

    @Test
    void blacklistedWinsRegardlessOfRule() {
        assertEquals(InfusionGate.Status.WORLD_BLACKLISTED,
                InfusionGate.status(true, Boolean.TRUE));
        assertEquals(InfusionGate.Status.WORLD_BLACKLISTED,
                InfusionGate.status(true, Boolean.FALSE));
        assertEquals(InfusionGate.Status.WORLD_BLACKLISTED,
                InfusionGate.status(true, null));
    }

    @Test
    void ruleOffDisablesWhenNotBlacklisted() {
        assertEquals(InfusionGate.Status.RULE_DISABLED,
                InfusionGate.status(false, Boolean.FALSE));
    }

    @Test
    void missingRuleFallsBackToDefaultOn() {
        // A fresh world save has no stored value — the rule default is true.
        assertEquals(InfusionGate.Status.ACTIVE,
                InfusionGate.status(false, null));
    }

    @Test
    void worldBlacklistBlocksOnlyListedDimensions() {
        // World Blacklist is per-dimension (CONTEXT.md: World Blacklist); Gamerule Gate is per-world (CONTEXT.md: Gamerule Gate).
        // Nether-only (blacklist overworld+end) is one example; logic is generic for any dimension id, including modded.
        var vanillaCfg = ModConfig.Instance.defaults()
                .withWorldBlacklist(List.of("minecraft:overworld", "minecraft:the_end"));

        assertTrue(vanillaCfg.isWorldBlacklisted("minecraft:overworld"));
        assertTrue(vanillaCfg.isWorldBlacklisted("minecraft:the_end"));
        assertTrue(!vanillaCfg.isWorldBlacklisted("minecraft:the_nether"));

        assertEquals(InfusionGate.Status.WORLD_BLACKLISTED,
                InfusionGate.status(vanillaCfg.isWorldBlacklisted("minecraft:overworld"), Boolean.TRUE));
        assertEquals(InfusionGate.Status.WORLD_BLACKLISTED,
                InfusionGate.status(vanillaCfg.isWorldBlacklisted("minecraft:the_end"), Boolean.TRUE));
        assertEquals(InfusionGate.Status.ACTIVE,
                InfusionGate.status(vanillaCfg.isWorldBlacklisted("minecraft:the_nether"), Boolean.TRUE));

        // Modded dimension: same per-dimension semantics (e.g. twilightforest, aether)
        var moddedCfg = ModConfig.Instance.defaults()
                .withWorldBlacklist(List.of("twilightforest:twilight_forest"));
        assertTrue(moddedCfg.isWorldBlacklisted("twilightforest:twilight_forest"));
        assertTrue(!moddedCfg.isWorldBlacklisted("minecraft:overworld"));
        assertEquals(InfusionGate.Status.WORLD_BLACKLISTED,
                InfusionGate.status(moddedCfg.isWorldBlacklisted("twilightforest:twilight_forest"), Boolean.TRUE));
        assertEquals(InfusionGate.Status.ACTIVE,
                InfusionGate.status(moddedCfg.isWorldBlacklisted("minecraft:overworld"), Boolean.TRUE));
        assertEquals(InfusionGate.Status.ACTIVE,
                InfusionGate.status(moddedCfg.isWorldBlacklisted("aether:the_aether"), Boolean.TRUE));
    }

    @Test
    void worldBlacklistDominatesRegardlessOfGamerule() {
        // World Blacklist (per-dimension) dominates even if Gamerule Gate (per-world) is true (spec story 12)
        var cfg = ModConfig.Instance.defaults()
                .withWorldBlacklist(List.of("minecraft:overworld", "twilightforest:twilight_forest"));
        assertEquals(InfusionGate.Status.WORLD_BLACKLISTED,
                InfusionGate.status(cfg.isWorldBlacklisted("minecraft:overworld"), Boolean.TRUE));
        assertEquals(InfusionGate.Status.WORLD_BLACKLISTED,
                InfusionGate.status(cfg.isWorldBlacklisted("minecraft:overworld"), Boolean.FALSE));
        assertEquals(InfusionGate.Status.WORLD_BLACKLISTED,
                InfusionGate.status(cfg.isWorldBlacklisted("twilightforest:twilight_forest"), Boolean.TRUE));
        assertEquals(InfusionGate.Status.WORLD_BLACKLISTED,
                InfusionGate.status(cfg.isWorldBlacklisted("twilightforest:twilight_forest"), null));
    }

    @Test
    void gameruleGateOffDisablesAllNonBlacklistedDimensions() {
        // Gamerule Gate is per-world (not per-dimension); when off, any non-blacklisted dimension is RULE_DISABLED (spec story 11: !blacklisted && gamerule)
        var cfg = ModConfig.Instance.defaults()
                .withWorldBlacklist(List.of("minecraft:overworld", "minecraft:the_end"));
        assertEquals(InfusionGate.Status.RULE_DISABLED,
                InfusionGate.status(cfg.isWorldBlacklisted("minecraft:the_nether"), Boolean.FALSE));
        var moddedCfg = ModConfig.Instance.defaults()
                .withWorldBlacklist(List.of("minecraft:overworld"));
        assertEquals(InfusionGate.Status.RULE_DISABLED,
                InfusionGate.status(moddedCfg.isWorldBlacklisted("twilightforest:twilight_forest"), Boolean.FALSE));
        assertEquals(InfusionGate.Status.RULE_DISABLED,
                InfusionGate.status(moddedCfg.isWorldBlacklisted("aether:the_aether"), Boolean.FALSE));
    }
}
