package io.github.hunter1712.mobabilities.ability.trigger;

import io.github.hunter1712.mobabilities.ability.Ability;
import io.github.hunter1712.mobabilities.ability.AbilityRegistry;
import io.github.hunter1712.mobabilities.ability.TriggerType;
import io.github.hunter1712.mobabilities.MobAbilitiesMod;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.core.registries.BuiltInRegistries;

import java.util.List;
import java.util.Optional;

/**
 * Handles the {@link TriggerType#DEATH DEATH} trigger for hostile mobs.
 *
 * <p>Listens for {@link ServerLivingEntityEvents#AFTER_DEATH}, filters
 * for {@link Monster} entities, queries the ability registry for
 * DEATH-triggered abilities, selects one at random (weighted), and
 * executes its effect logic with the killer as the second argument.
 *
 * <p>Death effect implementations (Split, Horde Caller, Corrosive Splash)
 * live in the {@code ability.effect} package and are invoked via the
 * registry's {@code effectLogic} BiConsumers.
 */
public final class MobDeathTrigger {

    private MobDeathTrigger() {
        // static-only utility class
    }

    /**
     * Registers the {@code AFTER_DEATH} event handler.
     */
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource source) -> {
            // Only process hostile mobs
            if (!(entity instanceof Monster mob)) {
                return;
            }

            // 1. Query available DEATH abilities (for debugging / logging)
            List<Ability> abilities =
                    AbilityRegistry.getAbilitiesForMob(mob, TriggerType.DEATH);
            if (abilities.isEmpty()) {
                return;
            }

            if (MobAbilitiesMod.LOGGER.isDebugEnabled()) {
                MobAbilitiesMod.LOGGER.debug(
                        "MobDeathTrigger: {} has {} death ability candidates",
                        BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()),
                        abilities.size());
            }

            // 2. Weighted-random selection of one DEATH ability
            Optional<Ability> selected =
                    AbilityRegistry.selectRandomAbility(mob, TriggerType.DEATH);
            if (selected.isEmpty()) {
                return;
            }

            // 3. Resolve the killer as the target (may be null)
            LivingEntity target = source.getEntity() instanceof LivingEntity living
                    ? living
                    : null;

            // 4. Execute the registered effect logic
            selected.get().effectLogic().accept(mob, target);
        });
    }
}
