package io.github.hunter1712.infusedmobs.ability;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AbilityRegistry} query methods.
 * <p>
 * These tests work with the empty registry (before {@code registerAll()} is called)
 * so they don't require Minecraft class initialisation. They verify the sampling
 * logic handles edge cases correctly.
 */
class AbilityRegistryTest {

    @Test
    void getRandomAbilitiesReturnsEmptyWhenAllCountsZero() {
        List<Ability> result = AbilityRegistry.getRandomAbilities(0);
        assertTrue(result.isEmpty());
    }

    @Test
    void getRandomAbilitiesReturnsEmptyWhenRegistryIsEmpty() {
        // With an empty registry, requesting any count > 0 should return
        // an empty list, but never throw.
        List<Ability> result = AbilityRegistry.getRandomAbilities(5);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getRandomAbilitiesReturnsUnmodifiableList() {
        List<Ability> result = AbilityRegistry.getRandomAbilities(0);
        assertThrows(UnsupportedOperationException.class, () -> result.add(null));
    }
}
