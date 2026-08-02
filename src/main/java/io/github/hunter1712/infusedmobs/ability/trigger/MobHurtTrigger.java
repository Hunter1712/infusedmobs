package io.github.hunter1712.infusedmobs.ability.trigger;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
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
 * attacks from tiered mobs, plus the Thorns reflection.
 * <p>
 * A single {@code AFTER_DAMAGE} handler covers both directions:
 * <ul>
 *   <li>player damaged by an infused mob → its HURT abilities fire,</li>
 *   <li>infused mob damaged by a player → Thorns reflects damage back.</li>
 * </ul>
 * Reflection damage ({@link DamageTypes#THORNS}) never re-triggers either
 * path, which prevents infinite loops without any reentrancy state.
 */
public final class MobHurtTrigger {

    /** Fraction of melee damage reflected by the Thorns ability. */
    private static final float THORNS_REFLECT_FRACTION = 0.15f;

    private MobHurtTrigger() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(MobHurtTrigger::onAfterDamage);
    }

    private static void onAfterDamage(
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

    /** Player hit a tiered mob that has Thorns — reflect a fraction back. */
    private static void onMobDamagedByPlayer(Mob mob, DamageSource source, float damageTaken) {
        if (!(source.getEntity() instanceof Player player)) return;
        if (!MobTierManager.hasAbility(mob, "thorns")) return;

        float reflected = damageTaken * THORNS_REFLECT_FRACTION;
        if (reflected > 0.0f && mob.level() instanceof ServerLevel level) {
            player.hurtServer(level, player.damageSources().thorns(mob), reflected);
        }
    }

    /** Player damaged by an infused mob (melee or projectile) — fire its HURT abilities. */
    private static void onPlayerDamagedByMob(Player player, DamageSource source, float damageTaken) {
        Mob mob = findAttackingMob(source);
        if (mob == null) return;
        // Gate on abilities rather than tier so Rupture split copies
        // (which have no tier) still fire their HURT abilities.
        if (MobTierManager.getAbilitiesByTrigger(mob, TriggerType.HURT).isEmpty()) return;

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
        for (Ability ability : MobTierManager.getAbilitiesByTrigger(mob, TriggerType.HURT)) {
            ability.effect().apply(mob, player, damageTaken);
        }
    }
}
