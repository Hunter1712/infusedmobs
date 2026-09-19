package io.github.hunter1712.infusedmobs.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import net.minecraft.server.level.ServerLevel;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Persists the complete infused state of every rolled mob to disk so that
 * mobs keep their tier, abilities, and split-copy status across world
 * reloads (and chunk unload/reload cycles).
 * <p>
 * Stored per-world in {@code data/infusedmobs_tiers.dat} and loaded
 * on demand via {@link net.minecraft.world.level.storage.SavedDataStorage#computeIfAbsent(SavedDataType)}.
 */
public final class TierSavedData extends SavedData {

    /**
     * DTO bridging the shared {@link Rolled} model to a flat serialisable
     * record ({@code kind} discriminates the variants). The Tier name rides
     * as plain text and resolves once in {@link Rolled#decode} — the single
     * lenient Tier parsing path, so unknown or missing tiers degrade to an
     * empty roll exactly like the NBT adapters.
     */
    record DTO(String kind, String tierName, List<String> abilityIds) {

        static DTO fromRolled(Rolled rolled) {
            return new DTO(rolled.kind(),
                    rolled instanceof Rolled.Tiered tiered ? tiered.tierName() : null,
                    rolled.abilityIds());
        }

        Rolled toRolled() {
            // A null or unknown tier name degrades to Nothing inside
            // Rolled.decode rather than crashing or NPE-ing later.
            return Rolled.decode(kind, tierName, abilityIds);
        }
    }

    /** Codec for a single roll, via the DTO bridge. */
    public static final Codec<Rolled> ROLLED_CODEC = RecordCodecBuilder.<DTO>create(instance -> instance.group(
                // Lenient: missing kind defaults to "nothing" (matching the NBT
                // adapters, where a missing kind also decodes to Nothing).
                // Unknown kind maps to Nothing via Rolled.decode, so corrupted
                // saves never fail world load.
                Codec.STRING.optionalFieldOf("kind", "nothing").forGetter(DTO::kind),
                // Lenient tier: decoded as raw string then resolved once in
                // Rolled.decode, so an unknown tier name falls back to
                // Nothing. Resolving here as well would parse every Tier
                // name twice on each roll load for no benefit.
                Codec.STRING.optionalFieldOf("tier").forGetter(dto ->
                        Optional.ofNullable(dto.tierName())),
                Codec.STRING.listOf().optionalFieldOf("abilityIds", List.of()).forGetter(DTO::abilityIds)
        ).apply(instance, (kind, tierName, abilityIds) -> new DTO(kind, tierName.orElse(null), abilityIds))
        ).xmap(DTO::toRolled, DTO::fromRolled);

    static final Codec<TierSavedData> CODEC = RecordCodecBuilder.<TierSavedData>create(instance ->
            instance.group(
                    // Lenient: missing rolls defaults to empty (matches NBT shims which
                    // return an empty store when the tag is absent, e.g. fresh worlds).
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, ROLLED_CODEC)
                            .optionalFieldOf("rolls", Map.of())
                            .forGetter(savedData -> savedData.store.snapshot())
            ).apply(instance, TierSavedData::new)
    );

    public static final SavedDataType<TierSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("infusedmobs", "tiers"),
            TierSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final TierRollStore store;

    /** Creates an empty store. */
    public TierSavedData() {
        this.store = new TierRollStore();
    }

    private TierSavedData(Map<UUID, Rolled> rolls) {
        // Copy into a fresh store — the codec may produce immutable maps
        this.store = new TierRollStore(rolls);
    }

    // ========================================
    // Queries
    // ========================================

    /** Returns the stored roll for this UUID, or null if never rolled. */
    public Rolled getRolled(UUID uuid) {
        return store.get(uuid);
    }

    // ========================================
    // Mutations
    // ========================================

    /** Records the roll result for this UUID. */
    public void setRolled(UUID uuid, Rolled rolled) {
        store.put(uuid, rolled);
        setDirty();
    }

    /** Removes all tracking for this UUID (called on mob death or despawn). */
    public void remove(UUID uuid) {
        if (store.remove(uuid)) {
            setDirty();
        }
    }

    // ========================================
    // Version shim — storage accessor
    // ========================================

    /** Version-agnostic accessor: modern codec path via SavedDataType. */
    public static TierSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TYPE);
    }
}
