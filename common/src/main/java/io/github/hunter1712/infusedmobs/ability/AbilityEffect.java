package io.github.hunter1712.infusedmobs.ability;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Effect logic for a mob ability.
 * <p>
 * {@code attacker} is the infused mob. {@code target} is the entity the
 * effect applies to — a player for HURT, the killer for DEATH — and is
 * null for TICK effects, which only affect the attacker. {@code damage}
 * carries the incoming damage amount for HURT effects (used by Siphon)
 * and is 0 for other trigger types.
 */
@FunctionalInterface
public interface AbilityEffect {

    void apply(Mob attacker, LivingEntity target, float damage);
}
