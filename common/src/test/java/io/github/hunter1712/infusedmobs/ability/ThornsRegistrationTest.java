package io.github.hunter1712.infusedmobs.ability;

import io.github.hunter1712.infusedmobs.platform.Platform;
import io.github.hunter1712.infusedmobs.test.FakePlatform;
import io.github.hunter1712.infusedmobs.test.IsolatedState;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Locks Thorns as an honest reactive HURT Ability: registered under the
 * trigger where it actually fires, never passive.
 */
@ExtendWith(IsolatedState.class)
class ThornsRegistrationTest {

    @Test
    void thornsRegistersUnderHurtNotTick() {
        Platform.setProvider(new FakePlatform());
        AbilityRegistry.registerAll();

        Ability thorns = AbilityRegistry.getById("thorns");
        assertTrue(thorns != null, "thorns must stay registered");
        assertEquals(TriggerType.HURT, thorns.trigger(),
                "Thorns must fire reactively as HURT, never as passive TICK");
    }

    @Test
    void thornsOnlyMobHasHurtButNoTick() {
        // Tick exclusion is locked in InfusedMobTest.thornsOnlyMobIsNotTickCapable;
        // here we lock the registry side: thorns must not be a TICK ability.
        Platform.setProvider(new FakePlatform());
        AbilityRegistry.registerAll();

        Ability thorns = AbilityRegistry.getById("thorns");
        assertTrue(thorns.trigger() != TriggerType.TICK,
                "Thorns must never fire from passive iteration");
    }

    @Test
    void poolStillMixesAllTriggersAtFourteen() {
        Platform.setProvider(new FakePlatform());
        AbilityRegistry.registerAll();

        assertEquals(14, AbilityRegistry.getAllAbilityIds().size(),
                "ability registration diverged through the seam");
        assertTrue(AbilityRegistry.getById("bane").trigger() == TriggerType.HURT);
        assertTrue(AbilityRegistry.getById("ward").trigger() == TriggerType.TICK);
        assertTrue(AbilityRegistry.getById("rupture").trigger() == TriggerType.DEATH);
    }
}
