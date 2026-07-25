package io.github.hunter1712.mobabilities.mixin;

import io.github.hunter1712.mobabilities.tier.MobTierManager;

import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixes into {@link Mob#tick} to assign a tier on the first tick.
 * Entry point for tier/ability assignment on spawn.
 */
@Mixin(Mob.class)
public class MobSpawnMixin {

    private boolean mobabilities$spawned = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (mobabilities$spawned) return;
        mobabilities$spawned = true;
        Mob self = (Mob) (Object) this;
        if (self.level() instanceof net.minecraft.server.level.ServerLevel) {
            MobTierManager.assignTier(self);
        }
    }
}
