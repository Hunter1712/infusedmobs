package io.github.hunter1712.infusedmobs.gamerules;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic tests for {@link ModGameRules} resolution helper.
 * <p>
 * These tests never touch Minecraft state — they verify only the
 * stored-vs-default fallback. The blacklist+rule composition lives in
 * {@link io.github.hunter1712.infusedmobs.tier.MobTierManager#canInfuse}
 * and is covered by {@code MobTierManagerTest}.
 */
class ModGameRulesTest {

    // ========================================
    // resolveRule — stored value vs default fallback
    // ========================================

    @Test
    void missingStoredValueFallsBackToDefault() {
        assertTrue(ModGameRules.resolveRule(null, true));
        assertFalse(ModGameRules.resolveRule(null, false));
    }

    @Test
    void storedValueWinsOverDefault() {
        assertTrue(ModGameRules.resolveRule(Boolean.TRUE, false));
        assertFalse(ModGameRules.resolveRule(Boolean.FALSE, true));
    }
}
