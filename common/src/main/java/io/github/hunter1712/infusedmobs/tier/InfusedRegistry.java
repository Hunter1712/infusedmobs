package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UUID-keyed roll registry: live per-mob state behind the single
 * Infusion interface in {@link MobTierManager} (which also owns nametag
 * presentation). The stored value is the persisted {@link Rolled} itself —
 * abilities resolve to live objects at query time — so tracking and
 * persistence can never disagree. Tests instantiate a fresh registry per
 * case with no manual reset.
 * <p>
 * Implements the shared {@link RollStore} seam, so live CRUD travels the
 * same {@code put} / {@code get} / {@code remove} shape as the persisted
 * adapter; only live-only queries ({@code isSplitCopy},
 * {@code tickMobUUIDs}) keep their own names.
 */
final class InfusedRegistry implements RollStore {

    private final Map<UUID, Rolled> tracked = new HashMap<>();

    /** Clears all tracking. */
    public void clear() {
        tracked.clear();
    }

    @Override
    public Rolled get(UUID uuid) {
        return tracked.get(uuid);
    }

    @Override
    public void put(UUID uuid, Rolled rolled) {
        Objects.requireNonNull(uuid, "uuid");
        Objects.requireNonNull(rolled, "rolled");
        tracked.put(uuid, rolled);
    }

    @Override
    public boolean remove(UUID uuid) {
        return tracked.remove(uuid) != null;
    }

    /** Returns true if this UUID is tracked as a Rupture split copy (Cinder stats, no Tier). */
    public boolean isSplitCopy(UUID id) {
        return get(id) instanceof Rolled.Split;
    }

    /**
     * Returns the UUIDs of tracked mobs that have at least one TICK ability.
     * <p>
     * Returns a snapshot so concurrent removal during iteration (mob death
     * mid-scan) cannot throw a {@code ConcurrentModificationException}.
     * Tick knowledge lives behind the Ability seam — this module only asks.
     */
    public Set<UUID> tickMobUUIDs() {
        return tracked.entrySet().stream()
                .filter(e -> AbilityRegistry.hasTickAbilities(e.getValue().abilityIds()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /** Defensive copy of every tracked roll, for iteration. */
    @Override
    public Map<UUID, Rolled> snapshot() {
        return new HashMap<>(tracked);
    }
}
