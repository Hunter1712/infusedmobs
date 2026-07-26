package io.github.hunter1712.infusedmobs.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;

import net.minecraft.core.UUIDUtil;
import net.minecraft.resources.Identifier;
import net.minecraft.util.datafix.DataFixTypes;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.level.saveddata.SavedDataType;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Persists tier roll results to disk so that mobs keep their tier
 * (or lack thereof) across world reloads.
 * <p>
 * Stored per-world in {@code data/infusedmobs_tiers.dat} and loaded
 * on demand via {@link net.minecraft.world.level.storage.SavedDataStorage#computeIfAbsent(SavedDataType)}.
 */
public final class TierSavedData extends SavedData {

    private static final Codec<TierSavedData> CODEC = RecordCodecBuilder.<TierSavedData>create(instance ->
            instance.ap2(
                    instance.point(TierSavedData::new),
                    Codec.unboundedMap(UUIDUtil.STRING_CODEC, MobTier.CODEC)
                            .fieldOf("tiers")
                            .forGetter(d -> d.tiers),
                    UUIDUtil.CODEC_SET
                            .fieldOf("rolled_nothing")
                            .forGetter(d -> d.rolledNothing)
            )
    );

    public static final SavedDataType<TierSavedData> TYPE = new SavedDataType<>(
            Identifier.fromNamespaceAndPath("infusedmobs", "tiers"),
            TierSavedData::new,
            CODEC,
            DataFixTypes.LEVEL
    );

    private final Map<UUID, MobTier> tiers;
    private final Set<UUID> rolledNothing;

    /** Creates an empty store. */
    public TierSavedData() {
        this(new HashMap<>(), new HashSet<>());
    }

    private TierSavedData(Map<UUID, MobTier> tiers, Set<UUID> rolledNothing) {
        // Copy into mutable collections — the codec may produce immutable maps
        this.tiers = new HashMap<>(tiers);
        this.rolledNothing = new HashSet<>(rolledNothing);
    }

    // ========================================
    // Queries
    // ========================================

    /** Returns the tier assigned to this UUID, or null. */
    public MobTier getTier(UUID uuid) {
        return tiers.get(uuid);
    }

    /** Returns true if this UUID has already been rolled (tier or nothing). */
    public boolean hasRolled(UUID uuid) {
        return tiers.containsKey(uuid) || rolledNothing.contains(uuid);
    }

    // ========================================
    // Mutations
    // ========================================

    /** Records that this UUID rolled a specific tier. */
    public void setTier(UUID uuid, MobTier tier) {
        tiers.put(uuid, tier);
        rolledNothing.remove(uuid);
        setDirty();
    }

    /** Records that this UUID rolled and got nothing. */
    public void markRolledNothing(UUID uuid) {
        rolledNothing.add(uuid);
        tiers.remove(uuid);
        setDirty();
    }

    /** Removes all tracking for this UUID (called on mob death). */
    public void remove(UUID uuid) {
        if (tiers.remove(uuid) != null || rolledNothing.remove(uuid)) {
            setDirty();
        }
    }
}
