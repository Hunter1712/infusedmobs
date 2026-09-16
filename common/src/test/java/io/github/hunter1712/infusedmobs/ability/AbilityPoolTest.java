package io.github.hunter1712.infusedmobs.ability;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contracts for the instantiable Ability pool core.
 */
class AbilityPoolTest {

    private static AbilityPool poolWith(String... ids) {
        AbilityPool pool = new AbilityPool();
        for (String id : ids) {
            pool.register(id, id, TriggerType.HURT, (mob, target, damage) -> {});
        }
        return pool;
    }

    @Test
    void instancesAreIsolated() {
        AbilityPool a = poolWith("bane");
        AbilityPool b = new AbilityPool();

        assertEquals(1, a.allIds().size());
        assertTrue(b.allIds().isEmpty());
    }

    @Test
    void duplicateRegistrationFailsFast() {
        AbilityPool pool = poolWith("bane");

        assertThrows(IllegalArgumentException.class,
                () -> pool.register("bane", "Bane", TriggerType.HURT, (mob, target, damage) -> {}));
    }

    @Test
    void byIdsPreservesInputOrderAndSkipsUnknown() {
        AbilityPool pool = poolWith("bane", "ward", "chill");

        assertEquals(List.of("ward", "bane"),
                pool.byIds(List.of("ward", "nope", "bane")).stream().map(Ability::id).toList());
    }

    @Test
    void randomRespectsExclusions() {
        AbilityPool pool = new AbilityPool();
        pool.register("bane", "Bane", TriggerType.HURT, (mob, target, damage) -> {});
        pool.register("rupture", "Rupture", TriggerType.DEATH, (mob, target, damage) -> {});

        for (int i = 0; i < 20; i++) {
            assertTrue(pool.random(5, "rupture").stream().noneMatch(a -> a.id().equals("rupture")));
        }
    }

    @Test
    void clearEmptiesPool() {
        AbilityPool pool = poolWith("bane");
        pool.clear();

        assertTrue(pool.allIds().isEmpty());
        assertTrue(pool.random(1).isEmpty());
    }
}
