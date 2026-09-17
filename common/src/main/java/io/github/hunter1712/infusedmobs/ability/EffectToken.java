package io.github.hunter1712.infusedmobs.ability;

import net.minecraft.world.entity.LivingEntity;

/**
 * Typed handle for a version-specific status effect.
 * <p>
 * Each Versioned Source Set returns its own implementation from the existing
 * effect accessors ({@code slowness()}, {@code poison()}, ...); shared code
 * passes tokens through without inspecting version types. Version Shims never
 * cast — the implementation captures its correctly typed effect
 * ({@code Holder<MobEffect>} on modern versions, raw {@code MobEffect} on
 * legacy) and applies it directly.
 */
public interface EffectToken {

    /** Applies this effect as a HURT effect to the target. */
    void applyHurt(LivingEntity target, int duration, int amplifier);

    /** Applies this effect as a TICK effect to the mob itself. */
    void applyTick(LivingEntity mob, int duration, int amplifier);
}
