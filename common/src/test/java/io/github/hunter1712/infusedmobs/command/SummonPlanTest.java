package io.github.hunter1712.infusedmobs.command;

import io.github.hunter1712.infusedmobs.tier.InfusionGate;
import io.github.hunter1712.infusedmobs.tier.MobTier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SummonPlanTest {

    @Test
    void parsesKnownTierCaseInsensitively() {
        assertEquals(MobTier.DOOM, SummonPlan.parseTier("doom").orElseThrow());
        assertEquals(MobTier.SHADE, SummonPlan.parseTier("Shade").orElseThrow());
    }

    @Test
    void unknownTierIsEmpty() {
        assertTrue(SummonPlan.parseTier("bogus").isEmpty());
    }

    @Test
    void activeGateHasNoRefusal() {
        assertTrue(SummonPlan.refusalFor(InfusionGate.Status.ACTIVE, "minecraft:overworld").isEmpty());
    }

    @Test
    void blacklistedGateRefuses() {
        assertTrue(SummonPlan.refusalFor(
                InfusionGate.Status.WORLD_BLACKLISTED, "minecraft:overworld").isPresent());
    }

    @Test
    void tierOptionsListsEveryTier() {
        String options = SummonPlan.tierOptions();
        for (MobTier tier : MobTier.values()) {
            assertTrue(options.contains(tier.name().toLowerCase(java.util.Locale.ROOT)));
        }
    }
}
