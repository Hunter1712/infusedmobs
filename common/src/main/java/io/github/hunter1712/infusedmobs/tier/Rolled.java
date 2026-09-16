package io.github.hunter1712.infusedmobs.tier;

import java.util.List;

/**
 * The immutable result of a mob's roll — the persisted source of truth shared
 * by every Versioned Source Set's {@code TierSavedData} Shim.
 * <ul>
 *   <li>{@link Tiered} — the mob rolled a Tier; abilities are stored by id
 *       so the exact set is restored (no re-roll on reload).</li>
 *   <li>{@link Split} — a Rupture split copy; its own distinct variant so
 *       it is never re-rolled into a regular tiered mob (which could gain
 *       DEATH abilities and recurse).</li>
 *   <li>{@link Nothing} — the mob rolled nothing and must never roll again.</li>
 * </ul>
 * <p>
 * The static helpers are the single home of the wire rules: every storage
 * adapter (codec on 26.2, NBT on 1.21.1/1.20.1) encodes to and decodes from
 * the same ({@code kind}, {@code tier}, {@code abilityIds}) shape through
 * them. Decoding is lenient — unknown kinds, unknown or missing tiers and
 * missing ability lists all degrade to {@link Nothing} so a corrupted save
 * never fails world load.
 */
public sealed interface Rolled {

    record Tiered(MobTier tier, List<String> abilityIds) implements Rolled {}

    record Split(List<String> abilityIds) implements Rolled {}

    record Nothing() implements Rolled {}

    /**
     * Rebuilds a roll from its stored parts. Never throws and never returns
     * null: anything unrecognised becomes {@link Nothing}.
     *
     * @param kind       {@code "tiered"}, {@code "split"} or anything else;
     *                   null and empty mean "nothing" (matching adapters whose
     *                   missing kind defaults that way)
     * @param tierName   {@link MobTier} name; null, empty or unknown degrades
     *                   a {@code "tiered"} payload to {@link Nothing}
     * @param abilityIds stored ability ids; null defaults to empty
     */
    static Rolled decode(String kind, String tierName, List<String> abilityIds) {
        List<String> ids = abilityIds == null ? List.of() : List.copyOf(abilityIds);
        if ("tiered".equals(kind)) {
            MobTier tier = null;
            if (tierName != null && !tierName.isEmpty()) {
                try {
                    tier = MobTier.valueOf(tierName);
                } catch (IllegalArgumentException ignored) {
                    tier = null;
                }
            }
            return tier != null ? new Tiered(tier, ids) : new Nothing();
        }
        if ("split".equals(kind)) {
            return new Split(ids);
        }
        return new Nothing();
    }

    /** Stored discriminator: {@code "tiered"}, {@code "split"} or {@code "nothing"}. */
    static String kindOf(Rolled rolled) {
        if (rolled instanceof Tiered) {
            return "tiered";
        }
        if (rolled instanceof Split) {
            return "split";
        }
        return "nothing";
    }

    /** The Tier of a {@link Tiered} roll, otherwise null (split copies have no Tier). */
    static MobTier tierOf(Rolled rolled) {
        if (rolled instanceof Tiered t) {
            return t.tier();
        }
        return null;
    }

    /** Stored tier name, or null unless {@link Tiered} (split entries persist no tier). */
    static String tierNameOf(Rolled rolled) {
        MobTier tier = tierOf(rolled);
        return tier == null ? null : tier.name();
    }

    /** Stored ability ids of a {@link Tiered} or {@link Split} roll, else empty. */
    static List<String> abilityIdsOf(Rolled rolled) {
        if (rolled instanceof Tiered t) {
            return t.abilityIds();
        }
        if (rolled instanceof Split s) {
            return s.abilityIds();
        }
        return List.of();
    }
}
