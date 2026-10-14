package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.TestAbilities;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.test.IsolatedState;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour tests for {@link InfusedRegistry}, the UUID-keyed live roll
 * state behind {@link MobTierManager}.
 * <p>
 * Each test instantiates a fresh registry — no shared state, no manual reset.
 * Ability ids resolve through the global pool, isolated per test.
 */
@ExtendWith(IsolatedState.class)
class InfusedRegistryTest {

    @Test
    void putAndGetRoundTrip() {
        var registry = new InfusedRegistry();
        var id = UUID.randomUUID();
        var rolled = new Rolled.Tiered(MobTier.DOOM, List.of("bane", "rupture"));

        registry.put(id, rolled);

        assertEquals(rolled, registry.get(id));
    }

    @Test
    void getMissingReturnsNull() {
        assertNull(new InfusedRegistry().get(UUID.randomUUID()));
    }

    @Test
    void putRejectsNulls() {
        var registry = new InfusedRegistry();
        var rolled = new Rolled.Split(List.of());

        assertThrows(NullPointerException.class, () -> registry.put(null, rolled));
        assertThrows(NullPointerException.class, () -> registry.put(UUID.randomUUID(), null));
    }

    @Test
    void removeExistingForgets() {
        var registry = new InfusedRegistry();
        var id = UUID.randomUUID();
        registry.put(id, new Rolled.Split(List.of()));

        assertTrue(registry.remove(id));
        assertNull(registry.get(id));
    }

    @Test
    void removeMissingReturnsFalse() {
        assertFalse(new InfusedRegistry().remove(UUID.randomUUID()));
    }

    @Test
    void instancesAreIsolated() {
        var a = new InfusedRegistry();
        var b = new InfusedRegistry();
        var id = UUID.randomUUID();
        a.put(id, new Rolled.Split(List.of()));

        assertNull(b.get(id));
    }

    @Test
    void tickScanReturnsOnlyTickCapableMobs() {
        TestAbilities.register("bane", TriggerType.HURT);
        TestAbilities.register("wraith", TriggerType.TICK);
        var registry = new InfusedRegistry();
        var tickId = UUID.randomUUID();
        var hurtId = UUID.randomUUID();
        var bareId = UUID.randomUUID();
        registry.put(tickId, new Rolled.Tiered(MobTier.SHADE, List.of("bane", "wraith")));
        registry.put(hurtId, new Rolled.Tiered(MobTier.CINDER, List.of("bane")));
        registry.put(bareId, new Rolled.Split(List.of()));

        Set<UUID> tickMobs = registry.tickMobUUIDs();

        assertEquals(Set.of(tickId), tickMobs);
    }

    @Test
    void clearEmptiesRegistry() {
        var registry = new InfusedRegistry();
        registry.put(UUID.randomUUID(), new Rolled.Split(List.of()));
        registry.clear();

        assertTrue(registry.tickMobUUIDs().isEmpty());
        assertNull(registry.get(UUID.randomUUID()));
    }

    @Test
    void splitCopyQueryDistinguishesVariants() {
        var registry = new InfusedRegistry();
        var splitId = UUID.randomUUID();
        var tieredId = UUID.randomUUID();
        var missingId = UUID.randomUUID();
        registry.put(splitId, new Rolled.Split(List.of()));
        registry.put(tieredId, new Rolled.Tiered(MobTier.CINDER, List.of("bane")));

        assertTrue(registry.isSplitCopy(splitId),
                "split copy must report as split copy for Cinder XP treatment");
        assertFalse(registry.isSplitCopy(tieredId),
                "tiered roll must never report as split copy");
        assertFalse(registry.isSplitCopy(missingId),
                "untracked mob must never report as split copy");
    }
}
