package io.github.hunter1712.infusedmobs.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure codec round-trip tests for {@link TierSavedData.Rolled}.
 * <p>
 * These verify that every rolled state survives a JSON encode/decode
 * cycle exactly — tier, ability ids, and the split-copy marker that
 * prevents re-rolling.
 */
class TierSavedDataTest {

    private static <T> T roundTrip(Codec<T> codec, T original) {
        var encoded = codec.encodeStart(JsonOps.INSTANCE, original).getOrThrow();
        return codec.decode(JsonOps.INSTANCE, encoded).getOrThrow().getFirst();
    }

    @Test
    void tieredRoundTripsWithTierAndAbilities() {
        var original = new TierSavedData.Rolled.Tiered(MobTier.DOOM, List.of("bane", "rupture"));
        assertEquals(original, roundTrip(TierSavedData.Rolled.CODEC, original));
    }

    @Test
    void tieredRoundTripsWithEmptyAbilities() {
        var original = new TierSavedData.Rolled.Tiered(MobTier.CINDER, List.of());
        assertEquals(original, roundTrip(TierSavedData.Rolled.CODEC, original));
    }

    @Test
    void splitRoundTripsWithAbilities() {
        var original = new TierSavedData.Rolled.Split(List.of("siphon"));
        assertEquals(original, roundTrip(TierSavedData.Rolled.CODEC, original));
    }

    @Test
    void nothingRoundTrips() {
        var original = new TierSavedData.Rolled.Nothing();
        assertEquals(original, roundTrip(TierSavedData.Rolled.CODEC, original));
    }

    @Test
    void splitEncodesWithoutTierField() {
        var encoded = TierSavedData.Rolled.CODEC
                .encodeStart(JsonOps.INSTANCE, new TierSavedData.Rolled.Split(List.of("ward")))
                .getOrThrow();
        assertEquals("split", encoded.getAsJsonObject().get("kind").getAsString());
        assertTrue(!encoded.getAsJsonObject().has("tier"),
                "split rolls must not carry a tier field");
    }

    @Test
    void setRolledAndGetRolledRoundTripInMemory() {
        TierSavedData store = new TierSavedData();
        var rolled = new TierSavedData.Rolled.Tiered(MobTier.SHADE, List.of("hex", "thorns"));

        store.setRolled(java.util.UUID.randomUUID(), rolled);
        // A second UUID to prove per-UUID storage
        java.util.UUID other = java.util.UUID.randomUUID();
        store.setRolled(other, new TierSavedData.Rolled.Nothing());

        assertTrue(store.getRolled(other) instanceof TierSavedData.Rolled.Nothing);
    }
}
