package io.github.hunter1712.infusedmobs.tier;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Pure nametag text formatting behind the presentation seam — no mob, no
 * level, no Minecraft bootstrap required.
 */
class NametagPresentationTest {

    @Test
    void textUsesTierColourForTieredRoll() {
        Rolled rolled = new Rolled.Tiered(MobTier.DOOM, List.of("bane"));

        assertEquals("§cBane §fZombie",
                NametagPresentation.text(rolled, List.of("Bane"), "Zombie"));
    }

    @Test
    void textIsGreyscaleForSplitRoll() {
        Rolled rolled = new Rolled.Split(List.of("ward"));

        assertEquals("§7Ward §fZombie",
                NametagPresentation.text(rolled, List.of("Ward"), "Zombie"));
    }

    @Test
    void textOmitsEmptyAbilityNames() {
        assertEquals("§aZombie",
                NametagPresentation.text(
                        new Rolled.Tiered(MobTier.CINDER, List.of()), List.of(), "Zombie"));
        assertEquals("§7Zombie",
                NametagPresentation.text(
                        new Rolled.Split(List.of()), List.of(), "Zombie"));
    }
}
