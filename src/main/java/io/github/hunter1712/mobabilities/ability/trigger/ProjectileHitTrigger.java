package io.github.hunter1712.mobabilities.ability.trigger;

import io.github.hunter1712.mobabilities.ability.AbilityRegistry;
import io.github.hunter1712.mobabilities.ability.TriggerType;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

/**
 * Handles projectile hit triggers for mob abilities.
 *
 * <p>Uses the Fabric {@link ServerLivingEntityEvents#AFTER_DAMAGE} event
 * to detect when a player takes damage from a projectile fired by a
 * hostile mob, and dispatches the appropriate {@link TriggerType#PROJECTILE_HIT}
 * ability.
 *
 * <p>The Volley multi-arrow ability requires a dedicated mixin (see
 * {@code SkeletonMixin} in the mixin package).
 */
public final class ProjectileHitTrigger {

    private ProjectileHitTrigger() {
        // static-only utility class
    }

    /**
     * Applies the Grave Dust effect: {@link MobEffects#WITHER} for 200 ticks
     * (10 seconds) at amplifier 0.
     */
    public static void applyGraveDust(LivingEntity target) {
        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
    }

    /**
     * Applies the Shield Breaker effect: if the target is a {@link Player}
     * blocking with a shield, applies {@link MobEffects#WEAKNESS} for 100 ticks.
     */
    public static void applyShieldBreaker(LivingEntity target) {
        if (target instanceof Player player && player.isBlocking()) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
        }
    }

    /**
     * Registers the {@link ServerLivingEntityEvents#AFTER_DAMAGE} hook that
     * detects when a player takes damage from a projectile fired by a hostile mob.
     */
    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(
                (LivingEntity entity, DamageSource source, float amount,
                 float originalAmount, boolean blockedByShield) -> {
                    if (!(entity instanceof Player player)) return;
                    if (!(source.getDirectEntity() instanceof Projectile projectile)) return;

                    Entity owner = projectile.getOwner();
                    if (!(owner instanceof Mob shooter)) return;

                    AbilityRegistry.selectRandomAbility(shooter, TriggerType.PROJECTILE_HIT)
                            .ifPresent(ability -> ability.effectLogic().accept(shooter, player));
                }
        );
    }
}
