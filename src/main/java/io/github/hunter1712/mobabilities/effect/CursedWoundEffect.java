package io.github.hunter1712.mobabilities.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Prevents natural healing while active.
 * <p>
 * The mere presence of this effect blocks natural health regeneration
 * (Minecraft does not regen health while any status effect is active).
 */
public class CursedWoundEffect extends MobEffect {

    protected CursedWoundEffect() {
        super(MobEffectCategory.HARMFUL, 0x8B0000);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        return true;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        // The effect's presence alone prevents natural healing.
        // No additional tick logic needed.
        return super.applyEffectTick(level, entity, amplifier);
    }
}
