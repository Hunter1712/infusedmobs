package io.github.hunter1712.infusedmobs.mixin;

import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Mixes into {@link Entity#remove} to clean up tier tracking when a mob
 * despawns ({@link Entity.RemovalReason#DISCARDED}).
 * <p>
 * Deaths are already handled by {@link io.github.hunter1712.infusedmobs.ability.trigger.MobDeathTrigger}.
 */
@Mixin(Entity.class)
public class EntityRemoveMixin {

    @Inject(method = "remove", at = @At("HEAD"))
    private void onRemove(Entity.RemovalReason reason, CallbackInfo ci) {
        if (reason != Entity.RemovalReason.DISCARDED) return;
        if (!(((Object) this) instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel)) return;

        MobTierManager.removeMob(mob);
    }
}
