package io.github.hunter1712.infusedmobs.tier;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Version-agnostic in-memory map of mob rolls behind every
 * {@code TierSavedData} Shim. Storage adapters (codec vs NBT) only add
 * persistence and dirty-marking around this map, so roll tracking behaves
 * identically on every Versioned Source Set.
 */
public final class TierRollStore {

    private final Map<UUID, Rolled> rolls;

    /** Creates an empty store. */
    public TierRollStore() {
        this.rolls = new HashMap<>();
    }

    /** Creates a store pre-filled with a copy of {@code initial}. */
    public TierRollStore(Map<UUID, Rolled> initial) {
        this.rolls = new HashMap<>(initial);
    }

    /** Returns the stored roll for this UUID, or null if never rolled. */
    public Rolled get(UUID uuid) {
        return rolls.get(uuid);
    }

    /** Records the roll result for this UUID. */
    public void put(UUID uuid, Rolled rolled) {
        rolls.put(Objects.requireNonNull(uuid), Objects.requireNonNull(rolled));
    }

    /**
     * Removes all tracking for this UUID (called on mob death or despawn).
     *
     * @return true if anything was tracked
     */
    public boolean remove(UUID uuid) {
        return rolls.remove(uuid) != null;
    }

    /** Defensive copy of every tracked roll, for serialization. */
    public Map<UUID, Rolled> snapshot() {
        return new HashMap<>(rolls);
    }
}
