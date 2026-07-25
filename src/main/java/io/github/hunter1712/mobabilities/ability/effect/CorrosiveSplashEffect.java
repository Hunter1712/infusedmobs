package io.github.hunter1712.mobabilities.ability.effect;

import io.github.hunter1712.mobabilities.effect.ModEffects;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.LivingEntity;

/**
 * Spawns an {@link AreaEffectCloud} that applies {@link com.example.effect.AcidEffect}
 * to any entity inside it.
 *
 * <p>Triggered when a Creeper with the Corrosive Splash ability dies.
 */
public final class CorrosiveSplashEffect {

    private CorrosiveSplashEffect() {
        // static-only utility class
    }

    /**
     * Creates the acid pool at the given entity's position.
     *
     * @param entity the dying mob (should be a Creeper)
     * @param level  the server level
     */
    public static void apply(LivingEntity entity, ServerLevel level) {
        AreaEffectCloud cloud = new AreaEffectCloud(level, entity.getX(), entity.getY(), entity.getZ());
        cloud.setRadius(3.0f);
        cloud.setRadiusOnUse(0.5f);
        cloud.setWaitTime(10);
        cloud.setDuration(200);
        cloud.setRadiusPerTick(-0.005f);
        cloud.addEffect(new MobEffectInstance(ModEffects.ACID, 100, 0));
        level.addFreshEntity(cloud);
    }
}
