package io.github.hunter1712.infusedmobs.ability.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins split-copy spawn geometry: the first copy lands on the negative
 * diagonal offset, the second on the positive one. Runs on the pure
 * offset seam — no entities, no bootstrap.
 */
class SplitEffectTest {

    @Test
    void firstCopyTakesNegativeDiagonal() {
        assertEquals(-1.5, SplitEffect.copyOffset(0));
    }

    @Test
    void secondCopyTakesPositiveDiagonal() {
        assertEquals(1.5, SplitEffect.copyOffset(1));
    }

    @Test
    void laterCopiesSpreadInsteadOfStacking() {
        assertEquals(-3.0, SplitEffect.copyOffset(2));
        assertEquals(3.0, SplitEffect.copyOffset(3));
    }
}
