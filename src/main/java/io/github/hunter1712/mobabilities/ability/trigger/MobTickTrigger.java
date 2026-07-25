package io.github.hunter1712.mobabilities.ability.trigger;

import io.github.hunter1712.mobabilities.ability.Ability;
import io.github.hunter1712.mobabilities.ability.TriggerType;
import io.github.hunter1712.mobabilities.tier.MobTierManager;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/**
 * Handles the {@link TriggerType#TICK} trigger.
 * <p>
 * Every second, iterates all loaded mobs and applies their TICK
 * abilities (Resistance, Strength, Speed, Regen).
 */
public final class MobTickTrigger {

    private static int tickCounter = 0;

    private MobTickTrigger() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter = (tickCounter + 1) % 20;
            if (tickCounter != 0) return;

            for (ServerLevel level : server.getAllLevels()) {
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof Mob mob && mob.isAlive()) {
                        List<Ability> abilities = MobTierManager.getAbilitiesByTrigger(mob, TriggerType.TICK);
                        for (Ability ability : abilities) {
                            ability.effectLogic().accept(mob, null);
                        }
                    }
                }
            }
        });
    }
}
