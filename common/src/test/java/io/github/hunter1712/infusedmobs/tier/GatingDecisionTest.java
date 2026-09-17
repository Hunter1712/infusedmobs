package io.github.hunter1712.infusedmobs.tier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure tests for {@link GatingDecision}, the instantiable World Blacklist
 * plus Gamerule Gate combination behind the static {@link InfusionGate}
 * facade.
 * <p>
 * Each test instantiates a fresh decision core — no Minecraft bootstrap, no
 * shared state, no manual reset. The combination is unchanged: the blacklist
 * dominates, otherwise the per-save rule decides.
 */
class GatingDecisionTest {

    @Test
    void activeWhenNotBlacklistedAndRuleOn() {
        assertEquals(InfusionGate.Status.ACTIVE,
                new GatingDecision().decide(false, Boolean.TRUE, true));
    }

    @Test
    void blacklistedWinsRegardlessOfRule() {
        var decision = new GatingDecision();
        assertEquals(InfusionGate.Status.WORLD_BLACKLISTED,
                decision.decide(true, Boolean.TRUE, true));
        assertEquals(InfusionGate.Status.WORLD_BLACKLISTED,
                decision.decide(true, Boolean.FALSE, true));
        assertEquals(InfusionGate.Status.WORLD_BLACKLISTED,
                decision.decide(true, null, true));
    }

    @Test
    void ruleOffDisablesWhenNotBlacklisted() {
        assertEquals(InfusionGate.Status.RULE_DISABLED,
                new GatingDecision().decide(false, Boolean.FALSE, true));
    }

    @Test
    void missingRuleFallsBackToDefaultOn() {
        assertEquals(InfusionGate.Status.ACTIVE,
                new GatingDecision().decide(false, null, true));
    }

    @Test
    void missingRuleFallsBackToDefaultOff() {
        assertEquals(InfusionGate.Status.RULE_DISABLED,
                new GatingDecision().decide(false, null, false));
    }

    @Test
    void storedValueWinsOverDefault() {
        var decision = new GatingDecision();
        assertEquals(InfusionGate.Status.ACTIVE,
                decision.decide(false, Boolean.TRUE, false));
        assertEquals(InfusionGate.Status.RULE_DISABLED,
                decision.decide(false, Boolean.FALSE, true));
    }
}
