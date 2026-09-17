package io.github.hunter1712.infusedmobs.ability.trigger;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.platform.Platform;
import io.github.hunter1712.infusedmobs.tier.InfusedTracker;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.List;

/**
 * Handles the {@link TriggerType#HURT} trigger for both melee and projectile
 * attacks from Infused Mobs, plus the Thorns reflection.
 * <p>
 * A single {@code AFTER_DAMAGE} handler covers both directions:
 * <ul>
 *   <li>player damaged by an Infused Mob → its HURT Abilities fire,</li>
 *   <li>Infused Mob damaged by a player → Thorns reflects damage back.</li>
 * </ul>
 * Reflection damage ({@link DamageTypes#THORNS}) never re-triggers either
 * path, which prevents infinite loops without any reentrancy state.
 * <p>
 * HURT fires against players only; mob-vs-mob hits never trigger Abilities.
 */
public final class MobHurtTrigger {

    /** Fraction of melee damage reflected by the Thorns ability. */
    private static final float THORNS_REFLECT_FRACTION = 0.15f;

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

    /** Player hit an Infused Mob that has Thorns — reflect a fraction back. */
    private static void onMobDamagedByPlayer(Mob mob, DamageSource source, float damageTaken) {
        if (!(source.getEntity() instanceof Player player)) return;
        if (!InfusedTracker.hasAbility(mob, "thorns")) return;

        float reflected = damageTaken * THORNS_REFLECT_FRACTION;
        if (reflected > 0.0f && mob.level() instanceof ServerLevel level) {
            Platform.hooks().reflectThorns(player, mob, reflected, level);
        }
    }

    /** Player damaged by an Infused Mob (melee or projectile) — fire its HURT Abilities. */
    private static void onPlayerDamagedByMob(Player player, DamageSource source, float damageTaken) {
        Mob mob = findAttackingMob(source);
        if (mob == null) return;
        // Gate on abilities rather than tier so Rupture split copies
        // (which have no tier) still fire their HURT abilities.
        if (InfusedTracker.getAbilitiesByTrigger(mob, TriggerType.HURT).isEmpty()) return;

        fireHurtAbilities(mob, player, damageTaken);
    }

    /**
     * Resolves the attacking mob from a damage source, accounting for both
     * direct melee hits and projectile attacks (arrows, tridents, fireballs).
     */
    private static Mob findAttackingMob(DamageSource source) {
        Entity attacker = source.getEntity();
        // Direct melee hit
        if (attacker instanceof Mob mob) return mob;
        // Projectile from a mob (arrow, trident, fire charge, etc.)
        if (attacker instanceof Projectile projectile
                && projectile.getOwner() instanceof Mob mob) return mob;
        return null;
    }

    /** Fires all HURT abilities for the mob, passing the damage amount through. */
    private static void fireHurtAbilities(Mob mob, Player player, float damageTaken) {
        for (Ability ability : InfusedTracker.getAbilitiesByTrigger(mob, TriggerType.HURT)) {
            ability.effect().apply(mob, player, damageTaken);
        }
    }
}
