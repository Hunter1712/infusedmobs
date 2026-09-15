package io.github.hunter1712.infusedmobs.tier;

import net.minecraft.server.level.ServerLevel;

/**
 * Version shim for dimension accessor on 1.20.1.
 * <p>
 * 1.20.1's {@code ResourceKey} exposes {@code location()} (ResourceLocation)
 * rather than {@code identifier()} (Identifier). This shim uses reflection so
 * it compiles against 26.2's mappings (workaround) but at runtime correctly
 * resolves the 1.20.1 API.
 * <p>
 * TODO: When 1.20.1 is compiled against its real mappings, replace reflection
 * with direct {@code level.dimension().location().toString()}.
 */
public final class DimensionHelper {
    private DimensionHelper() {}

    public static String getId(ServerLevel level) {
        var key = level.dimension();
        try {
            var m = key.getClass().getMethod("identifier");
            Object id = m.invoke(key);
            return id.toString();
        } catch (Exception e) {
            try {
                var m2 = key.getClass().getMethod("location");
                Object id2 = m2.invoke(key);
                return id2.toString();
            } catch (Exception e2) {
                return key.toString();
            }
        }
    }
}
