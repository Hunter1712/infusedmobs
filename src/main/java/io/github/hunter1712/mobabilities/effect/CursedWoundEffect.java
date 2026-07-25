package io.github.hunter1712.mobabilities.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Blocks all forms of healing while active.
 * <p>
 * A {@code CursedWoundMixin} into {@link LivingEntity#heal} prevents
 * the entity from regenerating health by any means — natural regen,
 * potions, golden apples, or other effects — for the effect's duration.
 *
 * <p>This makes Cursed Wound a severe debuff that requires milk or
 * time to clear.
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
