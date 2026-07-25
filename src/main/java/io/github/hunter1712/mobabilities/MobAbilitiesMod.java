package io.github.hunter1712.mobabilities;

import io.github.hunter1712.mobabilities.ability.AbilityRegistry;
import io.github.hunter1712.mobabilities.ability.trigger.MobDeathTrigger;
import io.github.hunter1712.mobabilities.ability.trigger.MobHurtTrigger;
import io.github.hunter1712.mobabilities.ability.trigger.MobSpawnTrigger;
import io.github.hunter1712.mobabilities.ability.trigger.MobTickTrigger;
import io.github.hunter1712.mobabilities.ability.trigger.ProjectileHitTrigger;
import io.github.hunter1712.mobabilities.config.ModConfig;
import io.github.hunter1712.mobabilities.damage.ModDamageTypes;
import io.github.hunter1712.mobabilities.effect.ModEffects;

import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.Identifier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MobAbilitiesMod implements ModInitializer {
	public static final String MOD_ID = "mobabilities";

	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	@Override
	public void onInitialize() {
		LOGGER.info("MobAbilitiesMod initializing...");

		ModEffects.onInitialize();
		ModDamageTypes.onInitialize();
		AbilityRegistry.registerAll();
		ModConfig.onInitialize();
		MobTickTrigger.register();
		MobHurtTrigger.register();
		MobDeathTrigger.register();
		ProjectileHitTrigger.register();
	}

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
