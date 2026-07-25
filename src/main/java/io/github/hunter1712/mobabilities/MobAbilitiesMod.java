package io.github.hunter1712.mobabilities;

import io.github.hunter1712.mobabilities.ability.AbilityRegistry;
import io.github.hunter1712.mobabilities.ability.trigger.MobDeathTrigger;
import io.github.hunter1712.mobabilities.ability.trigger.MobHurtTrigger;
import io.github.hunter1712.mobabilities.ability.trigger.MobTickTrigger;

import net.fabricmc.api.ModInitializer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobAbilitiesMod implements ModInitializer {
	public static final String MOD_ID = "mobabilities";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("MobAbilitiesMod initializing...");

		AbilityRegistry.registerAll();
		MobTickTrigger.register();
		MobHurtTrigger.register();
		MobDeathTrigger.register();
	}
}
