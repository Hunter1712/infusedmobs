package io.github.hunter1712.mobabilities.mixin;

import io.github.hunter1712.mobabilities.effect.ModEffects;

import net.minecraft.world.entity.LivingEntity;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixes into {@link LivingEntity#heal} to block all healing while the
 * {@link io.github.hunter1712.mobabilities.effect.CursedWoundEffect
 * CursedWound} effect is active.
 *
 * <p>Unlike natural healing blocks which only prevent passive regen, this
 * prevents <em>all</em> forms of healing — potions, golden apples,
 * regeneration effects, etc. — making Cursed Wound a severe debuff that
 * requires milk or time to clear.
 */
@Mixin(LivingEntity.class)
public class CursedWoundMixin {

    /**
     * Injected at the head of {@link LivingEntity#heal} to cancel all
     * healing while the entity has the CursedWound status effect.
     */
    @Inject(method = "heal", at = @At("HEAD"), cancellable = true)
    private void onHeal(float healAmount, CallbackInfo ci) {
        LivingEntity self = (LivingEntity) (Object) this;
        if (healAmount > 0 && self.hasEffect(ModEffects.CURSED_WOUND)) {
            ci.cancel();
        }
    }
}
