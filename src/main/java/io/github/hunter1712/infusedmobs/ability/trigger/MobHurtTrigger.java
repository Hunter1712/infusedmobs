package io.github.hunter1712.infusedmobs.ability.trigger;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.tier.MobTier;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;
import io.github.hunter1712.infusedmobs.trigger.DamageContext;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Handles the {@link TriggerType#HURT} trigger for both melee and projectile
 * attacks from tiered mobs.
 * <p>
 * When a player takes damage — whether from a mob's melee hit or from a
 * projectile fired by a mob — the mob's HURT abilities are applied.
 */
public final class MobHurtTrigger {

    /**
     * One-time announcement tracking per mob UUID.
     * Cleaned on death via {@link #removeAnnounced}
     * and on despawn via {@link io.github.hunter1712.infusedmobs.mixin.EntityRemoveMixin}.
     */
    private static final Set<UUID> ANNOUNCED = new HashSet<>();

    /**
     * Prevents recursive HURT-ability firing when Thorns reflection
     * triggers a nested {@code AFTER_DAMAGE} event.
     */
    private static final ThreadLocal<Boolean> REFLECTING = ThreadLocal.withInitial(() -> false);

    private MobHurtTrigger() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(MobHurtTrigger::onAfterDamage);
        ServerLivingEntityEvents.AFTER_DAMAGE.register(MobHurtTrigger::onThornsReflect);
    }

    /** Clean up announcement tracking when a mob dies. */
    public static void removeAnnounced(UUID uuid) {
        ANNOUNCED.remove(uuid);
    }

    private static void onAfterDamage(
            LivingEntity entity, DamageSource source,
            float baseDamageTaken /* unused */, float damageTaken, boolean blocked
    ) {
        if (REFLECTING.get()) return;  // Prevent recursive firing from Thorns reflection
        if (blocked) return;            // Shield block negates abilities
        if (!(entity instanceof Player player)) return;

        Mob mob = findAttackingMob(source);
        if (mob == null) return;

        MobTier tier = MobTierManager.getTier(mob);
        if (tier == null) return;

        announceIfFirstEncounter(player, mob, tier);
        fireHurtAbilities(mob, player, damageTaken);
    }

    /**
     * Resolves the attacking mob from a damage source, accounting for both
     * direct melee hits and projectile attacks (arrows, tridents, fireballs).
     */
    private static Mob findAttackingMob(DamageSource source) {
        // Direct melee hit
        if (source.getEntity() instanceof Mob mob) return mob;
        // Projectile from a mob (arrow, trident, fire charge, etc.)
        if (source.getEntity() instanceof Projectile projectile
                && projectile.getOwner() instanceof Mob mob) return mob;
        return null;
    }

    /** Sends a one-time announcement the first time a player is hit by this mob. */
    private static void announceIfFirstEncounter(Player player, Mob mob, MobTier tier) {
        List<Ability> allAbilities = MobTierManager.getAllAbilities(mob);
        if (allAbilities.isEmpty() || !ANNOUNCED.add(mob.getUUID())) return;

        String entityName = mob.getType().getDescription().getString();
        String abilityNames = String.join("§7, §f",
                allAbilities.stream().map(Ability::name).toList());
        player.sendSystemMessage(Component.literal(
                "§e⚡ " + tier.name() + " " + entityName
                        + " has: §f" + abilityNames));
    }

    /** Stores damage for Siphon then fires all HURT abilities for the mob. */
    private static void fireHurtAbilities(Mob mob, Player player, float damageTaken) {
        DamageContext.set(damageTaken);
        for (Ability ability : MobTierManager.getAbilitiesByTrigger(mob, TriggerType.HURT)) {
            ability.effectLogic().accept(mob, player);
        }
    }

    /**
     * Reflects 15 % of melee damage back at the attacker when a player
     * hits a tiered mob that has the Thorns ability.
     */
    private static void onThornsReflect(
            LivingEntity entity, DamageSource source,
            float baseDamageTaken /* unused */, float damageTaken, boolean blocked
    ) {
        if (blocked) return;
        if (!(entity instanceof Mob mob)) return;
        if (!(source.getEntity() instanceof Player player)) return;
        if (!MobTierManager.hasAbility(mob, "thorns")) return;

        float reflected = damageTaken * 0.15f;
        if (reflected > 0.0f) {
            REFLECTING.set(true);
            try {
                ServerLevel level = (ServerLevel) player.level();
                player.hurtServer(level, player.damageSources().thorns(mob), reflected);
            } finally {
                REFLECTING.set(false);
            }
        }
    }
}
