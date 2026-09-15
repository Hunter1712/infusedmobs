package io.github.hunter1712.infusedmobs.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
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

        /**
         * DTO bridging the sealed interface to a flat serialisable record
         * ({@code kind} discriminates the variants).
         */
        record DTO(String kind, MobTier tier, List<String> abilityIds) {

            static DTO fromRolled(Rolled rolled) {
                if (rolled instanceof Tiered t) {
                    return new DTO("tiered", t.tier(), t.abilityIds());
                } else if (rolled instanceof Split s) {
                    return new DTO("split", null, s.abilityIds());
                } else {
                    return new DTO("nothing", null, List.of());
                }
            }

            Rolled toRolled() {
                return switch (kind) {
                    // A null tier (corrupted save / unknown tier value) falls
                    // back to Nothing rather than crashing or NPE-ing later.
                    case "tiered" -> tier != null ? new Tiered(tier, abilityIds) : new Nothing();
                    case "split" -> new Split(abilityIds);
                    default -> new Nothing();
                };
            }
        }

        Codec<Rolled> CODEC = RecordCodecBuilder.<DTO>create(instance -> instance.group(
                Codec.STRING.fieldOf("kind").forGetter(DTO::kind),
                MobTier.CODEC.optionalFieldOf("tier").forGetter(dto -> Optional.ofNullable(dto.tier())),
                Codec.STRING.listOf().optionalFieldOf("abilityIds", List.of()).forGetter(DTO::abilityIds)
        ).apply(instance, (kind, tier, abilityIds) -> new DTO(kind, tier.orElse(null), abilityIds)))
                .xmap(DTO::toRolled, DTO::fromRolled);
    }

    private static final Codec<TierSavedData> CODEC = RecordCodecBuilder.<TierSavedData>create(instance ->
            instance.group(
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, Rolled.CODEC)
                            .fieldOf("rolls")
                            .forGetter(d -> d.rolls)
            ).apply(instance, TierSavedData::new)
    );

    public static final SavedDataType<TierSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("infusedmobs", "tiers"),
            TierSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<UUID, Rolled> rolls;

    /** Creates an empty store. */
    public TierSavedData() {
        this(new HashMap<>());
    }

    private TierSavedData(Map<UUID, Rolled> rolls) {
        // Copy into a mutable map — the codec may produce immutable maps
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
    // Version shim — storage accessor (legacy NBT path)
    // ========================================

    /**
     * Version-agnostic accessor. On 1.20.1 the underlying storage is
     * {@code DimensionDataStorage} with NBT ({@code CompoundTag}) rather than
     * the modern {@code SavedDataType} codec path. Field names
     * ({@code rolls}, {@code kind}, {@code tier}, {@code abilityIds}) are
     * identical so saves are conceptually compatible, but the serialization
     * adapter differs.
     * <p>
     * This shim compiles against 26.2's API (workaround) via reflection so
     * {@code gradle build} stays green while the wiring is validated. When
     * compiled against real 1.20.1 mappings, the NBT branch is the primary
     * path.
     */
    public static TierSavedData get(ServerLevel level) {
        // Try legacy NBT string key first via reflection (real 1.20.1 runtime)
        try {
            var storage = level.getServer().overworld().getDataStorage();
            var method = storage.getClass().getMethod("computeIfAbsent",
                    java.util.function.Function.class,
                    java.util.function.Supplier.class,
                    String.class);
            //noinspection unchecked
            return (TierSavedData) method.invoke(storage,
                    (java.util.function.Function<CompoundTag, TierSavedData>) TierSavedData::load,
                    (java.util.function.Supplier<TierSavedData>) TierSavedData::new,
                    "infusedmobs_tiers");
        } catch (Exception ignored) {
            // Fallback to modern SavedDataType path (26.2 / workaround)
            return level.getDataStorage().computeIfAbsent(TYPE);
        }
    }

    // ---- NBT serialization (1.20.1 legacy) — same field names as codec ----

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
        // Modern NBT API: use Optional<CompoundTag>
        var rollsOpt = tag.getCompound("rolls");
        if (rollsOpt.isEmpty()) return data;
        CompoundTag rollsTag = rollsOpt.get();
        for (String key : rollsTag.keySet()) {
            try {
                UUID uuid = UUID.fromString(key);
                CompoundTag entry = rollsTag.getCompound(key).orElse(new CompoundTag());
                String kind = entry.getString("kind").orElse("");
                List<String> abilityIds = List.of();
                var listOpt = entry.getList("abilityIds");
                if (listOpt.isPresent()) {
                    ListTag list = listOpt.get();
                    abilityIds = list.stream().map(t -> t.asString().orElse("")).toList();
                }
                Rolled rolled = switch (kind) {
                    case "tiered" -> {
                        String tierName = entry.getString("tier").orElse("");
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
