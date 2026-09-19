package io.github.hunter1712.infusedmobs.ability.trigger;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.platform.Platform;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.List;

/**
 * Handles the {@link TriggerType#HURT} trigger for both melee and projectile
 * attacks involving Infused Mobs.
 * <p>
 * A single {@code AFTER_DAMAGE} handler covers both directions:
 * <ul>
 *   <li>player damaged by an Infused Mob → its offensive HURT Abilities fire,</li>
 *   <li>Infused Mob damaged by a player → its Thorns Ability reflects damage back.</li>
 * </ul>
 * Thorns is a HURT Ability whose effect does the reflection, so querying by
 * TriggerType tells the truth and passive iteration never visits it.
 * Reflection damage ({@link DamageTypes#THORNS}) never re-triggers either
 * path, which prevents infinite loops without any reentrancy state.
 * <p>
 * HURT fires against players only; mob-vs-mob hits never trigger Abilities.
 */
public final class MobHurtTrigger {

    private MobHurtTrigger() {}

    /**
     * Version-neutral HURT callback. Mirrors Fabric's post-damage shape so
     * every adapter can forward its own damage event to it.
     */
    public interface HurtHandler {
        void onHurt(LivingEntity entity, DamageSource source,
                    float baseDamageTaken, float damageTaken, boolean blocked);
    }

    public static void register() {
        Platform.hooks().registerHurtTrigger(MobHurtTrigger::onAfterDamage);
    }

    static void onAfterDamage(
            LivingEntity entity, DamageSource source,
            float baseDamageTaken /* unused */, float damageTaken, boolean blocked
    ) {
        if (blocked) return;            // Shield block negates abilities
        if (source.is(DamageTypes.THORNS)) return;  // Reflection damage — never re-trigger abilities

        if (entity instanceof Mob mob) {
            onMobDamagedByPlayer(mob, source, damageTaken);
        } else if (entity instanceof Player player) {
            onPlayerDamagedByMob(player, source, damageTaken);
        }
    }

    /** Player hit an Infused Mob — fire its Thorns Ability exactly once if present. */
    private static void onMobDamagedByPlayer(Mob mob, DamageSource source, float damageTaken) {
        if (!(source.getEntity() instanceof Player player)) return;
        if (!MobTierManager.hasAbility(mob, AbilityRegistry.THORNS_ID)) return;
        Ability thorns = AbilityRegistry.getById(AbilityRegistry.THORNS_ID);
        if (thorns == null) return;
        thorns.effect().apply(mob, player, damageTaken);
    }

    /** Player damaged by an Infused Mob (melee or projectile) — fire its offensive HURT Abilities. */
    private static void onPlayerDamagedByMob(Player player, DamageSource source, float damageTaken) {
        Mob mob = findAttackingMob(source);
        if (mob == null) return;
        // Gate on abilities rather than tier so Rupture split copies
        // (which have no tier) still fire their HURT abilities.
        List<Ability> hurtAbilities = MobTierManager.getAbilitiesByTrigger(mob, TriggerType.HURT).stream()
                .filter(ability -> !ability.id().equals(AbilityRegistry.THORNS_ID))
                .toList();
        if (hurtAbilities.isEmpty()) return;

        fireHurtAbilities(mob, player, damageTaken, hurtAbilities);
    }

    /**
     * Resolves the attacking mob from a damage source, accounting for both
     * direct melee hits and projectile attacks (arrows, tridents, fireballs).
     */
    static Mob findAttackingMob(DamageSource source) {
        Entity attacker = source.getEntity();
        Entity owner = attacker instanceof Projectile projectile ? projectile.getOwner() : null;
        if (!isMobAttack(attacker instanceof Mob, attacker instanceof Projectile, owner instanceof Mob)) {
            return null;
        }
        return attacker instanceof Mob mob ? mob : (Mob) owner;
    }

    /**
     * Pure attack-classification truth table behind {@link #findAttackingMob}:
     * a direct mob is an attack, otherwise only a mob-owned projectile is.
     * Lives on plain booleans so unit tests pin the melee / mob-projectile /
     * non-mob / null matrix without Minecraft bootstrap; the caller only
     * extracts the three flags from the damage source.
     */
    static boolean isMobAttack(boolean attackerIsMob, boolean attackerIsProjectile, boolean ownerIsMob) {
        return attackerIsMob || (attackerIsProjectile && ownerIsMob);
    }

    /** Fires the given HURT abilities for the mob, passing the damage amount through. */
    private static void fireHurtAbilities(Mob mob, Player player, float damageTaken, List<Ability> hurtAbilities) {
        for (Ability ability : hurtAbilities) {
            ability.effect().apply(mob, player, damageTaken);
        }
    }
}
