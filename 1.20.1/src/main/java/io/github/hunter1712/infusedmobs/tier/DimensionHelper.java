package io.github.hunter1712.infusedmobs.tier;

import net.minecraft.server.level.ServerLevel;

/**
 * Version shim for dimension accessor (1.20.1 — ResourceLocation).
 * Uses {@code location()} which exists on 1.20.1's ResourceKey;
 * 26.2 renamed this to {@code identifier()}.
 */
public final class DimensionHelper {
    private DimensionHelper() {}

    public static String getId(ServerLevel level) {
        return level.dimension().location().toString();
    }
}
