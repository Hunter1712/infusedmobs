package io.github.hunter1712.mobabilities.trigger;

/**
 * Thread-safe context for passing damage amounts between triggers
 * and effect implementations.
 *
 * <p>The {@link io.github.hunter1712.mobabilities.ability.trigger.MobHurtTrigger}
 * stores the raw damage amount before dispatching to the ability registry,
 * and effects like Lifestrike read it via {@link #getAndClear()}.
 *
 * <p>This avoids changing the {@code BiConsumer<LivingEntity, LivingEntity>}
 * signature used by the {@link io.github.hunter1712.mobabilities.ability.Ability}
 * record, which would require updating all 23 ability definitions.
 */
public final class DamageContext {

    private static float lastDamageAmount = 0f;

    private DamageContext() {
        // static-only utility class
    }

    /**
     * Stores the damage amount for the next ability dispatch.
     *
     * @param amount the raw damage amount
     */
    public static void set(float amount) {
        lastDamageAmount = amount;
    }

    /**
     * Returns the last stored damage amount and resets it to zero.
     *
     * @return the stored damage amount
     */
    public static float getAndClear() {
        float amount = lastDamageAmount;
        lastDamageAmount = 0f;
        return amount;
    }
}
