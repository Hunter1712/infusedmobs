package io.github.hunter1712.infusedmobs.ability.effect;

import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Spawns 2 copies of the dying mob's entity type at 60% max health.
 * Each copy receives Cinder-tier stats and 1 random non-DEATH ability
 * (Rupture is excluded to prevent infinite recursion).
 */
public final class SplitEffect {

    private static final int COPY_COUNT = 2;
    private static final double COPY_OFFSET = 1.5;

    private SplitEffect() {}

    /**
     * Spawns 2 split copies of the given mob at offset positions.
     * Does nothing if called on the client side.
     *
     * @param mob the dying mob
     */
    public static void apply(LivingEntity mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;

        EntityType<?> type = mob.getType();
        for (int i = 0; i < COPY_COUNT; i++) {
            Entity raw = type.create(level, EntitySpawnReason.REINFORCEMENT);
            if (raw instanceof Mob copy) {
                placeCopy(copy, mob, i);
                MobTierManager.markSplitCopy(copy.getUUID());
                MobTierManager.applyCinderTierToSplitCopy(copy);
                level.addFreshEntity(copy);
            }
        }
    }

    /** Positions the copy at a diagonal offset from the original mob. */
    private static void placeCopy(Mob copy, LivingEntity original, int index) {
        double sign = (index == 0) ? -1.0 : 1.0;
        copy.setPos(original.getX() + sign * COPY_OFFSET,
                    original.getY(),
                    original.getZ() + sign * COPY_OFFSET);
    }
}
