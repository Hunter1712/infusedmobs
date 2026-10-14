package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.config.InstanceBuilder;
import io.github.hunter1712.infusedmobs.config.ModConfig;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class InfusionStatsTest {

    @Test
    void xpMultiplierUsesTierThenCinderFallbackThenOne() {
        ModConfig.Instance config = InstanceBuilder.valid()
                .cinder(new ModConfig.TierConfig(0.4, 1, 1.5, 1.5))
                .shade(new ModConfig.TierConfig(0.2, 2, 2.0, 2.0))
                .doom(new ModConfig.TierConfig(0.1, 3, 4.0, 4.0))
                .build();
        assertEquals(4.0, InfusionStats.xpMultiplier(MobTier.DOOM, false, config));
        assertEquals(1.5, InfusionStats.xpMultiplier(null, true, config));
        assertEquals(1.0, InfusionStats.xpMultiplier(null, false, config));
    }
}
