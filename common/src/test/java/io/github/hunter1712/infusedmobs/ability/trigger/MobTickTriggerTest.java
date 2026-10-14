package io.github.hunter1712.infusedmobs.ability.trigger;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Dispatch tests for {@link MobTickTrigger#tickOnce}.
 * <p>
 * Pins the per-second TICK pass with fakes (string mob holders, canned
 * ability lists) — no {@code ServerTickEvents} or Minecraft bootstrap.
 */
class MobTickTriggerTest {

    private static Ability ability(String id) {
        return new Ability(id, id, TriggerType.TICK, (mob, target, damage) -> {});
    }

    @Test
    void emptyIdSetFiresNothing() {
        List<String> fired = new ArrayList<>();

        int count = MobTickTrigger.tickOnce(
                Set.of(),
                uuid -> "mob",
                mob -> List.of(ability("ward")),
                (abilities, mob) -> fired.add(mob));

        assertEquals(0, count);
        assertTrue(fired.isEmpty());
    }

    @Test
    void missingMobIsSkipped() {
        UUID id = UUID.randomUUID();
        List<String> fired = new ArrayList<>();

        int count = MobTickTrigger.tickOnce(
                Set.of(id),
                uuid -> null,
                mob -> List.of(ability("ward")),
                (abilities, mob) -> fired.add("fired"));

        assertEquals(0, count);
        assertTrue(fired.isEmpty());
    }

    @Test
    void mobWithTickAbilitiesIsFired() {
        UUID id = UUID.randomUUID();
        List<String> applied = new ArrayList<>();
        Ability ward = new Ability("ward", "Ward", TriggerType.TICK,
                (mob, target, damage) -> applied.add("ward"));

        int count = MobTickTrigger.tickOnce(
                Set.of(id),
                uuid -> "mob-1",
                mob -> List.of(ward),
                (abilities, mob) -> {
                    for (Ability ability : abilities) {
                        ability.effect().apply(null, null, 0f);
                    }
                });

        assertEquals(1, count);
        assertEquals(List.of("ward"), applied);
    }

    @Test
    void mobWithoutAbilitiesIsSkipped() {
        UUID id = UUID.randomUUID();
        List<String> fired = new ArrayList<>();

        int count = MobTickTrigger.tickOnce(
                Set.of(id),
                uuid -> "mob-1",
                mob -> List.of(),
                (abilities, mob) -> fired.add("fired"));

        assertEquals(0, count);
        assertTrue(fired.isEmpty());
    }

    @Test
    void eachKnownMobFiresOnce() {
        UUID first = UUID.randomUUID();
        UUID second = UUID.randomUUID();
        Map<UUID, String> mobs = Map.of(first, "mob-1", second, "mob-2");
        List<String> fired = new ArrayList<>();

        int count = MobTickTrigger.tickOnce(
                Set.of(first, second),
                mobs::get,
                mob -> List.of(ability("ward")),
                (abilities, mob) -> fired.add(mob));

        assertEquals(2, count);
        assertEquals(2, fired.size());
        assertTrue(fired.containsAll(List.of("mob-1", "mob-2")));
    }
}
