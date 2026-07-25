package io.github.hunter1712.infusedmobs.ability;

import net.minecraft.world.entity.LivingEntity;
import java.util.function.BiConsumer;

/**
 * A data record representing a mob ability.
 * <p>
 * Each ability has a unique identifier, a display name, a trigger type
 * for when it activates, and the effect logic that executes when triggered.
 *
 * @param id           unique identifier (e.g., "venom")
 * @param name         display name (e.g., "Venom")
 * @param trigger      when this ability activates
 * @param effectLogic  the effect to execute — first LivingEntity is the mob,
 *                     second is the target (player)
 */
public record Ability(
        String id,
        String name,
        TriggerType trigger,
        BiConsumer<LivingEntity, LivingEntity> effectLogic
) {
}
