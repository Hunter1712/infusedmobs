package io.github.hunter1712.mobabilities.ability.effect;

import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Gives a configurable chance to dodge an incoming attack by teleporting
 * to a random nearby position.
 *
 * <p>All tuning parameters are backed by static fields so
 * {@link com.example.config.ModConfig ModConfig} can push values at
 * initialisation time.
 */
public final class ShadowstepEffect {

    /** Probability (0.0 – 1.0) of a successful dodge. */
    private static double dodgeChance = 0.15;

    /** Cooldown in ticks between consecutive dodge attempts. */
    private static int cooldownTicks = 100; // 5 s @ 20 tps

    /** Maximum teleport distance in blocks. */
    private static double teleportRange = 8.0;

    /** Last tick when a dodge was attempted. Guarded by the mob's UUID. */
    private static final java.util.Map<java.util.UUID, Long> lastAttempt = new java.util.HashMap<>();

    private ShadowstepEffect() {
    }

    // ================================================================
    // Configuration setters (called by ModConfig)
    // ================================================================

    public static void setDodgeChance(double chance) {
        dodgeChance = Mth.clamp(chance, 0.0, 1.0);
    }

    public static void setCooldownTicks(int ticks) {
        cooldownTicks = Math.max(0, ticks);
    }

    public static void setTeleportRange(double range) {
        teleportRange = Math.max(1.0, range);
    }

    // ================================================================
    // Core logic
    // ================================================================

    /**
     * Attempts a shadowstep dodge.  Returns {@code true} if the dodge
     * succeeded (the caller should cancel the incoming attack).
     *
     * @param mob   the mob attempting to dodge
     * @param level the level
     * @return {@code true} if the dodge succeeded
     */
    public static boolean tryDodge(LivingEntity mob, Level level) {
        if (!(level instanceof ServerLevel serverLevel)) return false;

        // Cooldown check
        long gameTime = serverLevel.getGameTime();
        Long last = lastAttempt.get(mob.getUUID());
        if (last != null && (gameTime - last) < cooldownTicks) return false;
        lastAttempt.put(mob.getUUID(), gameTime);

        // Probability check
        if (mob.getRandom().nextDouble() >= dodgeChance) return false;

        // Find a random teleport destination
        Vec3 pos = mob.position();
        for (int attempt = 0; attempt < 8; attempt++) {
            double dx = mob.getRandom().nextDouble() * teleportRange * 2 - teleportRange;
            double dz = mob.getRandom().nextDouble() * teleportRange * 2 - teleportRange;
            BlockPos target = BlockPos.containing(
                    pos.x + dx,
                    Mth.clamp(pos.y + (mob.getRandom().nextDouble() * 4 - 2),
                            level.getMinY(), level.getMaxY()),
                    pos.z + dz
            );

            if (level.isUnobstructed(mob, level.getBlockState(target)
                    .getCollisionShape(level, target)
                    .move(target.getX(), target.getY(), target.getZ()))) {
                // Play teleport sound
                serverLevel.playSound(null, mob.xo, mob.yo, mob.zo,
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.0f);
                mob.teleportTo(target.getX() + 0.5, target.getY(), target.getZ() + 0.5);
                serverLevel.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.ENDERMAN_TELEPORT, SoundSource.HOSTILE, 1.0f, 1.0f);
                return true;
            }
        }
        return false;
    }
}
