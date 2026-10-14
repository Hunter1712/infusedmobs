package io.github.hunter1712.infusedmobs.ability;

import io.github.hunter1712.infusedmobs.platform.Platform;
import io.github.hunter1712.infusedmobs.test.FakePlatform;
import io.github.hunter1712.infusedmobs.test.IsolatedState;
import io.github.hunter1712.infusedmobs.tier.MobTier;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks Bequest pool membership: a DEATH Ability drawn by Shade/Doom,
 * never by natural Cinder rolls or split-copy draws (both would fizzle —
 * Cinder carries a single Ability, copies cannot give or receive).
 */
@ExtendWith(IsolatedState.class)
class BequestDrawTest {

    private static void register(String id, TriggerType trigger) {
        TestAbilities.register(id, trigger);
    }

    @Test
    void bequestRegistersUnderDeath() {
        Platform.setProvider(new FakePlatform());
        AbilityRegistry.registerAll();

        Ability bequest = AbilityRegistry.getById(AbilityRegistry.BEQUEST_ID);
        assertTrue(bequest != null, "bequest must stay registered");
        assertEquals(TriggerType.DEATH, bequest.trigger(),
                "Bequest must fire on death, never passively or on hit");
    }

    @Test
    void poolMixesAllTriggersAtFifteen() {
        Platform.setProvider(new FakePlatform());
        AbilityRegistry.registerAll();

        assertEquals(15, AbilityRegistry.getAllAbilityIds().size(),
                "ability registration diverged through the seam");
        assertTrue(AbilityRegistry.getById("bequest").trigger() == TriggerType.DEATH);
    }

    @Test
    void cinderDrawNeverContainsBequest() {
        register("bane", TriggerType.HURT);
        register("bequest", TriggerType.DEATH);
        register("ward", TriggerType.TICK);
        register("rupture", TriggerType.DEATH);

        for (int i = 0; i < 20; i++) {
            List<Ability> result = MobTierManager.drawTierAbilities(MobTier.CINDER, 2);
            assertTrue(result.stream().noneMatch(a -> a.id().equals(AbilityRegistry.BEQUEST_ID)),
                    "draw #" + i + " gave Cinder an unmigratable support");
        }
    }

    @Test
    void shadeDrawCanContainBequest() {
        register("bane", TriggerType.HURT);
        register("bequest", TriggerType.DEATH);

        boolean sawBequest = false;
        for (int i = 0; i < 50; i++) {
            List<Ability> result = MobTierManager.drawTierAbilities(MobTier.SHADE, 2);
            if (result.stream().anyMatch(a -> a.id().equals(AbilityRegistry.BEQUEST_ID))) {
                sawBequest = true;
                break;
            }
        }
        assertTrue(sawBequest, "Shade should be able to draw bequest");
    }

    @Test
    void splitCopyDrawNeverContainsBequest() {
        register("bane", TriggerType.HURT);
        register("bequest", TriggerType.DEATH);
        register("rupture", TriggerType.DEATH);

        for (int i = 0; i < 20; i++) {
            List<Ability> result = AbilityRegistry.getSplitCopyAbilities();
            assertTrue(result.stream().noneMatch(a -> a.id().equals(AbilityRegistry.BEQUEST_ID)),
                    "copies cannot bequeath, so drawing bequest is dead weight");
        }
    }

    @Test
    void fireDeathKeepsRolledOrderWithBequest() {
        List<String> applied = new ArrayList<>();
        Ability rupture = new Ability("rupture", "rupture", TriggerType.DEATH,
                (mob, target, damage) -> applied.add("rupture"));
        Ability combust = new Ability("combust", "combust", TriggerType.DEATH,
                (mob, target, damage) -> applied.add("combust"));
        Ability bequest = new Ability("bequest", "bequest", TriggerType.DEATH,
                (mob, target, damage) -> applied.add("bequest"));

        AbilityRegistry.fireDeath(List.of(rupture, combust, bequest), null, null);

        assertEquals(List.of("rupture", "combust", "bequest"), applied);
    }
}
