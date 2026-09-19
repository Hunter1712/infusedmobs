package io.github.hunter1712.infusedmobs.ability.effect;

import io.github.hunter1712.infusedmobs.platform.Platform;
import io.github.hunter1712.infusedmobs.platform.PlatformHooks;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

/**
 * Spawns 2 copies of the dying mob's entity type at 60% max health.
 * Each copy receives Cinder-tier stats and 1 random ability — Rupture
 * itself is excluded (any other, including Combust, is fair game).
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
            Entity raw = Platform.hooks().spawn(type, level, PlatformHooks.SpawnKind.REINFORCEMENT);
            if (raw instanceof Mob copy) {
                placeCopy(copy, mob, i);
                // Registers the copy (split-copy entry) BEFORE it enters the
                // world, so the spawn handler won't roll a tier for it.
                MobTierManager.applyCinderTierToSplitCopy(copy);
                level.addFreshEntity(copy);
            }
        }
    }

    /** Positions the copy at a diagonal offset from the original mob. */
    static void placeCopy(Mob copy, LivingEntity original, int index) {
        double offset = copyOffset(index);
        copy.setPos(original.getX() + offset,
                    original.getY(),
                    original.getZ() + offset);
    }

    /**
     * Pure copy-placement offset behind {@link #placeCopy}: copies alternate
     * sides of the diagonal with growing magnitude, so the current two-copy
     * behavior (-1.5, +1.5) is preserved while any future count spreads
     * instead of stacking on one spot. Plain arithmetic so unit tests pin
     * the spawn geometry without bootstrap.
     */
    static double copyOffset(int index) {
        return (index % 2 == 0 ? -1.0 : 1.0) * COPY_OFFSET * ((index / 2) + 1);
    }
}
