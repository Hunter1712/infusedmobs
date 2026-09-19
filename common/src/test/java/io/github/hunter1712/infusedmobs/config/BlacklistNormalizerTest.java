package io.github.hunter1712.infusedmobs.config;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Contract for World Blacklist normalisation, through the config interface.
 */
class BlacklistNormalizerTest {

    @Test
    void trimsDedupsDropsBlanksAndNulls() {
        List<String> result = InstanceBuilder.valid().build().withWorldBlacklist(Arrays.asList(
                "  minecraft:overworld  ",
                "minecraft:the_nether",
                "minecraft:overworld",
                "",
                null)).worldBlacklist();

        assertEquals(List.of("minecraft:overworld", "minecraft:the_nether"), result);
    }

    @Test
    void nullReturnsEmpty() {
        assertTrue(InstanceBuilder.valid().build().withWorldBlacklist(null).worldBlacklist().isEmpty());
    }

    @Test
    void returnsImmutableList() {
        List<String> result = InstanceBuilder.valid().build()
                .withWorldBlacklist(List.of("minecraft:overworld")).worldBlacklist();

        assertThrows(UnsupportedOperationException.class,
                () -> result.add("minecraft:the_end"));
    }
}
