package io.github.hunter1712.mobabilities.mixin;

import io.github.hunter1712.mobabilities.ability.effect.ShadowstepEffect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixes into {@link LivingEntity} to handle Shadowstep dodge and other
 * defensive abilities.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    /**
     * Injects into {@link LivingEntity#hurtServer} to give mobs with the
     * Shadowstep ability a chance to dodge incoming attacks.
     */
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onHurtServer(ServerLevel level, DamageSource source, float amount,
                              CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Mob mob) {
            if (ShadowstepEffect.tryDodge(mob, level)) {
                cir.setReturnValue(false);
            }
        }
    }
}
