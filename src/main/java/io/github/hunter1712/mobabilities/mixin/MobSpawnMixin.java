package io.github.hunter1712.mobabilities.mixin;

import io.github.hunter1712.mobabilities.ability.trigger.MobSpawnTrigger;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixes into {@link Mob#tick} to trigger spawn abilities on the first tick.
 */
@Mixin(Mob.class)
public class MobSpawnMixin {

    private boolean mobabilities$spawned = false;

    @Inject(method = "tick", at = @At("HEAD"))
    private void onTick(CallbackInfo ci) {
        if (!mobabilities$spawned) {
            mobabilities$spawned = true;
            if ((Object) this instanceof Entity entity && entity.level() instanceof ServerLevel serverLevel) {
                MobSpawnTrigger.onMobSpawn((Mob) entity, serverLevel);
            }
        }
    }
}
