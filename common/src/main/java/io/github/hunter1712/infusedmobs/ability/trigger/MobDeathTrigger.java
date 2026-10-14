package io.github.hunter1712.infusedmobs.ability.trigger;

import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.platform.Platform;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Handles the {@link TriggerType#DEATH} trigger.
 * <p>
 * Fires all DEATH abilities (Rupture) when the mob dies,
 * then cleans up tier tracking to prevent memory leaks.
 */
public final class MobDeathTrigger {

    private MobDeathTrigger() {}

    public static void register() {
        Platform.hooks().registerDeathTrigger(MobDeathTrigger::onDeath);
    }

    static void onDeath(LivingEntity entity, DamageSource source) {
        if (!(entity instanceof Mob mob)) return;

        LivingEntity killer = resolveKiller(source.getEntity());

        // Fire ALL DEATH abilities
        AbilityRegistry.fireDeath(
                MobTierManager.getAbilitiesByTrigger(mob, TriggerType.DEATH), mob, killer);

        // Clean up tracking
        MobTierManager.removeMob(mob);
    }

    /**
     * Pure killer resolution behind {@link #onDeath}: the damage-source
     * owner passes through only when living, else null. Plain reference
     * logic so tests pin the mapping without a damage source.
     */
    static LivingEntity resolveKiller(Entity attacker) {
        return attacker instanceof LivingEntity living ? living : null;
    }
}
