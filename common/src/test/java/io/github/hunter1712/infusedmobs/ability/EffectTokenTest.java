package io.github.hunter1712.infusedmobs.ability;

import io.github.hunter1712.infusedmobs.platform.Platform;
import io.github.hunter1712.infusedmobs.test.FakePlatform;
import io.github.hunter1712.infusedmobs.test.IsolatedState;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contracts for genuinely typed Ability handles.
 * <p>
 * The token carries no untyped payload — version-specific implementations
 * stay behind the boundary — while registration call sites keep their
 * one-liner shape with no gameplay change.
 */
@ExtendWith(IsolatedState.class)
class EffectTokenTest {

    @Test
    void tokenExposesNoUntypedPayload() {
        assertTrue(EffectToken.class.isInterface(),
                "EffectToken must be a typed abstraction, not a wrapper class");
        assertTrue(Arrays.stream(EffectToken.class.getMethods())
                        .noneMatch(m -> m.getName().equals("handle") || m.getName().equals("of")),
                "EffectToken must expose no untyped handle payload");
    }

    @Test
    void registrationKeepsOneLinerShapeWithNoGameplayChange() {
        Platform.setProvider(new FakePlatform());

        AbilityRegistry.registerAll();

        // 8 HURT (7 offensive + reactive Thorns) + 4 TICK + 2 DEATH, no duplicates, ids stable.
        assertEquals(14, AbilityRegistry.getAllAbilityIds().size());
        assertEquals(8, AbilityRegistry.getAbilitiesByIds(AbilityRegistry.getAllAbilityIds()).stream()
                .filter(a -> a.trigger() == TriggerType.HURT).count());
        assertEquals(4, AbilityRegistry.getAbilitiesByIds(AbilityRegistry.getAllAbilityIds()).stream()
                .filter(a -> a.trigger() == TriggerType.TICK).count());
        assertEquals(2, AbilityRegistry.getAbilitiesByIds(AbilityRegistry.getAllAbilityIds()).stream()
                .filter(a -> a.trigger() == TriggerType.DEATH).count());
    }

    @Test
    void fakeTokensApplyWithoutCasting() {
        FakePlatform fake = new FakePlatform();

        EffectToken poison = fake.poison();
        poison.applyHurt(null, 60, 0);
        poison.applyTick(null, 60, 0);

        assertEquals(1, fake.hurtApplications,
                "HURT token must apply through the typed boundary without casting");
        assertEquals(1, fake.tickApplications,
                "TICK token must apply through the typed boundary without casting");
    }
}
