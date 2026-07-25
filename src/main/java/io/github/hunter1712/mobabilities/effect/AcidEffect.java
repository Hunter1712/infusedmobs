package io.github.hunter1712.mobabilities.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.entity.LivingEntity;

/**
 * Deals damage over time (1 HP per second) while active.
 */
public class AcidEffect extends MobEffect {

    private static final int DAMAGE_INTERVAL = 20; // every second

    protected AcidEffect() {
        super(MobEffectCategory.HARMFUL, 0x00FF00);
    }

    @Override
    public boolean shouldApplyEffectTickThisTick(int duration, int amplifier) {
        // Apply damage once per second
        return duration % DAMAGE_INTERVAL == 0;
    }

    @Override
    public boolean applyEffectTick(ServerLevel level, LivingEntity entity, int amplifier) {
        if (!entity.isDeadOrDying()) {
            entity.hurtServer(level, level.damageSources().magic(), 1.0f);
        }
        return super.applyEffectTick(level, entity, amplifier);
    }
}
