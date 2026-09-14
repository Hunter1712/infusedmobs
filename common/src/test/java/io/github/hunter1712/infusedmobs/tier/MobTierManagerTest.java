package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;

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
