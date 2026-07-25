package io.github.hunter1712.infusedmobs.trigger;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic tests for {@link DamageContext}.
 * <p>
 * Note: {@code getAndClear()} resets the internal state to 0, so
 * call it at the start of each test to ensure isolation.
 */
class DamageContextTest {

    @Test
    void getAndClearReturnsZeroByDefault() {
        DamageContext.getAndClear(); // reset
        assertEquals(0.0f, DamageContext.getAndClear());
    }

    @Test
    void setStoresValueAndGetAndClearReturnsIt() {
        DamageContext.getAndClear(); // reset
        DamageContext.set(25.5f);
        assertEquals(25.5f, DamageContext.getAndClear());
    }

    @Test
    void getAndClearClearsToZero() {
        DamageContext.set(10.0f);
        DamageContext.getAndClear(); // consume
        assertEquals(0.0f, DamageContext.getAndClear());
    }

    @Test
    void setOverwritesPreviousValue() {
        DamageContext.getAndClear(); // reset
        DamageContext.set(5.0f);
        DamageContext.set(15.0f);
        assertEquals(15.0f, DamageContext.getAndClear());
    }

    @Test
    void handlesNegativeValues() {
        DamageContext.getAndClear(); // reset
        DamageContext.set(-3.0f);
        assertEquals(-3.0f, DamageContext.getAndClear());
    }

    @Test
    void handlesZeroExplicitly() {
        DamageContext.getAndClear(); // reset
        DamageContext.set(0.0f);
        assertEquals(0.0f, DamageContext.getAndClear());
    }
}
