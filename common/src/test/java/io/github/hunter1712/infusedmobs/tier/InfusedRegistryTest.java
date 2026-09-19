package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour tests for {@link InfusedRegistry}, the UUID-keyed live Infused
 * Mob state behind {@link MobTierManager}.
 * <p>
 * Each test instantiates a fresh registry — no shared state, no manual reset.
 */
class InfusedRegistryTest {

    private static Ability ability(String id, TriggerType trigger) {
        return new Ability(id, id, trigger, (mob, target, damage) -> {});
    }

    @Test
    void trackAndFindRoundTrip() {
        var registry = new InfusedRegistry();
        var id = UUID.randomUUID();
        var infused = InfusedMob.tiered(MobTier.DOOM, List.of(ability("bane", TriggerType.HURT)));

        registry.track(id, infused);

        assertEquals(infused, registry.find(id));
    }

    @Test
    void findMissingReturnsNull() {
        assertNull(new InfusedRegistry().find(UUID.randomUUID()));
    }

    @Test
    void trackRejectsNulls() {
        var registry = new InfusedRegistry();
        var infused = InfusedMob.split(List.of());

        assertThrows(NullPointerException.class, () -> registry.track(null, infused));
        assertThrows(NullPointerException.class, () -> registry.track(UUID.randomUUID(), null));
    }

    @Test
    void untrackExistingForgets() {
        var registry = new InfusedRegistry();
        var id = UUID.randomUUID();
        registry.track(id, InfusedMob.split(List.of()));

        assertTrue(registry.untrack(id));
        assertNull(registry.find(id));
    }

    @Test
    void untrackMissingReturnsFalse() {
        assertFalse(new InfusedRegistry().untrack(UUID.randomUUID()));
    }

    @Test
    void instancesAreIsolated() {
        var a = new InfusedRegistry();
        var b = new InfusedRegistry();
        var id = UUID.randomUUID();
        a.track(id, InfusedMob.split(List.of()));

        assertNull(b.find(id));
    }

    @Test
    void tickScanReturnsOnlyTickCapableMobs() {
        var registry = new InfusedRegistry();
        var tickId = UUID.randomUUID();
        var hurtId = UUID.randomUUID();
        var bareId = UUID.randomUUID();
        registry.track(tickId, InfusedMob.tiered(MobTier.SHADE,
                List.of(ability("bane", TriggerType.HURT), ability("wraith", TriggerType.TICK))));
        registry.track(hurtId, InfusedMob.tiered(MobTier.CINDER,
                List.of(ability("bane", TriggerType.HURT))));
        registry.track(bareId, InfusedMob.split(List.of()));

        Set<UUID> tickMobs = registry.tickMobUUIDs();

        assertEquals(Set.of(tickId), tickMobs);
    }

    @Test
    void clearEmptiesRegistry() {
        var registry = new InfusedRegistry();
        registry.track(UUID.randomUUID(), InfusedMob.split(List.of()));
        registry.clear();

        assertTrue(registry.tickMobUUIDs().isEmpty());
        assertNull(registry.find(UUID.randomUUID()));
    }

    @Test
    void splitCopyQueryDistinguishesVariants() {
        var registry = new InfusedRegistry();
        var splitId = UUID.randomUUID();
        var tieredId = UUID.randomUUID();
        var missingId = UUID.randomUUID();
        registry.track(splitId, InfusedMob.split(List.of()));
        registry.track(tieredId, InfusedMob.tiered(MobTier.CINDER,
                List.of(ability("bane", TriggerType.HURT))));

        assertTrue(registry.isSplitCopy(splitId),
                "split copy must report as split copy for Cinder XP treatment");
        assertFalse(registry.isSplitCopy(tieredId),
                "tiered roll must never report as split copy");
        assertFalse(registry.isSplitCopy(missingId),
                "untracked mob must never report as split copy");
    }
}
