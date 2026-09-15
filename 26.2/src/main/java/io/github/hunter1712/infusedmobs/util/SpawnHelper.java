package io.github.hunter1712.infusedmobs.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;

/**
 * Version shim for entity spawn reason (26.2 — EntitySpawnReason).
 */
public final class SpawnHelper {
    private SpawnHelper() {}

    @SuppressWarnings("unchecked")
    public static <T extends Entity> T create(EntityType<T> type, ServerLevel level) {
        return (T) type.create(level, EntitySpawnReason.REINFORCEMENT);
    }
}
