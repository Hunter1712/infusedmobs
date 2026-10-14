package io.github.hunter1712.infusedmobs.ability;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Seeded draws are deterministic and honour exclusions. */
class SeededDrawTest {

    private static AbilityRegistry pool() {
        AbilityRegistry registry = new AbilityRegistry();
        registry.add("bane", "Bane", TriggerType.HURT, (m, t, d) -> {});
        registry.add("chill", "Chill", TriggerType.HURT, (m, t, d) -> {});
        registry.add("rupture", "Rupture", TriggerType.DEATH, (m, t, d) -> {});
        registry.add("combust", "Combust", TriggerType.DEATH, (m, t, d) -> {});
        return registry;
    }

    @Test
    void sameSeedDrawsSameSet() {
        AbilityRegistry registry = pool();
        List<String> first = registry.random(2, new Random(42)).stream()
                .map(Ability::id).sorted().collect(Collectors.toList());
        List<String> second = registry.random(2, new Random(42)).stream()
                .map(Ability::id).sorted().collect(Collectors.toList());
        assertEquals(first, second);
    }

    @Test
    void exclusionsNeverDrawn() {
        AbilityRegistry registry = pool();
        for (long seed = 0; seed < 20; seed++) {
            List<Ability> drawn = registry.random(4, new Random(seed), AbilityRegistry.RUPTURE_ID);
            assertTrue(drawn.stream().noneMatch(a -> a.id().equals(AbilityRegistry.RUPTURE_ID)));
        }
    }
}
