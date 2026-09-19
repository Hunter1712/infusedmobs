package io.github.hunter1712.infusedmobs.tier;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour tests for the shared {@link Rolled} persistence model.
 * <p>
 * Every version adapter (codec on 26.2, NBT on 1.21.1/1.20.1) encodes to and
 * decodes from the same ({@code kind}, {@code tier}, {@code abilityIds})
 * shape through these helpers, so the lenient rules (unknown kind, unknown
 * tier, missing fields all degrade to {@link Rolled.Nothing} instead of
 * failing world load) are verified once, here, rather than per shim.
 */
class RolledTest {

    // ========================================
    // decode — valid payloads
    // ========================================

    @Test
    void tieredDecodesWithTierAndAbilities() {
        var decoded = Rolled.decode("tiered", "DOOM", List.of("bane", "rupture"));
        assertEquals(new Rolled.Tiered(MobTier.DOOM, List.of("bane", "rupture")), decoded);
    }

    @Test
    void splitDecodesWithAbilities() {
        var decoded = Rolled.decode("split", null, List.of("siphon"));
        assertEquals(new Rolled.Split(List.of("siphon")), decoded);
    }

    @Test
    void splitWithMissingAbilityIdsDefaultsToEmpty() {
        assertEquals(new Rolled.Split(List.of()), Rolled.decode("split", null, null));
    }

    @Test
    void tieredWithMissingAbilityIdsDefaultsToEmpty() {
        assertEquals(new Rolled.Tiered(MobTier.CINDER, List.of()),
                Rolled.decode("tiered", "CINDER", null));
    }

    // ========================================
    // decode — lenient degradation to Nothing
    // ========================================

    @Test
    void unknownKindDecodesToNothing() {
        assertTrue(Rolled.decode("bogus", null, List.of()) instanceof Rolled.Nothing);
    }

    @Test
    void nullKindDecodesToNothing() {
        assertTrue(Rolled.decode(null, null, null) instanceof Rolled.Nothing);
    }

    @Test
    void emptyKindDecodesToNothing() {
        // Legacy NBT path defaults a missing kind to "" — same outcome as missing.
        assertTrue(Rolled.decode("", null, List.of()) instanceof Rolled.Nothing);
    }

    @Test
    void tieredWithUnknownTierFallsBackToNothing() {
        assertTrue(Rolled.decode("tiered", "ULTRA", List.of("bane")) instanceof Rolled.Nothing);
    }

    @Test
    void tieredWithMissingTierFallsBackToNothing() {
        assertTrue(Rolled.decode("tiered", null, List.of()) instanceof Rolled.Nothing);
    }

    @Test
    void tieredWithEmptyTierFallsBackToNothing() {
        assertTrue(Rolled.decode("tiered", "", List.of()) instanceof Rolled.Nothing);
    }

    @Test
    void splitNeverGainsATier() {
        var decoded = Rolled.decode("split", "DOOM", List.of("combust"));
        assertTrue(decoded instanceof Rolled.Split);
        assertTrue(!(decoded instanceof Rolled.Tiered));
    }

    @Test
    void decodeCopiesAbilityIds() {
        var mutable = new ArrayList<>(List.of("bane"));
        var decoded = Rolled.decode("tiered", "SHADE", mutable);
        mutable.add("rupture");
        assertEquals(List.of("bane"), decoded.abilityIds());
    }

    // ========================================
    // polymorphic accessors — honest shape discriminated by kind
    // ========================================

    @Test
    void accessorsDescribeTiered() {
        Rolled.Tiered tiered = new Rolled.Tiered(MobTier.SHADE, List.of("hex"));
        assertEquals("tiered", tiered.kind());
        assertEquals(MobTier.SHADE, tiered.tier());
        assertEquals("SHADE", tiered.tierName());
        assertEquals(List.of("hex"), tiered.abilityIds());
    }

    @Test
    void splitExposesNoTierAccessor() {
        Rolled split = new Rolled.Split(List.of("ward"));
        assertEquals("split", split.kind());
        assertTrue(split instanceof Rolled.Split);
        assertTrue(!(split instanceof Rolled.Tiered),
                "split copy must never expose a Tier — discriminate by kind");
        assertEquals(List.of("ward"), split.abilityIds());
    }

    @Test
    void nothingExposesNoTierAccessor() {
        Rolled nothing = new Rolled.Nothing();
        assertEquals("nothing", nothing.kind());
        assertTrue(nothing instanceof Rolled.Nothing);
        assertTrue(!(nothing instanceof Rolled.Tiered),
                "empty roll must never expose a Tier — discriminate by kind");
        assertEquals(List.of(), nothing.abilityIds());
    }
}
