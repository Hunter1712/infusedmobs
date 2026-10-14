package io.github.hunter1712.infusedmobs.mixin;

import io.github.hunter1712.infusedmobs.config.ModConfig;
import io.github.hunter1712.infusedmobs.tier.InfusionStats;
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
 * Rupture split copies have no Tier but carry full Cinder stats, so they
 * grant the documented Cinder experience treatment through the same path.
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    @Inject(method = "getExperienceReward", at = @At("RETURN"), cancellable = true)
    private void onGetExperienceReward(CallbackInfoReturnable<Integer> cir) {
        if ((Object) this instanceof Mob mob) {
            MobTier tier = MobTierManager.getTier(mob);
            boolean splitCopy = tier == null && MobTierManager.isSplitCopy(mob);
            if (tier == null && !splitCopy) return;
            // Split-copy Cinder treatment lives in InfusionStats with the
            // assignment path, so pack-maker tier edits apply to XP too.
            double multiplier = InfusionStats.xpMultiplier(tier, splitCopy, ModConfig.get());
            cir.setReturnValue((int) Math.round(cir.getReturnValue() * multiplier));
        }
    }
}
