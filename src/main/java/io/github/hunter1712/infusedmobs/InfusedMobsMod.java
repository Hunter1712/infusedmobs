package io.github.hunter1712.infusedmobs;

import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.ability.trigger.MobDeathTrigger;
import io.github.hunter1712.infusedmobs.ability.trigger.MobHurtTrigger;
import io.github.hunter1712.infusedmobs.ability.trigger.MobTickTrigger;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class InfusedMobsMod implements ModInitializer {
	public static final String MOD_ID = "infusedmobs";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("InfusedMobs initializing...");

		AbilityRegistry.registerAll();
		MobTickTrigger.register();
		MobHurtTrigger.register();
		MobDeathTrigger.register();
	}
}
