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
        List<Ability> result = AbilityRegistry.getRandomAbilities(0, 0, 0);
        assertTrue(result.isEmpty());
    }

    @Test
    void getRandomAbilitiesRespectsCountsWhenRegistryIsEmpty() {
        // With an empty registry, requesting any count > 0 should return
        // fewer items (or empty), but never throw.
        List<Ability> result = AbilityRegistry.getRandomAbilities(3, 2, 1);
        assertNotNull(result);
        // Since the registry is empty, we expect all requests to come back empty
        assertTrue(result.isEmpty());
    }

    @Test
    void getRandomAbilitiesReturnsUnmodifiableList() {
        List<Ability> result = AbilityRegistry.getRandomAbilities(0, 0, 0);
        assertThrows(UnsupportedOperationException.class, () -> result.add(null));
    }
}
