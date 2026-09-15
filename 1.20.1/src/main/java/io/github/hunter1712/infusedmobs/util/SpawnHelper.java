package io.github.hunter1712.infusedmobs.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Version shim for entity creation (1.20.1 — no EntitySpawnReason).
 * 1.20.1's {@code EntityType} has no 2-arg {@code create(Level, reason)}
 * overload (only 1-arg {@code create(Level)} and spawn methods taking
 * {@code MobSpawnType}); 26.2 introduced {@code EntitySpawnReason}.
 * Split copies are positioned immediately after creation, so the plain
 * 1-arg create preserves behaviour.
 */
public final class SpawnHelper {
    private SpawnHelper() {}

    @SuppressWarnings("unchecked")
    public static <T extends Entity> T create(EntityType<T> type, ServerLevel level) {
        return (T) type.create(level);
    }
}
