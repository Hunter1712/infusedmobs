package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.config.ModConfig;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic tests for {@link MobTierManager}.
 * <p>
 * Covers the {@link InfuseStatus} decision ({@link MobTierManager#canInfuse})
 * and the {@link InfusedMob} trigger index. No Minecraft bootstrap needed.
 */
class MobTierManagerTest {

    private static Ability ability(String id, TriggerType trigger) {
        return new Ability(id, id, trigger, (mob, target, damage) -> {});
    }

    // ========================================
    // canInfuse — blacklist + enabled rule → status
    // ========================================

    @Test
    void activeWhenNotBlacklistedAndRuleOn() {
        assertEquals(MobTierManager.InfuseStatus.ACTIVE,
                MobTierManager.canInfuse(false, Boolean.TRUE));
    }

    @Test
    void blacklistedWinsRegardlessOfRule() {
        assertEquals(MobTierManager.InfuseStatus.WORLD_BLACKLISTED,
                MobTierManager.canInfuse(true, Boolean.TRUE));
        assertEquals(MobTierManager.InfuseStatus.WORLD_BLACKLISTED,
                MobTierManager.canInfuse(true, Boolean.FALSE));
        assertEquals(MobTierManager.InfuseStatus.WORLD_BLACKLISTED,
                MobTierManager.canInfuse(true, null));
    }

    @Test
    void ruleOffDisablesWhenNotBlacklisted() {
        assertEquals(MobTierManager.InfuseStatus.RULE_DISABLED,
                MobTierManager.canInfuse(false, Boolean.FALSE));
    }

    @Test
    void missingRuleFallsBackToDefaultOn() {
        // A fresh world save has no stored value — the rule default is true.
        assertEquals(MobTierManager.InfuseStatus.ACTIVE,
                MobTierManager.canInfuse(false, null));
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

        assertEquals(MobTierManager.InfuseStatus.WORLD_BLACKLISTED,
                MobTierManager.canInfuse(vanillaCfg.isWorldBlacklisted("minecraft:overworld"), Boolean.TRUE));
        assertEquals(MobTierManager.InfuseStatus.WORLD_BLACKLISTED,
                MobTierManager.canInfuse(vanillaCfg.isWorldBlacklisted("minecraft:the_end"), Boolean.TRUE));
        assertEquals(MobTierManager.InfuseStatus.ACTIVE,
                MobTierManager.canInfuse(vanillaCfg.isWorldBlacklisted("minecraft:the_nether"), Boolean.TRUE));

        // Modded dimension: same per-dimension semantics (e.g. twilightforest, aether)
        var moddedCfg = ModConfig.Instance.defaults()
                .withWorldBlacklist(List.of("twilightforest:twilight_forest"));
        assertTrue(moddedCfg.isWorldBlacklisted("twilightforest:twilight_forest"));
        assertTrue(!moddedCfg.isWorldBlacklisted("minecraft:overworld"));
        assertEquals(MobTierManager.InfuseStatus.WORLD_BLACKLISTED,
                MobTierManager.canInfuse(moddedCfg.isWorldBlacklisted("twilightforest:twilight_forest"), Boolean.TRUE));
        assertEquals(MobTierManager.InfuseStatus.ACTIVE,
                MobTierManager.canInfuse(moddedCfg.isWorldBlacklisted("minecraft:overworld"), Boolean.TRUE));
        assertEquals(MobTierManager.InfuseStatus.ACTIVE,
                MobTierManager.canInfuse(moddedCfg.isWorldBlacklisted("aether:the_aether"), Boolean.TRUE));
    }

    @Test
    void worldBlacklistDominatesRegardlessOfGamerule() {
        // World Blacklist (per-dimension) dominates even if Gamerule Gate (per-world) is true (spec story 12)
        var cfg = ModConfig.Instance.defaults()
                .withWorldBlacklist(List.of("minecraft:overworld", "twilightforest:twilight_forest"));
        assertEquals(MobTierManager.InfuseStatus.WORLD_BLACKLISTED,
                MobTierManager.canInfuse(cfg.isWorldBlacklisted("minecraft:overworld"), Boolean.TRUE));
        assertEquals(MobTierManager.InfuseStatus.WORLD_BLACKLISTED,
                MobTierManager.canInfuse(cfg.isWorldBlacklisted("minecraft:overworld"), Boolean.FALSE));
        assertEquals(MobTierManager.InfuseStatus.WORLD_BLACKLISTED,
                MobTierManager.canInfuse(cfg.isWorldBlacklisted("twilightforest:twilight_forest"), Boolean.TRUE));
        assertEquals(MobTierManager.InfuseStatus.WORLD_BLACKLISTED,
                MobTierManager.canInfuse(cfg.isWorldBlacklisted("twilightforest:twilight_forest"), null));
    }

    @Test
    void gameruleGateOffDisablesAllNonBlacklistedDimensions() {
        // Gamerule Gate is per-world (not per-dimension); when off, any non-blacklisted dimension is RULE_DISABLED (spec story 11: !blacklisted && gamerule)
        var cfg = ModConfig.Instance.defaults()
                .withWorldBlacklist(List.of("minecraft:overworld", "minecraft:the_end"));
        assertEquals(MobTierManager.InfuseStatus.RULE_DISABLED,
                MobTierManager.canInfuse(cfg.isWorldBlacklisted("minecraft:the_nether"), Boolean.FALSE));
        var moddedCfg = ModConfig.Instance.defaults()
                .withWorldBlacklist(List.of("minecraft:overworld"));
        assertEquals(MobTierManager.InfuseStatus.RULE_DISABLED,
                MobTierManager.canInfuse(moddedCfg.isWorldBlacklisted("twilightforest:twilight_forest"), Boolean.FALSE));
        assertEquals(MobTierManager.InfuseStatus.RULE_DISABLED,
                MobTierManager.canInfuse(moddedCfg.isWorldBlacklisted("aether:the_aether"), Boolean.FALSE));
    }

    // ========================================
    // InfusedMob — trigger index + tier queries
    // ========================================

    @Test
    void indexGroupsAbilitiesByTrigger() {
        Ability bane = ability("bane", TriggerType.HURT);
        Ability thorns = ability("thorns", TriggerType.TICK);

        InfusedMob infused = InfusedMob.tiered(MobTier.SHADE, List.of(bane, thorns));

        assertEquals(List.of(bane), infused.forTrigger(TriggerType.HURT));
        assertEquals(List.of(thorns), infused.forTrigger(TriggerType.TICK));
        assertTrue(infused.forTrigger(TriggerType.DEATH).isEmpty());
    }

    @Test
    void splitCopyIndexesDeathAbilitiesAndHasNoTier() {
        Ability combust = ability("combust", TriggerType.DEATH);

        InfusedMob copy = InfusedMob.split(List.of(combust));

        assertEquals(List.of(combust), copy.forTrigger(TriggerType.DEATH));
        assertTrue(copy.forTrigger(TriggerType.HURT).isEmpty());
        assertTrue(copy instanceof InfusedMob.SplitCopyMob);
    }

    @Test
    void emptyAbilitiesProduceEmptyIndex() {
        InfusedMob infused = InfusedMob.tiered(MobTier.CINDER, List.of());

        assertTrue(infused.forTrigger(TriggerType.TICK).isEmpty());
        assertTrue(infused.forTrigger(TriggerType.HURT).isEmpty());
        assertTrue(infused.forTrigger(TriggerType.DEATH).isEmpty());
    }
}
