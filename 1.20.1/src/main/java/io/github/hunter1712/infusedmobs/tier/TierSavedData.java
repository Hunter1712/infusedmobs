package io.github.hunter1712.infusedmobs.tier;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
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

    /**
     * The immutable result of a mob's roll — the persisted source of truth.
     * <ul>
     *   <li>{@link Tiered} — the mob rolled a tier; abilities are stored by id
     *       so the exact set is restored (no re-roll on reload).</li>
     *   <li>{@link Split} — a Rupture split copy; its own distinct variant so
     *       it is never re-rolled into a regular tiered mob (which could gain
     *       DEATH abilities and recurse).</li>
     *   <li>{@link Nothing} — the mob rolled nothing and must never roll again.</li>
     * </ul>
     */
    public sealed interface Rolled {

        record Tiered(MobTier tier, List<String> abilityIds) implements Rolled {}

        record Split(List<String> abilityIds) implements Rolled {}

        record Nothing() implements Rolled {}
    }

    private static final String ID = "infusedmobs_tiers";

    private final Map<UUID, Rolled> rolls;

    /** Creates an empty store. */
    public TierSavedData() {
        this(new HashMap<>());
    }

    private TierSavedData(Map<UUID, Rolled> rolls) {
        this.rolls = new HashMap<>(rolls);
    }

    // ========================================
    // Queries
    // ========================================

    /** Returns the stored roll for this UUID, or null if never rolled. */
    public Rolled getRolled(UUID uuid) {
        return rolls.get(uuid);
    }

    // ========================================
    // Mutations
    // ========================================

    /** Records the roll result for this UUID. */
    public void setRolled(UUID uuid, Rolled rolled) {
        rolls.put(uuid, rolled);
        setDirty();
    }

    /** Removes all tracking for this UUID (called on mob death or despawn). */
    public void remove(UUID uuid) {
        if (rolls.remove(uuid) != null) {
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
        for (Map.Entry<UUID, Rolled> e : rolls.entrySet()) {
            CompoundTag entry = new CompoundTag();
            Rolled r = e.getValue();
            if (r instanceof Rolled.Tiered t) {
                entry.putString("kind", "tiered");
                entry.putString("tier", t.tier().name());
                ListTag list = new ListTag();
                for (String id : t.abilityIds()) list.add(StringTag.valueOf(id));
                entry.put("abilityIds", list);
            } else if (r instanceof Rolled.Split s) {
                entry.putString("kind", "split");
                ListTag list = new ListTag();
                for (String id : s.abilityIds()) list.add(StringTag.valueOf(id));
                entry.put("abilityIds", list);
            } else {
                entry.putString("kind", "nothing");
            }
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
                String kind = entry.contains("kind", Tag.TAG_STRING) ? entry.getString("kind") : "";
                List<String> abilityIds = List.of();
                if (entry.contains("abilityIds", Tag.TAG_LIST)) {
                    ListTag list = entry.getList("abilityIds", Tag.TAG_STRING);
                    abilityIds = list.stream().map(t -> t.getAsString()).toList();
                }
                Rolled rolled = switch (kind) {
                    case "tiered" -> {
                        String tierName = entry.contains("tier", Tag.TAG_STRING) ? entry.getString("tier") : "";
                        MobTier tier;
                        try { tier = tierName.isEmpty() ? null : MobTier.valueOf(tierName); }
                        catch (IllegalArgumentException ex) { tier = null; }
                        yield tier != null ? new Rolled.Tiered(tier, abilityIds) : new Rolled.Nothing();
                    }
                    case "split" -> new Rolled.Split(abilityIds);
                    default -> new Rolled.Nothing();
                };
                data.rolls.put(uuid, rolled);
            } catch (IllegalArgumentException ignored) {}
        }
        return data;
    }
}
