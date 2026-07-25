package io.github.hunter1712.mobabilities.mixin;

import io.github.hunter1712.mobabilities.ability.AbilityRegistry;
import io.github.hunter1712.mobabilities.ability.TriggerType;

import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.skeleton.AbstractSkeleton;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixes into {@link AbstractSkeleton} to fire extra arrows via the Volley
 * ability.  Targets the abstract base class so {@code performRangedAttack}
 * is directly resolvable.
 */
@Mixin(AbstractSkeleton.class)
public class SkeletonMixin {

    private boolean mobabilities$volleyActive = false;

    /**
     * Injects into {@link AbstractSkeleton#performRangedAttack} and fires 2
     * extra arrows if the skeleton has the Volley ability.
     */
    @Inject(method = "performRangedAttack", at = @At("TAIL"))
    private void onPerformRangedAttack(LivingEntity target, float velocity, CallbackInfo ci) {
        if (mobabilities$volleyActive) return;

        AbstractSkeleton skeleton = (AbstractSkeleton) (Object) this;

        boolean hasVolley = AbilityRegistry.getAbilitiesForMob(skeleton, TriggerType.PROJECTILE_HIT)
                .stream()
                .anyMatch(a -> a.name().equals("Volley"));

        if (hasVolley) {
            mobabilities$volleyActive = true;
            skeleton.performRangedAttack(target, velocity + 0.5f);
            skeleton.performRangedAttack(target, velocity + 0.5f);
            mobabilities$volleyActive = false;
        }
    }
}
