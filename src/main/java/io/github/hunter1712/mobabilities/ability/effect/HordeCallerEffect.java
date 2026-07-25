package io.github.hunter1712.mobabilities.ability.effect;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;

/**
 * Spawns 2–3 zombie reinforcements at 50 % health when the mob dies.
 */
public final class HordeCallerEffect {

    private HordeCallerEffect() {
        // static-only utility class
    }

    /**
     * Spawns zombie reinforcements around the death location.
     *
     * @param mob   the dying mob
     * @param level the server level
     */
    public static void apply(LivingEntity mob, ServerLevel level) {
        int count = 2 + mob.getRandom().nextInt(2); // 2 or 3

        for (int i = 0; i < count; i++) {
            Entity raw = EntityTypes.ZOMBIE.create(level, EntitySpawnReason.REINFORCEMENT);
            if (raw instanceof LivingEntity zombie) {
                double angle = 2.0 * Math.PI * i / count;
                double offsetX = Math.cos(angle) * 2.0;
                double offsetZ = Math.sin(angle) * 2.0;
                zombie.setPos(mob.getX() + offsetX, mob.getY(), mob.getZ() + offsetZ);
                zombie.setHealth(zombie.getMaxHealth() * 0.5f);
                level.addFreshEntity(zombie);
            }
        }
    }
}
