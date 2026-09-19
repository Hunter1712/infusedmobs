package io.github.hunter1712.infusedmobs.tier;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Seeded-distribution locks for the single Tier-roll decision point.
 * Effective shares at defaults must match the Tier contract (40/20/10).
 */
class TierRollTest {

    private static final double CINDER = 0.4;
    private static final double SHADE = 0.2;
    private static final double DOOM = 0.1;

    @Test
    void doomIntervalStartsAtZero() {
        assertEquals(MobTier.DOOM, TierRoll.decide(0.0, CINDER, SHADE, DOOM));
        assertEquals(MobTier.DOOM, TierRoll.decide(0.05, CINDER, SHADE, DOOM));
        assertEquals(MobTier.DOOM, TierRoll.decide(0.0999, CINDER, SHADE, DOOM));
    }

    @Test
    void shadeIntervalFollowsDoom() {
        assertEquals(MobTier.SHADE, TierRoll.decide(0.1001, CINDER, SHADE, DOOM));
        assertEquals(MobTier.SHADE, TierRoll.decide(0.2, CINDER, SHADE, DOOM));
        assertEquals(MobTier.SHADE, TierRoll.decide(0.2999, CINDER, SHADE, DOOM));
    }

    @Test
    void cinderIntervalFollowsShade() {
        assertEquals(MobTier.CINDER, TierRoll.decide(0.3001, CINDER, SHADE, DOOM));
        assertEquals(MobTier.CINDER, TierRoll.decide(0.5, CINDER, SHADE, DOOM));
        assertEquals(MobTier.CINDER, TierRoll.decide(0.6999, CINDER, SHADE, DOOM));
    }

    @Test
    void remainderRollsNothing() {
        assertNull(TierRoll.decide(0.7001, CINDER, SHADE, DOOM));
        assertNull(TierRoll.decide(0.9, CINDER, SHADE, DOOM));
        assertNull(TierRoll.decide(0.9999, CINDER, SHADE, DOOM));
    }

    @Test
    void effectiveSharesMatchContractByEnumeration() {
        // 1000 evenly spaced rolls approximate the uniform distribution;
        // counts must equal the configured shares within one bucket.
        int doom = 0;
        int shade = 0;
        int cinder = 0;
        int nothing = 0;
        int buckets = 1000;
        for (int i = 0; i < buckets; i++) {
            double roll = (i + 0.5) / buckets;
            MobTier tier = TierRoll.decide(roll, CINDER, SHADE, DOOM);
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
