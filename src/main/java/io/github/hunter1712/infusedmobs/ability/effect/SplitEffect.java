package io.github.hunter1712.infusedmobs.ability.effect;

import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.Level;

/**
 * Spawns 2 copies of the dying mob's entity type at 60 % max health.
 * Each copy receives 1 random HURT ability but no full tier assignment
 * (flagged via {@link MobTierManager#markSplitCopy} to prevent
 * infinite recursion).
 */
public final class SplitEffect {

    private SplitEffect() {}

    /**
     * Spawns 2 split copies of the given mob at offset positions.
     * Does nothing if called on the client side.
     *
     * @param mob the dying mob
     */
    public static void apply(LivingEntity mob) {
        Level rawLevel = mob.level();
        if (!(rawLevel instanceof ServerLevel level)) return;

        EntityType<?> type = mob.getType();
        for (int i = 0; i < 2; i++) {
            Entity raw = type.create(level, EntitySpawnReason.REINFORCEMENT);
            if (raw instanceof Mob copy) {
                double offsetX = (i == 0) ? -1.5 : 1.5;
                double offsetZ = (i == 0) ? -1.5 : 1.5;
                copy.setPos(mob.getX() + offsetX, mob.getY(), mob.getZ() + offsetZ);
                copy.setHealth(copy.getMaxHealth() * 0.6f);

                // Flag so MobTierManager skips tier assignment, then give it 1 HURT ability
                MobTierManager.markSplitCopy(copy.getUUID());
                MobTierManager.assignSplitAbility(copy);

                level.addFreshEntity(copy);
            }
        }
    }
}
