package io.github.hunter1712.mobabilities.ability.trigger;

import io.github.hunter1712.mobabilities.ability.AbilityRegistry;
import io.github.hunter1712.mobabilities.ability.TriggerType;
import io.github.hunter1712.mobabilities.effect.ModEffects;

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
 *
 * <p>Register by calling {@link #register()} from the mod initialiser.
 *
 * <h3>HURT abilities handled</h3>
 * <table>
 *   <caption>Registered HURT abilities and their associated mob types</caption>
 *   <tr><th>Ability</th><th>Mob(s)</th><th>Effect</th></tr>
 *   <tr><td>Plague Bearer</td><td>Zombie</td><td>Poison + Hunger</td></tr>
 *   <tr><td>Venomous Bite</td><td>Spider</td><td>Poison + Nausea</td></tr>
 *   <tr><td>Lifestrike</td><td>Any Monster</td><td>Heals mob for 50 % of damage dealt</td></tr>
 *   <tr><td>Cursed Wound</td><td>Any Monster</td><td>Custom CursedWound effect</td></tr>
 *   <tr><td>Rust</td><td>Any Monster</td><td>Damages player armour durability</td></tr>
 *   <tr><td>Disarm</td><td>Any Monster</td><td>Drops player's held item (25 % chance)</td></tr>
 *   <tr><td>Mind Shatter</td><td>Enderman</td><td>Nausea + Blindness</td></tr>
 *   <tr><td>Shield Breaker</td><td>Pillager (ranged)</td><td>Weakness (placeholder)</td></tr>
 * </table>
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
     *
     * <p>For each damage event where a {@link Player} is damaged by a
     * {@link Monster}, a random HURT ability is selected from the
     * {@link AbilityRegistry} and its registered effect logic is executed.
     *
     * <p>Call this once during mod initialisation:
     * <pre>{@code
     * MobHurtTrigger.register();
     * }</pre>
     */
    public static void register() {
        // AFTER_DAMAGE callback signature (Fabric API 0.155.2+26.2):
        // (LivingEntity entity, DamageSource source,
        //  float baseDamageTaken, float damageTaken, boolean blocked)
        ServerLivingEntityEvents.AFTER_DAMAGE.register(MobHurtTrigger::onAfterDamage);
    }

    /**
     * Handles the AFTER_DAMAGE event by checking whether a {@link Player}
     * was damaged by a {@link Monster} and, if so, dispatching a random
     * HURT ability.
     *
     * @param entity          the entity that took damage (damage taker)
     * @param source          the source of the damage
     * @param baseDamageTaken damage before armour / enchantment mitigation
     * @param damageTaken     actual damage taken before armour / enchantments
     * @param blocked         {@code true} if the damage was blocked by a shield
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

        // Select a weighted-random HURT ability for this mob type
        // and execute its registered effect logic.
        // mob    = the attacker (Monster that caused the damage)
        // player = the damage taker (Player that was hit)
        AbilityRegistry.selectRandomAbility(mob, TriggerType.HURT)
                .ifPresent(ability -> ability.effectLogic().accept(mob, player));
    }

    // ============================================================
    // Effect implementations (reference)
    //
    // These static methods document the concrete effect logic that
    // should be used when updating the corresponding BiConsumer
    // lambdas in AbilityRegistry.registerAll().  They are not called
    // directly by this trigger — dispatch goes through the registry's
    // effectLogic field.
    //
    // Each implementation follows the convention:
    //   (LivingEntity mob, LivingEntity player) -> { effect body }
    // ============================================================

    /**
     * Plague Bearer — applies {@link MobEffects#POISON} (10 s, amp 0)
     * and {@link MobEffects#HUNGER} (15 s, amp 1) to the player.
     *
     * <p>Intended for {@link net.minecraft.world.entity.EntityTypes#ZOMBIE}.
     *
     * @param mob    the attacking zombie
     * @param player the damaged player
     */
    static void applyPlagueBearer(final LivingEntity mob, final LivingEntity player) {
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 1));
    }

    /**
     * Venomous Bite — applies {@link MobEffects#POISON} (10 s, amp 0)
     * and {@link MobEffects#NAUSEA} (10 s, amp 0) to the player.
     *
     * <p>Intended for {@link net.minecraft.world.entity.EntityTypes#SPIDER}.
     *
     * @param mob    the attacking spider
     * @param player the damaged player
     */
    static void applyVenomousBite(final LivingEntity mob, final LivingEntity player) {
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
    }

    /**
     * Lifestrike — heals the attacking mob for 50 % of the damage dealt.
     *
     * <p><b>Note:</b> The current {@code BiConsumer<LivingEntity, LivingEntity>}
     * effect-logic signature does not convey the damage amount.  A proper
     * implementation requires either a custom tri-consumer or capturing the
     * amount from the event.  The line below shows the intended behaviour:
     * <pre>{@code
     * mob.heal(damageAmount * 0.5f);
     * }</pre>
     *
     * <p>Intended for any {@link Monster}.
     *
     * @param mob    the attacking mob
     * @param player the damaged player (unused by this effect)
     */
    static void applyLifestrike(final LivingEntity mob, final LivingEntity player) {
        // Actual implementation requires the damage amount from the event.
        // mob.heal(damageAmount * 0.5f);
        // The registry placeholder currently applies Instant Health as a
        // temporary substitute.
    }

    /**
     * Cursed Wound — applies {@link ModEffects#CURSED_WOUND} (10 s, amp 0)
     * to the player, preventing natural health regeneration for the duration.
     *
     * <p>Intended for any {@link Monster}.
     *
     * @param mob    the attacking mob
     * @param player the damaged player
     */
    static void applyCursedWound(final LivingEntity mob, final LivingEntity player) {
        player.addEffect(new MobEffectInstance(ModEffects.CURSED_WOUND, 200, 0));
    }

    /**
     * Rust — damages every equipped armour piece on the player by 2
     * durability points per hit.
     *
     * <p>Iterates {@link Player#getInventory()}.{@code armor} and calls
     * {@link ItemStack#hurtAndBreak(int, LivingEntity, java.util.function.Consumer)}
     * on each piece.
     *
     * <p>Intended for any {@link Monster}.
     *
     * @param mob    the attacking mob
     * @param player the damaged player whose armour will be damaged
     */
    static void applyRust(final LivingEntity mob, final LivingEntity player) {
        if (!(player instanceof final Player p)) {
            return;
        }
        var serverLevel = (ServerLevel) p.level();
        var serverPlayer = (ServerPlayer) p;
        p.getItemBySlot(EquipmentSlot.HEAD).hurtAndBreak(2, serverLevel, serverPlayer, item -> {});
        p.getItemBySlot(EquipmentSlot.CHEST).hurtAndBreak(2, serverLevel, serverPlayer, item -> {});
        p.getItemBySlot(EquipmentSlot.LEGS).hurtAndBreak(2, serverLevel, serverPlayer, item -> {});
        p.getItemBySlot(EquipmentSlot.FEET).hurtAndBreak(2, serverLevel, serverPlayer, item -> {});
    }

    /**
     * Disarm — drops the player's main-hand item on the ground, but only
     * has a 25 % chance to activate per hit to avoid excessive annoyance.
     *
     * <p>Uses the {@code player.drop(…, true, false)} method to spawn the
     * item entity, then sets the main-hand slot to {@link ItemStack#EMPTY}.
     *
     * <p>Intended for any {@link Monster}.
     *
     * @param mob    the attacking mob
     * @param player the damaged player to disarm
     */
    static void applyDisarm(final LivingEntity mob, final LivingEntity player) {
        if (!(player instanceof final Player p)) {
            return;
        }

        // 25 % chance to trigger
        if (p.getRandom().nextFloat() >= 0.25f) {
            return;
        }

        final ItemStack held = p.getMainHandItem();
        if (held.isEmpty()) {
            return;
        }

        // Drop the held item as an entity at the player's position
        p.drop(held, true, false);
        // Clear the main-hand slot
        p.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
    }

    /**
     * Mind Shatter — applies {@link MobEffects#NAUSEA} (10 s, amp 0) and
     * {@link MobEffects#BLINDNESS} (5 s, amp 0) to the player, creating a
     * disorienting effect.
     *
     * <p>Intended for {@link net.minecraft.world.entity.EntityTypes#ENDERMAN}.
     *
     * @param mob    the attacking enderman
     * @param player the damaged player
     */
    static void applyMindShatter(final LivingEntity mob, final LivingEntity player) {
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
    }

    /**
     * Shield Breaker — applies {@link MobEffects#WEAKNESS} (5 s, amp 1)
     * as a placeholder effect.
     *
     * <p><b>TODO:</b> The full implementation requires a mixin to disable
     * the player's shield via cooldown mechanics.  For now, Weakness serves
     * as a stopgap that reduces the player's melee effectiveness.
     *
     * <p>Intended for {@link net.minecraft.world.entity.EntityTypes#PILLAGER}
     * (via PROJECTILE_HIT trigger, but registered here for HURT as well).
     *
     * @param mob    the attacking mob
     * @param player the damaged player
     */
    static void applyShieldBreaker(final LivingEntity mob, final LivingEntity player) {
        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
    }
}
