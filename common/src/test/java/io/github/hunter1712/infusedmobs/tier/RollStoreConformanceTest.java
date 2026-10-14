package io.github.hunter1712.infusedmobs.tier;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Both RollStore adapters answer the same CRUD shape behind one seam.
 * Lifetimes stay separate per ADR-0003; this pins interchangeability.
 */
class RollStoreConformanceTest {

    private static void exercise(RollStore store) {
        UUID id = UUID.randomUUID();
        assertNull(store.get(id));
        Rolled tiered = new Rolled.Tiered(MobTier.CINDER, List.of("bane"));
        store.put(id, tiered);
        assertEquals(tiered, store.get(id));
        assertEquals(1, store.snapshot().size());
        assertTrue(store.remove(id));
        assertNull(store.get(id));
        assertTrue(store.snapshot().isEmpty());
    }

    @Test
    void liveRegistryConforms() {
        exercise(new InfusedRegistry());
    }

    @Test
    void persistedStoreConforms() {
        exercise(new TierRollStore());
    }
}
