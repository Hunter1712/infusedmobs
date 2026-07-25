package io.github.hunter1712.mobabilities.ability.effect;

import net.minecraft.world.entity.LivingEntity;

/**
 * Heals the defending mob for 50 % of the damage dealt to the attacker.
 *
 * <p>Called from {@code MobHurtTrigger} when a mob has the Lifestrike ability.
 */
public final class LifestrikeEffect {

    private LifestrikeEffect() {
        // static-only utility class
    }

    /**
     * Applies the lifesteal heal to the given mob based on the damage amount.
     *
     * @param mob    the mob that dealt the damage (receives the heal)
     * @param amount the raw damage dealt before reduction
     */
    public static void apply(LivingEntity mob, float amount) {
        float heal = amount * 0.5f;
        if (heal > 0 && mob.isAlive()) {
            mob.heal(heal);
        }
    }
}
