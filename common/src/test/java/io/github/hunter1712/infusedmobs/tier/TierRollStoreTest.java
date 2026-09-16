package io.github.hunter1712.infusedmobs.tier;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Behaviour tests for {@link TierRollStore}, the version-agnostic in-memory
 * roll map behind every {@code TierSavedData} shim. Storage adapters (codec
 * vs NBT) only add persistence and dirty-marking around this map.
 */
class TierRollStoreTest {

    @Test
    void putAndGetRoundTrip() {
        var store = new TierRollStore();
        var id = UUID.randomUUID();
        var rolled = new Rolled.Tiered(MobTier.DOOM, List.of("bane"));

        store.put(id, rolled);

        assertEquals(rolled, store.get(id));
    }

    @Test
    void missingUuidReturnsNull() {
        assertNull(new TierRollStore().get(UUID.randomUUID()));
    }

    @Test
    void removeExistingReturnsTrueAndForgets() {
        var store = new TierRollStore();
        var id = UUID.randomUUID();
        store.put(id, new Rolled.Nothing());

        assertTrue(store.remove(id));
        assertNull(store.get(id));
    }

    @Test
    void removeMissingReturnsFalse() {
        assertFalse(new TierRollStore().remove(UUID.randomUUID()));
    }

    @Test
    void initialMapIsCopied() {
        var id = UUID.randomUUID();
        Map<UUID, Rolled> initial = new HashMap<>();
        initial.put(id, new Rolled.Split(List.of("siphon")));

        var store = new TierRollStore(initial);
        initial.clear();

        assertEquals(new Rolled.Split(List.of("siphon")), store.get(id));
    }

    @Test
    void entriesExposeEveryRoll() {
        var store = new TierRollStore();
        var tieredId = UUID.randomUUID();
        var splitId = UUID.randomUUID();
        store.put(tieredId, new Rolled.Tiered(MobTier.CINDER, List.of()));
        store.put(splitId, new Rolled.Split(List.of("ward")));

        var entries = store.entries();

        assertEquals(2, entries.size());
        assertTrue(entries.stream().anyMatch(e ->
                e.getKey().equals(tieredId)
                        && e.getValue().equals(new Rolled.Tiered(MobTier.CINDER, List.of()))));
        assertTrue(entries.stream().anyMatch(e ->
                e.getKey().equals(splitId)
                        && e.getValue().equals(new Rolled.Split(List.of("ward")))));
    }

    @Test
    void nullRollIsRejected() {
        assertThrows(NullPointerException.class,
                () -> new TierRollStore().put(UUID.randomUUID(), null));
    }

    @Test
    void snapshotCopiesEveryRoll() {
        var store = new TierRollStore();
        var id = UUID.randomUUID();
        var rolled = new Rolled.Tiered(MobTier.SHADE, List.of("hex"));
        store.put(id, rolled);

        var snapshot = store.snapshot();

        assertEquals(Map.of(id, rolled), snapshot);
        snapshot.clear();
        assertEquals(rolled, store.get(id));
    }
}
