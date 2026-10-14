package io.github.hunter1712.infusedmobs.tier;

import java.util.Map;
import java.util.UUID;

/**
 * Shared seam for UUID-keyed roll state.
 *
 * <p>Both adapters keep their own lifetimes — live Infused Mob tracking
 * dies with the mob, persisted Tier rolls survive reloads (see ADR-0003)
 * — but they answer the same four operations so one conformance test
 * pins them both and callers learn one shape.
 */
public interface RollStore {

    /** Stored roll for this UUID, or null if never rolled. */
    Rolled get(UUID uuid);

    /** Records the roll result for this UUID. */
    void put(UUID uuid, Rolled rolled);

    /**
     * Removes stored state for this UUID.
     *
     * @return true if anything was stored
     */
    boolean remove(UUID uuid);

    /** Defensive copy of every stored roll. */
    Map<UUID, Rolled> snapshot();
}
