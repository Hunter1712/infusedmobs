package io.github.hunter1712.infusedmobs.tier;

import net.minecraft.server.level.ServerLevel;

/**
 * Version shim for dimension accessor.
 * <p>
 * 26.2 uses {@code ResourceKey#identifier()} (Identifier) while 1.20.1 uses
 * {@code ResourceKey#location()} (ResourceLocation). This shim isolates the
 * difference so {@link MobTierManager} can stay in common.
 */
public final class DimensionHelper {
    private DimensionHelper() {}

    public static String getId(ServerLevel level) {
        return level.dimension().identifier().toString();
    }
}
