package io.github.hunter1712.infusedmobs.tier;

import java.util.List;

/**
 * Formats Tier nametag text from colour, Ability names and entity name.
 * Pure string logic — the visibility gate and component wiring stay at the call site.
 * Tier colours come from {@link MobTier#colourCode()}; split copies use grey.
 */
final class NametagFormatter {
    private NametagFormatter() {}

    /** Greyscale colour code for Rupture split-copy nametags (no Tier). */
    static final String SPLIT_COPY_COLOUR = "§7";

    static String format(String colour, List<String> abilityNames, String entityName) {
        if (abilityNames.isEmpty()) return colour + entityName;
        return colour + String.join("§7, ", abilityNames) + " §f" + entityName;
    }

    /** Tier nametag text using the Tier's own colour. */
    static String formatTier(MobTier tier, List<String> abilityNames, String entityName) {
        return format(tier.colourCode(), abilityNames, entityName);
    }

    /** Split-copy nametag text — greyscale, no Tier. */
    static String formatSplit(List<String> abilityNames, String entityName) {
        return format(SPLIT_COPY_COLOUR, abilityNames, entityName);
    }
}
