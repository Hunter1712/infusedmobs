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
import net.minecraft.world.entity.player.Player;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Handles the {@link TriggerType#HURT} trigger.
 * <p>
 * Fires ALL HURT abilities assigned to the attacking mob simultaneously
 * when a player takes damage from a hostile mob.
 */
public final class MobHurtTrigger {

    /**
     * One-time announcement tracking per mob UUID.
     * Cleaned on mob death via {@link #removeAnnounced}.
     * Despawned mobs (unloaded chunks) are NOT cleaned — an acceptably
     * small leak (~16 bytes per unique mob encountered).
     */
    private static final Set<UUID> ANNOUNCED = new HashSet<>();

    private MobHurtTrigger() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(MobHurtTrigger::onAfterDamage);
    }

    /** Clean up announcement tracking when a mob dies. */
    public static void removeAnnounced(UUID uuid) {
        ANNOUNCED.remove(uuid);
    }

    private static void onAfterDamage(
            LivingEntity entity, DamageSource source,
            float baseDamageTaken /* unused */, float damageTaken, boolean blocked
    ) {
        if (!(entity instanceof Player player)) return;
        if (!(source.getEntity() instanceof Mob mob)) return;
        if (blocked) return;  // Shield block negates abilities

        MobTier tier = MobTierManager.getTier(mob);
        if (tier == null) return;

        announceIfFirstEncounter(player, mob, tier);
        fireHurtAbilities(mob, player, damageTaken);
    }

    /** Sends a one-time announcement the first time a player is hit by this mob. */
    private static void announceIfFirstEncounter(Player player, Mob mob, MobTier tier) {
        List<Ability> allAbilities = MobTierManager.getAllAbilities(mob);
        if (allAbilities.isEmpty() || !ANNOUNCED.add(mob.getUUID())) return;

        String abilityNames = String.join("§7, §f",
                allAbilities.stream().map(Ability::name).toList());
        player.sendSystemMessage(Component.literal(
                "§e⚡ " + tier.name() + " " + mob.getName().getString()
                        + " has: §f" + abilityNames));
    }

    /** Stores damage for Siphon then fires all HURT abilities for the mob. */
    private static void fireHurtAbilities(Mob mob, Player player, float damageTaken) {
        DamageContext.set(damageTaken);
        for (Ability ability : MobTierManager.getAbilitiesByTrigger(mob, TriggerType.HURT)) {
            ability.effectLogic().accept(mob, player);
        }
    }
}
