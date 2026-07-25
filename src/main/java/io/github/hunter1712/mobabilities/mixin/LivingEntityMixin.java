package io.github.hunter1712.mobabilities.mixin;

import io.github.hunter1712.mobabilities.ability.effect.ShadowstepEffect;
import io.github.hunter1712.mobabilities.ability.trigger.MobSpawnTrigger;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Mixes into {@link LivingEntity} to handle defensive abilities:
 * <ul>
 *   <li>Shadowstep dodge — chance to teleport away and cancel damage</li>
 *   <li>Thorns reflection — reflect damage back to the attacker</li>
 * </ul>
 */
@Mixin(LivingEntity.class)
public class LivingEntityMixin {

    /**
     * Injects into {@link LivingEntity#hurtServer} to:
     * <ol>
     *   <li>Give mobs with the Shadowstep ability a chance to dodge.</li>
     *   <li>Reflect damage when a mob with Thorns is hit.</li>
     * </ol>
     */
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void onHurtServer(ServerLevel level, DamageSource source, float amount,
                              CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Mob mob) {
            // 1. Shadowstep dodge
            if (ShadowstepEffect.tryDodge(mob, level)) {
                cir.setReturnValue(false);
                return;
            }

            // 2. Thorns reflection — if this mob has Thorns, reflect damage to attacker
            if (MobSpawnTrigger.hasThorns().test(mob)) {
                if (source.getEntity() instanceof LivingEntity attacker) {
                    float reflected = 1.0f + mob.getRandom().nextInt(4); // 1-4 damage
                    attacker.hurtServer(level, level.damageSources().thorns(mob), reflected);
                }
            }
        }
    }
}
