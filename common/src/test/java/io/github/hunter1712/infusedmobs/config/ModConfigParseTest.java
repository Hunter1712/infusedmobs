package io.github.hunter1712.infusedmobs.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Disk-free tests for the corrupt-file path behind {@link ModConfig#load}.
 * <p>
 * Pins {@code parseOrNull} (raw Gson parse, no clamping or backfill) without
 * a {@code FabricLoader} or disk harness.
 */
class ModConfigParseTest {

    @Test
    void defaultsRoundTripThroughJson() {
        ModConfig.Instance defaults = ModConfig.Instance.defaults();

        ModConfig.Instance parsed = ModConfig.parseOrNull(ModConfig.toJson(defaults));

        assertEquals(defaults, parsed);
    }

    @Test
    void malformedJsonParsesToNull() {
        assertNull(ModConfig.parseOrNull("{ not json"));
    }

    @Test
    void nullInputParsesToNull() {
        assertNull(ModConfig.parseOrNull(null));
    }

    @Test
    void nullLiteralParsesToNull() {
        assertNull(ModConfig.parseOrNull("null"));
    }
}
