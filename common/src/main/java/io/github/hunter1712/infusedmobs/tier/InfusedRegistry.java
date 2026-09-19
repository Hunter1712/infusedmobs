package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.ability.TriggerType;

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
 */
public final class InfusedRegistry {

    private final Map<UUID, Rolled> tracked = new HashMap<>();

    /** Starts tracking this UUID as the given roll. */
    public void track(UUID id, Rolled rolled) {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(rolled, "rolled");
        tracked.put(id, rolled);
    }

    /** Returns the tracked roll for this UUID, or null if untracked. */
    public Rolled find(UUID id) {
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
        return find(id) instanceof Rolled.Split;
    }

    /**
     * Returns the UUIDs of tracked mobs that have at least one TICK ability.
     * <p>
     * Returns a snapshot so concurrent removal during iteration (mob death
     * mid-scan) cannot throw a {@code ConcurrentModificationException}.
     */
    public Set<UUID> tickMobUUIDs() {
        return tracked.entrySet().stream()
                .filter(e -> !AbilityRegistry.forTrigger(
                        AbilityRegistry.getAbilitiesByIds(e.getValue().abilityIds()),
                        TriggerType.TICK).isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    /** Defensive copy of every tracked roll, for iteration. */
    public Map<UUID, Rolled> snapshot() {
        return new HashMap<>(tracked);
    }
}
