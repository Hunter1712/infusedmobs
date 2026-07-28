package io.github.hunter1712.infusedmobs.ability.trigger;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Handles the {@link TriggerType#TICK} trigger.
 * <p>
 * Every second, looks up each tracked mob by UUID across server levels
 * and applies its TICK abilities (Resistance, Strength, Speed, Regen).
 * Only mobs that actually have TICK abilities are processed.
 */
public final class MobTickTrigger {

    private static final int TICK_INTERVAL = 20; // 1 second

    private static int tickCounter = 0;

    private MobTickTrigger() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter = (tickCounter + 1) % TICK_INTERVAL;
            if (tickCounter != 0) return;

            Set<UUID> tracked = MobTierManager.getTrackedMobUUIDs();
            if (tracked.isEmpty()) return;

            for (UUID uuid : tracked) {
                Mob mob = MobTierManager.findMob(server, uuid);
                if (mob == null) continue;

                List<Ability> abilities = MobTierManager.getAbilitiesByTrigger(mob, TriggerType.TICK);
                for (Ability ability : abilities) {
                    ability.effectLogic().accept(mob, null);
                }
            }
        });
    }
}
