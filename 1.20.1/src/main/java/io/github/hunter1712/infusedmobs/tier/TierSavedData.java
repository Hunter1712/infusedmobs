package io.github.hunter1712.infusedmobs.tier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Persists the complete infused state of every rolled mob to disk so that
 * mobs keep their tier, abilities, and split-copy status across world
 * reloads (and chunk unload/reload cycles).
 * <p>
 * Stored per-world in {@code data/infusedmobs_tiers.dat} and loaded
 * on demand via {@link #get(ServerLevel)}.
 * <p>
 * 1.20.1 uses the legacy NBT path
 * ({@code DimensionDataStorage#computeIfAbsent(Function, Supplier, String)}
 * with single-arg {@code save(CompoundTag)}); field names ({@code rolls},
 * {@code kind}, {@code tier}, {@code abilityIds}) match the modern codec
 * path so saves stay conceptually compatible across versions.
 */
public final class TierSavedData extends SavedData {

    private static final String ID = "infusedmobs_tiers";

    private final TierRollStore store;

    /** Creates an empty store. */
    public TierSavedData() {
        this.store = new TierRollStore();
    }

    private TierSavedData(Map<UUID, Rolled> rolls) {
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

    /** Version-agnostic accessor: legacy NBT path via load/supplier functions. */
    public static TierSavedData get(ServerLevel level) {
        return level.getDataStorage().computeIfAbsent(TierSavedData::load, TierSavedData::new, ID);
    }

    // ========================================
    // NBT serialization — same field names as codec
    // ========================================

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag rollsTag = new CompoundTag();
        for (Map.Entry<UUID, Rolled> e : store.entries()) {
            CompoundTag entry = new CompoundTag();
            Rolled r = e.getValue();
            entry.putString("kind", r.kind());
            String tierName = r.tierName();
            if (tierName != null) {
                entry.putString("tier", tierName);
            }
            ListTag list = new ListTag();
            for (String id : r.abilityIds()) {
                list.add(StringTag.valueOf(id));
            }
            entry.put("abilityIds", list);
            rollsTag.put(e.getKey().toString(), entry);
        }
        tag.put("rolls", rollsTag);
        return tag;
    }

    public static TierSavedData load(CompoundTag tag) {
        TierSavedData data = new TierSavedData();
        if (!tag.contains("rolls", Tag.TAG_COMPOUND)) return data;
        CompoundTag rollsTag = tag.getCompound("rolls");
        for (String key : rollsTag.getAllKeys()) {
            try {
                UUID uuid = UUID.fromString(key);
                if (!rollsTag.contains(key, Tag.TAG_COMPOUND)) continue;
                CompoundTag entry = rollsTag.getCompound(key);
                String kind = entry.contains("kind", Tag.TAG_STRING) ? entry.getString("kind") : null;
                String tierName = entry.contains("tier", Tag.TAG_STRING) ? entry.getString("tier") : null;
                List<String> abilityIds = List.of();
                if (entry.contains("abilityIds", Tag.TAG_LIST)) {
                    ListTag list = entry.getList("abilityIds", Tag.TAG_STRING);
                    abilityIds = list.stream().map(t -> t.getAsString()).toList();
                }
                data.store.put(uuid, Rolled.decode(kind, tierName, abilityIds));
            } catch (IllegalArgumentException ignored) {}
        }
        return data;
    }
}
