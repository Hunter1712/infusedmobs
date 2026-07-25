package io.github.hunter1712.mobabilities.ability.trigger;

import io.github.hunter1712.mobabilities.ability.AbilityRegistry;
import io.github.hunter1712.mobabilities.ability.TriggerType;
import io.github.hunter1712.mobabilities.effect.ModEffects;
import io.github.hunter1712.mobabilities.trigger.DamageContext;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

/**
 * Handles the {@link TriggerType#HURT HURT} trigger for hostile mobs.
 *
 * <p>Listens for {@link ServerLivingEntityEvents#AFTER_DAMAGE}, filters
 * for cases where a {@link Player} takes damage from a {@link Monster},
 * queries the ability registry for a random HURT-triggered ability, and
 * executes its effect logic with the attacking mob as the source and the
 * damaged player as the target.
 */
public final class MobHurtTrigger {

    private MobHurtTrigger() {
        // static-only utility class
    }

    // ========================================
    // Registration
    // ========================================

    /**
     * Registers the {@code AFTER_DAMAGE} event handler.
     */
    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(MobHurtTrigger::onAfterDamage);
    }

    // ========================================
    // Event handler
    // ========================================

    /**
     * Handles the AFTER_DAMAGE event by checking whether a {@link Player}
     * was damaged by a {@link Monster} and, if so, dispatching a random
     * HURT ability.
     *
     * <p>Stores the damage amount in {@link DamageContext} before dispatch
     * so that effects like Lifestrike can access the actual damage dealt.
     */
    private static void onAfterDamage(
            final LivingEntity entity,
            final DamageSource source,
            final float baseDamageTaken,
            final float damageTaken,
            final boolean blocked
    ) {
        // Only apply HURT effects when the damage taker is a Player
        if (!(entity instanceof final Player player)) {
            return;
        }

        // Only process damage from a hostile Monster
        if (!(source.getEntity() instanceof final Monster mob)) {
            return;
        }

        // Store the damage amount for Lifestrike and other amount-dependent effects
        DamageContext.set(damageTaken);

        // Select a weighted-random HURT ability for this mob type
        // and execute its registered effect logic.
        AbilityRegistry.selectRandomAbility(mob, TriggerType.HURT)
                .ifPresent(ability -> ability.effectLogic().accept(mob, player));
    }
}
