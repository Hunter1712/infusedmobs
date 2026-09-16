package io.github.hunter1712.infusedmobs.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract for World Blacklist normalisation.
 */
class BlacklistNormalizerTest {

    @Test
    void trimsDedupsDropsBlanksAndNulls() {
        List<String> result = BlacklistNormalizer.normalise(Arrays.asList(
                "  minecraft:overworld  ",
                "minecraft:the_nether",
                "minecraft:overworld",
                "",
                null));

        assertEquals(List.of("minecraft:overworld", "minecraft:the_nether"), result);
    }

    @Test
    void nullReturnsEmpty() {
        assertTrue(BlacklistNormalizer.normalise(null).isEmpty());
    }

    @Test
    void returnsImmutableList() {
        List<String> result = BlacklistNormalizer.normalise(List.of("minecraft:overworld"));

        assertThrows(UnsupportedOperationException.class,
                () -> result.add("minecraft:the_end"));
    }
}
