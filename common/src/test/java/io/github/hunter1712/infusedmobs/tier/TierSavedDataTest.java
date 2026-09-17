package io.github.hunter1712.infusedmobs.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.JsonOps;
import com.google.gson.JsonObject;
import com.google.gson.JsonArray;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure codec round-trip tests for {@link Rolled}.
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
        var original = new Rolled.Tiered(MobTier.DOOM, List.of("bane", "rupture"));
        assertEquals(original, roundTrip(TierSavedData.ROLLED_CODEC, original));
    }

    @Test
    void tieredRoundTripsWithEmptyAbilities() {
        var original = new Rolled.Tiered(MobTier.CINDER, List.of());
        assertEquals(original, roundTrip(TierSavedData.ROLLED_CODEC, original));
    }

    @Test
    void splitRoundTripsWithAbilities() {
        var original = new Rolled.Split(List.of("siphon"));
        assertEquals(original, roundTrip(TierSavedData.ROLLED_CODEC, original));
    }

    @Test
    void nothingRoundTrips() {
        var original = new Rolled.Nothing();
        assertEquals(original, roundTrip(TierSavedData.ROLLED_CODEC, original));
    }

    @Test
    void splitEncodesWithoutTierField() {
        var encoded = TierSavedData.ROLLED_CODEC
                .encodeStart(JsonOps.INSTANCE, new Rolled.Split(List.of("ward")))
                .getOrThrow();
        assertEquals("split", encoded.getAsJsonObject().get("kind").getAsString());
        assertTrue(!encoded.getAsJsonObject().has("tier"),
                "split rolls must not carry a tier field");
    }

    @Test
    void tieredEncodesKindTierAndAbilityIds() {
        var encoded = TierSavedData.ROLLED_CODEC
                .encodeStart(JsonOps.INSTANCE, new Rolled.Tiered(MobTier.SHADE, List.of("hex")))
                .getOrThrow().getAsJsonObject();
        assertEquals("tiered", encoded.get("kind").getAsString());
        assertEquals("SHADE", encoded.get("tier").getAsString());
        assertEquals(1, encoded.getAsJsonArray("abilityIds").size());
        assertEquals("hex", encoded.getAsJsonArray("abilityIds").get(0).getAsString());
    }

    @Test
    void nothingEncodesOnlyLongStandingForm() {
        // Save-shape lock: Nothing writes no tier and no abilities — kind is
        // the long-standing "nothing" default (omitted when equal to default),
        // so downgrades never see unexpected data.
        var encoded = TierSavedData.ROLLED_CODEC
                .encodeStart(JsonOps.INSTANCE, new Rolled.Nothing())
                .getOrThrow().getAsJsonObject();
        if (encoded.has("kind")) {
            assertEquals("nothing", encoded.get("kind").getAsString());
        }
        assertTrue(!encoded.has("tier"),
                "nothing rolls must not carry a tier field");
        if (encoded.has("abilityIds")) {
            assertEquals(0, encoded.getAsJsonArray("abilityIds").size(),
                    "nothing rolls must not carry abilities");
        }
    }

    @Test
    void setRolledAndGetRolledRoundTripInMemory() {
        TierSavedData store = new TierSavedData();
        var rolled = new Rolled.Tiered(MobTier.SHADE, List.of("hex", "thorns"));

        store.setRolled(java.util.UUID.randomUUID(), rolled);
        // A second UUID to prove per-UUID storage
        java.util.UUID other = java.util.UUID.randomUUID();
        store.setRolled(other, new Rolled.Nothing());

        assertTrue(store.getRolled(other) instanceof Rolled.Nothing);
    }

    // ========================================
    // #8: corrupted-save tolerance — codec must match NBT fallback to Nothing
    // ========================================

    private static Rolled decodeRolled(JsonObject json) {
        return TierSavedData.ROLLED_CODEC.decode(JsonOps.INSTANCE, json).getOrThrow().getFirst();
    }

    @Test
    void unknownKindDecodesToNothing() {
        JsonObject json = new JsonObject();
        json.addProperty("kind", "bogus");
        json.add("abilityIds", new JsonArray());
        assertTrue(decodeRolled(json) instanceof Rolled.Nothing,
                "unknown kind must degrade to Nothing, not fail");
    }

    @Test
    void missingKindDecodesToNothing() {
        JsonObject json = new JsonObject();
        json.add("abilityIds", new JsonArray());
        assertTrue(decodeRolled(json) instanceof Rolled.Nothing,
                "missing kind must degrade to Nothing, matching NBT shim");
    }

    @Test
    void unknownTierFallsBackToNothing() {
        JsonObject json = new JsonObject();
        json.addProperty("kind", "tiered");
        json.addProperty("tier", "ULTRA");
        JsonArray ids = new JsonArray();
        ids.add("bane");
        json.add("abilityIds", ids);
        assertTrue(decodeRolled(json) instanceof Rolled.Nothing,
                "corrupted tier name must fall back to Nothing, matching NBT shim");
    }

    @Test
    void missingTierInTieredFallsBackToNothing() {
        JsonObject json = new JsonObject();
        json.addProperty("kind", "tiered");
        json.add("abilityIds", new JsonArray());
        assertTrue(decodeRolled(json) instanceof Rolled.Nothing,
                "tiered without tier must fall back to Nothing");
    }

    @Test
    void missingAbilityIdsDefaultsToEmpty() {
        JsonObject json = new JsonObject();
        json.addProperty("kind", "split");
        var decoded = decodeRolled(json);
        assertEquals(new Rolled.Split(List.of()), decoded);
    }

    // ========================================
    // #8: store-level round-trip — tiered, split-copy, empty via outer CODEC
    // ========================================

    @Test
    void storeRoundTripsTieredSplitAndEmpty() {
        java.util.UUID tieredId = java.util.UUID.randomUUID();
        java.util.UUID splitId = java.util.UUID.randomUUID();
        java.util.UUID emptyId = java.util.UUID.randomUUID();

        TierSavedData store = new TierSavedData();
        var tiered = new Rolled.Tiered(MobTier.DOOM, List.of("bane", "rupture"));
        var split = new Rolled.Split(List.of("siphon"));
        var empty = new Rolled.Nothing();
        store.setRolled(tieredId, tiered);
        store.setRolled(splitId, split);
        store.setRolled(emptyId, empty);

        TierSavedData reloaded = roundTrip(TierSavedData.CODEC, store);

        assertEquals(tiered, reloaded.getRolled(tieredId));
        assertEquals(split, reloaded.getRolled(splitId));
        assertTrue(reloaded.getRolled(emptyId) instanceof Rolled.Nothing);
    }

    @Test
    void storeMissingRollsDecodesToEmpty() {
        // Fresh/corrupted save without rolls field must load as empty, matching NBT shims
        JsonObject json = new JsonObject();
        TierSavedData decoded = TierSavedData.CODEC.decode(JsonOps.INSTANCE, json).getOrThrow().getFirst();
        assertTrue(decoded.getRolled(java.util.UUID.randomUUID()) == null,
                "missing rolls must decode to empty store");
    }

    @Test
    void splitCopyNeverRetainsTierAfterReload() {
        java.util.UUID id = java.util.UUID.randomUUID();
        TierSavedData store = new TierSavedData();
        store.setRolled(id, new Rolled.Split(List.of("combust")));

        TierSavedData reloaded = roundTrip(TierSavedData.CODEC, store);

        var rolled = reloaded.getRolled(id);
        assertTrue(rolled instanceof Rolled.Split,
                "split copy must reload as Split, never Tiered");
        assertTrue(!(rolled instanceof Rolled.Tiered),
                "split copy must never gain a tier on reload");
    }
}
