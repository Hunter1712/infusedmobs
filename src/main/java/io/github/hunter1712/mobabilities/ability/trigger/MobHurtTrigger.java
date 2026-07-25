package io.github.hunter1712.mobabilities.ability.trigger;

import io.github.hunter1712.mobabilities.ability.Ability;
import io.github.hunter1712.mobabilities.ability.TriggerType;
import io.github.hunter1712.mobabilities.tier.MobTier;
import io.github.hunter1712.mobabilities.tier.MobTierManager;
import io.github.hunter1712.mobabilities.trigger.DamageContext;

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

    /** Tracks which mobs already announced their abilities (one-time per mob). */
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
            float baseDamageTaken, float damageTaken, boolean blocked
    ) {
        if (!(entity instanceof Player player)) return;
        if (!(source.getEntity() instanceof Mob mob)) return;

        MobTier tier = MobTierManager.getTier(mob);
        if (tier == null) return;

        // One-time announcement of tier + all abilities
        List<Ability> allAbilities = MobTierManager.getAllAbilities(mob);
        if (!allAbilities.isEmpty() && ANNOUNCED.add(mob.getUUID())) {
            player.sendSystemMessage(
                    Component.literal("§e⚡ " + tier.name() + " "
                            + mob.getName().getString() + " has: §f"
                            + String.join("§7, §f",
                                    allAbilities.stream().map(Ability::name).toList()))
            );
        }

        // Store damage for Siphon, then fire ALL HURT abilities
        DamageContext.set(damageTaken);
        for (Ability ability : MobTierManager.getAbilitiesByTrigger(mob, TriggerType.HURT)) {
            ability.effectLogic().accept(mob, player);
        }
    }
}
