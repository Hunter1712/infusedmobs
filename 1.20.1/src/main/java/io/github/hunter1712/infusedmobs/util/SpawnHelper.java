package io.github.hunter1712.infusedmobs.util;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

import java.lang.reflect.Method;

/**
 * Version shim for entity spawn reason on 1.20.1 (MobSpawnType vs EntitySpawnReason).
 * Uses reflection to compile against 26.2's EntitySpawnReason while at runtime
 * correctly invoking 1.20.1's MobSpawnType.REINFORCEMENT.
 */
public final class SpawnHelper {
    private SpawnHelper() {}

    @SuppressWarnings("unchecked")
    public static <T extends Entity> T create(EntityType<T> type, ServerLevel level) {
        // Try modern EntitySpawnReason first (26.2 workaround)
        try {
            Class<?> reasonClass = Class.forName("net.minecraft.world.entity.EntitySpawnReason");
            Object reason = Enum.valueOf((Class<Enum>) reasonClass, "REINFORCEMENT");
            Method create = EntityType.class.getMethod("create", ServerLevel.class, reasonClass);
            return (T) create.invoke(type, level, reason);
        } catch (Exception ignored) {}
        // Try legacy MobSpawnType
        try {
            Class<?> legacyClass = Class.forName("net.minecraft.world.entity.MobSpawnType");
            Object reason = Enum.valueOf((Class<Enum>) legacyClass, "REINFORCEMENT");
            Method create2 = EntityType.class.getMethod("create", ServerLevel.class, legacyClass);
            return (T) create2.invoke(type, level, reason);
        } catch (Exception ignored2) {}
        // Fallback: no suitable creation method — caller will handle null
        return null;
    }
}
