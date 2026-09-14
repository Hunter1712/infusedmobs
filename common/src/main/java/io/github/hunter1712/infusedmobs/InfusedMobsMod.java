package io.github.hunter1712.infusedmobs;

import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.ability.trigger.MobDeathTrigger;
import io.github.hunter1712.infusedmobs.ability.trigger.MobHurtTrigger;
import io.github.hunter1712.infusedmobs.ability.trigger.MobTickTrigger;
import io.github.hunter1712.infusedmobs.command.InfusedMobsCommand;
import io.github.hunter1712.infusedmobs.config.ModConfig;
import io.github.hunter1712.infusedmobs.gamerules.ModGameRules;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.minecraft.world.entity.Mob;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfusedMobsMod implements ModInitializer {
    public static final String MOD_ID = "infusedmobs";

    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitialize() {
        LOGGER.info("InfusedMobs initializing...");

        ModConfig.load();
        ModGameRules.register();
        AbilityRegistry.registerAll();
        MobTickTrigger.register();
        MobHurtTrigger.register();
        MobDeathTrigger.register();
        InfusedMobsCommand.register();

        // Tier assignment entry point (replaces the MobSpawnMixin): fires for
        // every entity that enters a server level, including chunk reloads —
        // assignTier is idempotent thanks to TierSavedData + its own guards.
        ServerEntityEvents.ENTITY_LOAD.register((entity, level) -> {
            if (entity instanceof Mob mob) {
                MobTierManager.assignTier(mob);
            }
        });
    }
}
