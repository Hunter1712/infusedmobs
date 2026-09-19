package io.github.hunter1712.infusedmobs.tier;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contract for Tier nametag text formatting.
 */
class NametagFormatterTest {

    @Test
    void joinsAbilityNamesWithColourAndEntity() {
        assertEquals("§aBane§7, Ward §fZombie",
                NametagFormatter.format("§a", List.of("Bane", "Ward"), "Zombie"));
    }

    @Test
    void emptyAbilitiesRenderCleanNametag() {
        assertEquals("§aZombie",
                NametagFormatter.format("§a", List.of(), "Zombie"));
    }

    @Test
    void tierColoursPreserved() {
        assertEquals("§aBane §fZombie",
                NametagFormatter.format(MobTier.CINDER.colourCode(), List.of("Bane"), "Zombie"));
        assertEquals("§eBane §fZombie",
                NametagFormatter.format(MobTier.SHADE.colourCode(), List.of("Bane"), "Zombie"));
        assertEquals("§cBane §fZombie",
                NametagFormatter.format(MobTier.DOOM.colourCode(), List.of("Bane"), "Zombie"));
        assertEquals("§7Bane §fZombie",
                NametagFormatter.format("§7", List.of("Bane"), "Zombie"));
    }
}
