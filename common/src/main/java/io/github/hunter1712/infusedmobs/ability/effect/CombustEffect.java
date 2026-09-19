package io.github.hunter1712.infusedmobs.ability.effect;

import io.github.hunter1712.infusedmobs.config.ModConfig;
import io.github.hunter1712.infusedmobs.platform.Platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Detonates a Combust blast around the dying mob: distance-falloff damage
 * to nearby living entities plus explosion sound, no particles or block damage.
 * Mirrors {@link SplitEffect}: the Ability pool registers, this module hits.
 */
public final class CombustEffect {

    /** Base damage at point-blank before falloff. */
    private static final float BASE_DAMAGE = 4.0f;

    private CombustEffect() {}

    /**
     * Explodes around the given mob. Does nothing off-server.
     *
     * @param mob the dying mob
     */
    public static void apply(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        double radius = ModConfig.get().combustExplosionPower() * 2.0;
        var entities = level.getEntities(mob, mob.getBoundingBox().inflate(radius));
        var dmgSource = level.damageSources().explosion(null, null);
        for (var entity : entities) {
            if (entity instanceof LivingEntity living && entity != mob) {
                double dist = entity.distanceTo(mob);
                if (dist <= radius) {
                    Platform.hooks().hurtFromExplosion(living, level, dmgSource, damageFor(dist, radius));
                }
            }
        }
        // Explosion sound without particles or block damage
        level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0f, 1.0f);
    }

    /**
     * Pure falloff behind {@link #apply}: linear from base at point-blank
     * to a 1.0 minimum at the rim. Plain arithmetic so unit tests pin the
     * curve without bootstrap.
     */
    static float damageFor(double dist, double radius) {
        return Math.max((float) (BASE_DAMAGE * (1.0 - dist / radius)), 1.0f);
    }
}
