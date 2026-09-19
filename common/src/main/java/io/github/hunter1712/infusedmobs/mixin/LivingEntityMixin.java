package io.github.hunter1712.infusedmobs.mixin;

import io.github.hunter1712.infusedmobs.config.ModConfig;
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
            double multiplier;
            if (tier != null) {
                // Use the config multiplier (not the enum constant) so pack
                // makers' tier edits apply to XP too.
                multiplier = ModConfig.get().forTier(tier).xpMultiplier();
            } else if (MobTierManager.isSplitCopy(mob)) {
                // Split copies have no Tier but carry full Cinder stats.
                multiplier = ModConfig.get().forTier(MobTier.CINDER).xpMultiplier();
            } else {
                return;
            }
            cir.setReturnValue((int) Math.round(cir.getReturnValue() * multiplier));
        }
    }
}
