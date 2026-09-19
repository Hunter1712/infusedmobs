package io.github.hunter1712.infusedmobs.ability.effect;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pins Combust falloff: full base at point-blank, half at mid-radius,
 * floored at 1.0 on and past the rim. Runs on the pure seam — no bootstrap.
 */
class CombustEffectTest {

    @Test
    void pointBlankDealsFullBase() {
        assertEquals(4.0f, CombustEffect.damageFor(0, 8.0));
    }

    @Test
    void midRadiusHalvesDamage() {
        assertEquals(2.0f, CombustEffect.damageFor(4.0, 8.0));
    }

    @Test
    void rimAndBeyondFloorAtOne() {
        assertEquals(1.0f, CombustEffect.damageFor(8.0, 8.0));
        assertEquals(1.0f, CombustEffect.damageFor(9.0, 8.0));
    }
}
