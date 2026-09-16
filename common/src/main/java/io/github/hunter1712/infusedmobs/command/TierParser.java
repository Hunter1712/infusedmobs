package io.github.hunter1712.infusedmobs.command;

import io.github.hunter1712.infusedmobs.tier.MobTier;

import java.util.Locale;

/**
 * Parses Tier names for the command tree.
 * Derived from the enum so adding a Tier works without touching callers.
 */
final class TierParser {
    private TierParser() {}

    static MobTier parse(String name) {
        if (name == null) return null;
        try {
            return MobTier.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
