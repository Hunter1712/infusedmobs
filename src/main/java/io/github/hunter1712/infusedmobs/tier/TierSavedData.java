package io.github.hunter1712.infusedmobs.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
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
                return switch (rolled) {
                    case Tiered t -> new DTO("tiered", t.tier(), t.abilityIds());
                    case Split s -> new DTO("split", null, s.abilityIds());
                    case Nothing n -> new DTO("nothing", null, List.of());
                };
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
}
