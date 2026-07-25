package io.github.hunter1712.infusedmobs.ability.trigger;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.UUID;

/**
 * Handles the {@link TriggerType#DEATH} trigger.
 * <p>
 * Fires all DEATH abilities (Split) when the mob dies,
 * then cleans up tier tracking to prevent memory leaks.
 */
public final class MobDeathTrigger {

    private MobDeathTrigger() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register(MobDeathTrigger::onDeath);
    }

    private static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Mob mob)) return;

        LivingEntity killer = source.getEntity() instanceof LivingEntity living ? living : null;

        // Fire ALL DEATH abilities
        List<Ability> abilities = MobTierManager.getAbilitiesByTrigger(mob, TriggerType.DEATH);
        for (Ability ability : abilities) {
            ability.effectLogic().accept(mob, killer);
        }

        // Clean up tracking
        UUID uuid = mob.getUUID();
        MobTierManager.removeMob(mob);
        MobHurtTrigger.removeAnnounced(uuid);
    }
}
