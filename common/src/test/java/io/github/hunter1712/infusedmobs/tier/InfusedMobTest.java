package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic tests for {@link InfusedMob}.
 * <p>
 * Covers the per-trigger ability index. No Minecraft bootstrap needed.
 */
class InfusedMobTest {

    private static Ability ability(String id, TriggerType trigger) {
        return new Ability(id, id, trigger, (mob, target, damage) -> {});
    }

    @Test
    void indexGroupsAbilitiesByTrigger() {
        Ability bane = ability("bane", TriggerType.HURT);
        Ability thorns = ability("thorns", TriggerType.HURT);
        Ability ward = ability("ward", TriggerType.TICK);

        InfusedMob infused = InfusedMob.tiered(MobTier.SHADE, List.of(bane, thorns, ward));

        assertEquals(List.of(bane, thorns), infused.forTrigger(TriggerType.HURT));
        assertEquals(List.of(ward), infused.forTrigger(TriggerType.TICK));
        assertTrue(infused.forTrigger(TriggerType.DEATH).isEmpty());
    }

    @Test
    void thornsOnlyMobIsNotTickCapable() {
        // Thorns is reactive HURT, never passive: a thorns-only mob must not
        // appear in the tick scan, so passive iteration only visits mobs it
        // can actually affect.
        Ability thorns = ability("thorns", TriggerType.HURT);
        InfusedMob infused = InfusedMob.tiered(MobTier.CINDER, List.of(thorns));

        assertEquals(List.of(thorns), infused.forTrigger(TriggerType.HURT));
        assertTrue(infused.forTrigger(TriggerType.TICK).isEmpty());
    }

    @Test
    void splitCopyIndexesDeathAbilitiesAndHasNoTier() {
        Ability combust = ability("combust", TriggerType.DEATH);

        InfusedMob copy = InfusedMob.split(List.of(combust));

        assertEquals(List.of(combust), copy.forTrigger(TriggerType.DEATH));
        assertTrue(copy.forTrigger(TriggerType.HURT).isEmpty());
        assertTrue(copy instanceof InfusedMob.SplitCopy);
    }

    @Test
    void emptyAbilitiesProduceEmptyIndex() {
        InfusedMob infused = InfusedMob.tiered(MobTier.CINDER, List.of());

        assertTrue(infused.forTrigger(TriggerType.TICK).isEmpty());
        assertTrue(infused.forTrigger(TriggerType.HURT).isEmpty());
        assertTrue(infused.forTrigger(TriggerType.DEATH).isEmpty());
    }

    @Test
    void factoriesIsolateCallerListMutation() {
        Ability bane = ability("bane", TriggerType.HURT);
        java.util.List<Ability> source = new java.util.ArrayList<>(List.of(bane));

        InfusedMob infused = InfusedMob.tiered(MobTier.CINDER, source);
        source.clear();

        assertEquals(List.of(bane), infused.abilities());
        assertEquals(List.of(bane), infused.forTrigger(TriggerType.HURT));
    }

    @Test
    void triggerIndexIsUnmodifiable() {
        InfusedMob infused = InfusedMob.tiered(MobTier.CINDER, List.of(ability("bane", TriggerType.HURT)));

        assertThrows(UnsupportedOperationException.class,
                () -> infused.byTrigger().put(TriggerType.TICK, List.of()));
    }
}
