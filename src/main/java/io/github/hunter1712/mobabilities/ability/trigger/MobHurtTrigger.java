package io.github.hunter1712.mobabilities.ability.trigger;

import io.github.hunter1712.mobabilities.ability.Ability;
import io.github.hunter1712.mobabilities.ability.TriggerType;
import io.github.hunter1712.mobabilities.tier.MobTierManager;
import io.github.hunter1712.mobabilities.trigger.DamageContext;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.List;

/**
 * Handles the {@link TriggerType#HURT} trigger.
 * <p>
 * Fires ALL HURT abilities assigned to the attacking mob simultaneously
 * when a player takes damage from a hostile mob.
 */
public final class MobHurtTrigger {

    private MobHurtTrigger() {}

    public static void register() {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(MobHurtTrigger::onAfterDamage);
    }

    private static void onAfterDamage(
            LivingEntity entity, DamageSource source,
            float baseDamageTaken, float damageTaken, boolean blocked
    ) {
        if (!(entity instanceof Player)) return;
        if (!(source.getEntity() instanceof Mob mob)) return;

        // Store damage for Lifesteal
        DamageContext.set(damageTaken);

        // Fire ALL HURT abilities on this mob
        List<Ability> abilities = MobTierManager.getAbilitiesByTrigger(mob, TriggerType.HURT);
        for (Ability ability : abilities) {
            ability.effectLogic().accept(mob, entity);
        }
    }
}
