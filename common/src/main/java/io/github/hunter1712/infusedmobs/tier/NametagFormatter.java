package io.github.hunter1712.infusedmobs.tier;

import java.util.List;

/**
 * Formats Tier nametag text from colour, Ability names and entity name.
 * Pure string logic — the visibility gate and component wiring stay at the call site.
 */
final class NametagFormatter {
    private NametagFormatter() {}

    static String format(String colour, List<String> abilityNames, String entityName) {
        return colour + String.join("§7, ", abilityNames) + " §f" + entityName;
    }
}
