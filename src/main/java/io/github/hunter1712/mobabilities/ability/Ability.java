package io.github.hunter1712.mobabilities.ability;

import net.minecraft.world.entity.LivingEntity;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * A data record representing a mob ability.
 * <p>
 * Each ability has a unique identifier, a display name, a predicate to
 * determine which mobs it applies to, a trigger type for when it activates,
 * and the effect logic that executes when triggered.
 *
 * @param id           unique identifier (e.g., "plague_bearer")
 * @param name         display name (e.g., "Plague Bearer")
 * @param mobPredicate filters which mobs this ability applies to
 * @param trigger      when this ability activates
 * @param effectLogic  the effect to execute — first LivingEntity is the mob,
 *                     second is the target (player)
 */
public record Ability(
        String id,
        String name,
        Predicate<LivingEntity> mobPredicate,
        TriggerType trigger,
        BiConsumer<LivingEntity, LivingEntity> effectLogic
) {
}
