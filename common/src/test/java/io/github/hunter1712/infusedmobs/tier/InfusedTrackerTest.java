package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.config.ModConfig;
import io.github.hunter1712.infusedmobs.test.IsolatedState;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour tests for {@link InfusedTracker}, the in-memory Infused Mob
 * registry. The UUID-keyed core (track / find / untrack) and the
 * tick-capable scan execute without Minecraft bootstrap; the Mob-based
 * queries are one-line delegates over {@link InfusedTracker#find}.
 */
@ExtendWith(IsolatedState.class)
class InfusedTrackerTest {

    private static Ability ability(String id, TriggerType trigger) {
        return new Ability(id, id, trigger, (mob, target, damage) -> {});
    }

    @Test
    void trackAndFindRoundTrip() {
        var id = UUID.randomUUID();
        var infused = InfusedMob.tiered(MobTier.DOOM, List.of(ability("bane", TriggerType.HURT)));

        InfusedTracker.track(id, infused);

        assertEquals(infused, InfusedTracker.find(id));
    }

    @Test
    void findMissingReturnsNull() {
        assertNull(InfusedTracker.find(UUID.randomUUID()));
    }

    @Test
    void untrackExistingForgets() {
        var id = UUID.randomUUID();
        InfusedTracker.track(id, InfusedMob.split(List.of()));

        assertTrue(InfusedTracker.untrack(id));
        assertNull(InfusedTracker.find(id));
    }

    @Test
    void untrackMissingReturnsFalse() {
        assertFalse(InfusedTracker.untrack(UUID.randomUUID()));
    }

    @Test
    void tickScanReturnsOnlyTickCapableMobs() {
        var tickId = UUID.randomUUID();
        var hurtId = UUID.randomUUID();
        var bareId = UUID.randomUUID();
        InfusedTracker.track(tickId, InfusedMob.tiered(MobTier.SHADE,
                List.of(ability("bane", TriggerType.HURT), ability("wraith", TriggerType.TICK))));
        InfusedTracker.track(hurtId, InfusedMob.tiered(MobTier.CINDER,
                List.of(ability("bane", TriggerType.HURT))));
        InfusedTracker.track(bareId, InfusedMob.split(List.of()));

        Set<UUID> tickMobs = InfusedTracker.getTickMobUUIDs();

        assertEquals(Set.of(tickId), tickMobs);
    }

    @Test
    void clearEmptiesRegistry() {
        InfusedTracker.track(UUID.randomUUID(), InfusedMob.split(List.of()));
        InfusedTracker.clear();

        assertTrue(InfusedTracker.getTickMobUUIDs().isEmpty());
        assertNull(InfusedTracker.find(UUID.randomUUID()));
    }

    @Test
    void splitCopyQueryDistinguishesVariants() {
        var splitId = UUID.randomUUID();
        var tieredId = UUID.randomUUID();
        var missingId = UUID.randomUUID();
        InfusedTracker.track(splitId, InfusedMob.split(List.of()));
        InfusedTracker.track(tieredId, InfusedMob.tiered(MobTier.CINDER,
                List.of(ability("bane", TriggerType.HURT))));

        assertTrue(InfusedTracker.isSplitCopy(splitId),
                "split copy must report as split copy for Cinder XP treatment");
        assertFalse(InfusedTracker.isSplitCopy(tieredId),
                "tiered roll must never report as split copy");
        assertFalse(InfusedTracker.isSplitCopy(missingId),
                "untracked mob must never report as split copy");
    }

    @Test
    void splitCopyCinderXpContract() {
        // Split copies carry the full documented Cinder experience treatment
        // through the same path as Tiered rolls (see LivingEntityMixin).
        assertEquals(MobTier.CINDER.xpMultiplier(),
                ModConfig.Instance.defaults().forTier(MobTier.CINDER).xpMultiplier(),
                "split-copy XP must equal the Cinder rule");
    }
}
