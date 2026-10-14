package io.github.hunter1712.infusedmobs.ability.trigger;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

/**
 * Handles the {@link TriggerType#TICK} trigger.
 * <p>
 * Every second, looks up each tick-capable mob by UUID across server levels
 * and applies its TICK abilities (Resistance, Strength, Speed, Regen).
 * Production callers cross one seam — {@link #tickServer(MinecraftServer)} —
 * instead of composing the UUID scan, mob lookup, and fire steps themselves;
 * the dispatch core ({@link #tickOnce}) is exercised with fakes.
 */
public final class MobTickTrigger {

    private static final int TICK_INTERVAL = 20; // 1 second

    private static int tickCounter = 0;

    private MobTickTrigger() {}

    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter = (tickCounter + 1) % TICK_INTERVAL;
            if (tickCounter != 0) return;

            tickServer(server);
        });
    }

    /**
     * Runs one per-second TICK pass over every tick-capable Infused Mob.
     * Behind this seam so callers never compose the scan, lookup, and fire
     * steps themselves.
     */
    public static void tickServer(MinecraftServer server) {
        tickOnce(
                MobTierManager.getTickMobUUIDs(),
                uuid -> MobTierManager.findMob(server, uuid),
                mob -> MobTierManager.getAbilitiesByTrigger(mob, TriggerType.TICK),
                AbilityRegistry::fireTick);
    }

    /**
     * Dispatch core behind {@link #tickServer}: resolves each id to a mob,
     * skips missing mobs and mobs with no TICK abilities, and fires the rest.
     * Generic over the mob holder so unit tests pin the dispatch with fakes
     * and no Minecraft bootstrap.
     *
     * @return the number of mobs actually fired
     */
    static <M> int tickOnce(Set<UUID> ids, Function<UUID, M> finder,
                            Function<M, List<Ability>> abilitiesFor,
                            BiConsumer<List<Ability>, M> fire) {
        int fired = 0;
        for (UUID uuid : ids) {
            M mob = finder.apply(uuid);
            if (mob == null) continue;
            List<Ability> abilities = abilitiesFor.apply(mob);
            if (abilities == null || abilities.isEmpty()) continue;
            fire.accept(abilities, mob);
            fired++;
        }
        return fired;
    }
}
