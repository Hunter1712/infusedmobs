package io.github.hunter1712.infusedmobs.tier;

import java.util.List;

/**
 * The immutable result of a mob's roll — the persisted source of truth shared
 * by every Versioned Source Set's {@code TierSavedData} Shim.
 * <ul>
 *   <li>{@link Tiered} — the mob rolled a Tier; abilities are stored by id
 *       so the exact set is restored (no re-roll on reload).</li>
 *   <li>{@link Split} — a Rupture split copy; its own distinct variant so
 *       it is never re-rolled into a regular Infused Mob (which could gain
 *       DEATH abilities and recurse).</li>
 *   <li>{@link Nothing} — the mob rolled nothing and must never roll again.</li>
 * </ul>
 * <p>
 * The honest shape: callers discriminate by kind ({@code instanceof}
 * pattern matching). Tier is available only on {@link Tiered} rolls —
 * split-copy and empty rolls expose no Tier accessor, so no caller can
 * observe a null Tier. Every storage adapter (codec on 26.2, NBT on
 * 1.21.1/1.20.1) encodes through {@link #kind} plus the variant payload,
 * and {@link #decode} decodes from the same ({@code kind}, {@code tier},
 * {@code abilityIds}) shape. Decoding is lenient — unknown kinds, unknown or
 * missing tiers and missing ability lists all degrade to {@link Nothing} so a
 * corrupted save never fails world load.
 */
public sealed interface Rolled {

    /** Stored discriminator: {@code "tiered"}, {@code "split"} or {@code "nothing"}. */
    String kind();

    /** Stored ability ids of a {@link Tiered} or {@link Split} roll, else empty. */
    List<String> abilityIds();

    record Tiered(MobTier tier, List<String> abilityIds) implements Rolled {
        @Override
        public String kind() {
            return "tiered";
        }

        /** Stored tier name for persistence. */
        public String tierName() {
            return tier.name();
        }
    }

    record Split(List<String> abilityIds) implements Rolled {
        @Override
        public String kind() {
            return "split";
        }
    }

    record Nothing() implements Rolled {
        @Override
        public String kind() {
            return "nothing";
        }

        @Override
        public List<String> abilityIds() {
            return List.of();
        }
    }

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
        List<String> copiedAbilityIds = abilityIds == null ? List.of() : List.copyOf(abilityIds);
        if ("tiered".equals(kind)) {
            MobTier tier = null;
            if (tierName != null && !tierName.isEmpty()) {
                try {
                    tier = MobTier.valueOf(tierName);
                } catch (IllegalArgumentException ignored) {
                    tier = null;
                }
            }
            return tier != null ? new Tiered(tier, copiedAbilityIds) : new Nothing();
        }
        if ("split".equals(kind)) {
            return new Split(copiedAbilityIds);
        }
        return new Nothing();
    }
}
