package io.github.hunter1712.infusedmobs.ability.effect;

import io.github.hunter1712.infusedmobs.platform.Platform;
import io.github.hunter1712.infusedmobs.platform.PlatformHooks;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.function.Consumer;

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
     * <p>
     * The Infusion assignment runs through the given assigner so the
     * effect stays one-way injectable in tests; live code passes the
     * Infusion seam ({@code MobTierManager::applyCinderTierToSplitCopy}).
     *
     * @param mob the dying mob
     * @param assigner registers each copy before it enters the world
     */
    public static void apply(LivingEntity mob, Consumer<Mob> assigner) {
        if (!(mob.level() instanceof ServerLevel level)) return;

        EntityType<?> type = mob.getType();
        for (int i = 0; i < COPY_COUNT; i++) {
            Entity raw = Platform.hooks().spawn(type, level, PlatformHooks.SpawnKind.REINFORCEMENT);
            if (raw instanceof Mob copy) {
                placeCopy(copy, mob, i);
                // Registers the copy (split-copy entry) BEFORE it enters the
                // world, so the spawn handler won't roll a tier for it.
                assigner.accept(copy);
                level.addFreshEntity(copy);
            }
        }
    }

    /**
     * Spawns 2 split copies with live Infusion assignment.
     * <p>
     * This default wires the Infusion seam directly; it is the live
     * adapter, not a layer to mock — tests use
     * {@link #apply(LivingEntity, Consumer)} with a recording assigner.
     * Rupture registration stays in the Ability pool (with the other 14
     * abilities, locked by registration tests), so this return edge is the
     * documented direction: Ability pool → Infusion seam, never the reverse.
     *
     * @param mob the dying mob
     */
    public static void apply(LivingEntity mob) {
        apply(mob, MobTierManager::applyCinderTierToSplitCopy);
    }

    /** Positions the copy at a diagonal offset from the original mob. */
    static void placeCopy(Mob copy, LivingEntity original, int index) {
        double offset = copyOffset(index);
        copy.setPos(original.getX() + offset,
                    original.getY(),
                    original.getZ() + offset);
    }

    /**
     * Pure copy-placement offset behind {@link #placeCopy}: first copy takes
     * the negative diagonal, second the positive one. Plain arithmetic so
     * unit tests pin the spawn geometry without bootstrap.
     */
    static double copyOffset(int index) {
        return index == 0 ? -COPY_OFFSET : COPY_OFFSET;
    }
}
