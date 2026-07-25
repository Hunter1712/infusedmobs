package io.github.hunter1712.infusedmobs.mixin;

import io.github.hunter1712.infusedmobs.tier.MobTier;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixes into {@link LivingEntity} to multiply XP drops based on the
 * mob's assigned {@link MobTier}.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
    private void onGetExperienceReward(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Mob mob) {
            MobTier tier = MobTierManager.getTier(mob);
            if (tier != null) {
                cir.setReturnValue((int) Math.round(cir.getReturnValue() * tier.xpMultiplier()));
            }
        }
    }
}
