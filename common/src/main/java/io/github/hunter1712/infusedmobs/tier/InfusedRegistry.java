package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.TriggerType;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * UUID-keyed Infused Mob registry: live per-mob state behind the single
 * Infusion interface in {@link MobTierManager} (which also owns nametag
 * presentation). Tests instantiate a fresh registry per case with no
 * manual reset.
 */
public final class InfusedRegistry {

    private final Map<UUID, InfusedMob> tracked = new HashMap<>();

    /** Starts tracking this UUID as the given infused state. */
    public void track(UUID id, InfusedMob infused) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(infused, "infused");
        tracked.put(id, infused);
    }

    /** Returns the tracked state for this UUID, or null if untracked. */
    public InfusedMob find(UUID id) {
        return tracked.get(id);
    }

    /**
     * Stops tracking this UUID.
     *
     * @return true if anything was tracked
     */
    public boolean untrack(UUID id) {
        return tracked.remove(id) != null;
    }

    /** Clears all tracking. */
    public void clear() {
        tracked.clear();
    }

    /** Returns true if this UUID is tracked as a Rupture split copy (Cinder stats, no Tier). */
    public boolean isSplitCopy(UUID id) {
        return find(id) instanceof InfusedMob.SplitCopy;
    }

    /**
     * Returns the UUIDs of tracked mobs that have at least one TICK ability.
     * <p>
     * Returns a snapshot so concurrent removal during iteration (mob death
     * mid-scan) cannot throw a {@code ConcurrentModificationException}.
     */
    public Set<UUID> tickMobUUIDs() {
        return tracked.entrySet().stream()
                .filter(e -> !e.getValue().forTrigger(TriggerType.TICK).isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /** Defensive copy of every tracked mob, for iteration. */
    public Map<UUID, InfusedMob> snapshot() {
        return new HashMap<>(tracked);
    }
}
