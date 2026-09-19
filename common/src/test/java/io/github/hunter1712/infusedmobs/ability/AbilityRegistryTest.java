package io.github.hunter1712.infusedmobs.ability;

import io.github.hunter1712.infusedmobs.test.IsolatedState;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link AbilityRegistry} query methods.
 * <p>
 * The pool is populated with fake abilities via the package-private
 * {@link AbilityRegistry#all} test hook — no Minecraft class initialisation
 * required. {@link #reset()} keeps every test deterministic, and
 * {@link #cleanup()} restores the empty-registry state for other test
 * classes that assume no abilities are registered.
 */
@ExtendWith(IsolatedState.class)
class AbilityRegistryTest {

    private static void register(String id, TriggerType trigger) {
        TestAbilities.register(id, trigger);
    }

    // ========================================
    // Empty-registry edge cases
    // ========================================

    @Test
    void getRandomAbilitiesReturnsEmptyWhenAllCountsZero() {
        register("bane", TriggerType.HURT);
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

    // ========================================
    // Drawing from a populated pool
    // ========================================

    @Test
    void drawReturnsRequestedCountWithoutDuplicates() {
        register("bane", TriggerType.HURT);
        register("chill", TriggerType.HURT);
        register("ward", TriggerType.TICK);
        register("rupture", TriggerType.DEATH);

        List<Ability> result = AbilityRegistry.getRandomAbilities(3);
        assertEquals(3, result.size());
        assertEquals(3, result.stream().map(Ability::id).distinct().count());
        // Every drawn ability comes from the registered pool
        assertTrue(result.stream().allMatch(a -> List.of("bane", "chill", "ward", "rupture")
                .contains(a.id())));
    }

    @Test
    void drawCappedByPoolSize() {
        register("bane", TriggerType.HURT);
        register("ward", TriggerType.TICK);

        List<Ability> result = AbilityRegistry.getRandomAbilities(10);
        assertEquals(2, result.size());
    }

    // ========================================
    // Id-based exclusion (Rupture split copies)
    // ========================================

    @Test
    void excludedIdNeverAppears() {
        register("bane", TriggerType.HURT);
        register("combust", TriggerType.DEATH);
        register("rupture", TriggerType.DEATH);

        // Repeated draws must never contain the excluded id
        for (int i = 0; i < 20; i++) {
            List<Ability> result = AbilityRegistry.getRandomAbilities(5, "rupture");
            assertTrue(result.stream().noneMatch(a -> a.id().equals("rupture")),
                    "draw #" + i + " contained the excluded ability");
        }
    }

    @Test
    void onlyExcludedIdIsRemoved_otherDeathAbilitiesStillDrawable() {
        register("bane", TriggerType.HURT);
        register("combust", TriggerType.DEATH);
        register("rupture", TriggerType.DEATH);

        // The user requirement: split copies may roll ANY ability except
        // Rupture itself — Combust (also DEATH-trigger) must stay available.
        boolean sawCombust = false;
        for (int i = 0; i < 50; i++) {
            List<Ability> result = AbilityRegistry.getRandomAbilities(2, "rupture");
            if (result.stream().anyMatch(a -> a.id().equals("combust"))) {
                sawCombust = true;
                break;
            }
        }
        assertTrue(sawCombust, "Combust should be drawable when only rupture is excluded");
    }

    @Test
    void unknownExcludedIdIsNoop() {
        register("bane", TriggerType.HURT);
        register("ward", TriggerType.TICK);

        List<Ability> result = AbilityRegistry.getRandomAbilities(2, "does-not-exist");
        assertEquals(2, result.size());
    }

    @Test
    void excludingEverythingReturnsEmpty() {
        register("rupture", TriggerType.DEATH);

        List<Ability> result = AbilityRegistry.getRandomAbilities(1, "rupture");
        assertTrue(result.isEmpty());
    }

    // ========================================
    // Id lookup order contract (#14)
    // ========================================

    @Test
    void getAbilitiesByIdsPreservesInputOrder() {
        register("bane", TriggerType.HURT);
        register("chill", TriggerType.HURT);
        register("ward", TriggerType.TICK);

        List<Ability> result = AbilityRegistry.getAbilitiesByIds(List.of("ward", "bane"));

        assertEquals(List.of("ward", "bane"), result.stream().map(Ability::id).toList());
    }

    @Test
    void getAbilitiesByIdsSkipsUnknownIds() {
        register("bane", TriggerType.HURT);

        List<Ability> result = AbilityRegistry.getAbilitiesByIds(List.of("bane", "nope", "ward"));

        assertEquals(List.of("bane"), result.stream().map(Ability::id).toList());
    }

    // ========================================
    // Instance isolation (fresh registry per test, no reset)
    // ========================================

    private static AbilityRegistry registryWith(String... ids) {
        AbilityRegistry registry = new AbilityRegistry();
        for (String id : ids) {
            registry.add(id, id, TriggerType.HURT, (mob, target, damage) -> {});
        }
        return registry;
    }

    @Test
    void instancesAreIsolated() {
        AbilityRegistry a = registryWith("bane");
        AbilityRegistry b = new AbilityRegistry();

        assertEquals(1, a.allIds().size());
        assertTrue(b.allIds().isEmpty());
    }

    @Test
    void duplicateRegistrationFailsFast() {
        AbilityRegistry registry = registryWith("bane");

        assertThrows(IllegalArgumentException.class,
                () -> registry.add("bane", "Bane", TriggerType.HURT, (mob, target, damage) -> {}));
    }

    @Test
    void clearEmptiesRegistry() {
        AbilityRegistry registry = registryWith("bane");
        registry.clear();

        assertTrue(registry.allIds().isEmpty());
        assertTrue(registry.random(1).isEmpty());
    }
}
