package io.github.hunter1712.infusedmobs.tier;

import java.util.List;

/**
 * Formats Tier nametag text from colour, Ability names and entity name.
 * Pure string logic — the visibility gate and component wiring stay at the call site.
 * Tier colours come from {@link MobTier#colourCode()}; split copies use grey.
 */
final class NametagFormatter {
    private NametagFormatter() {}

    static String format(String colour, List<String> abilityNames, String entityName) {
        if (abilityNames.isEmpty()) return colour + entityName;
        return colour + String.join("§7, ", abilityNames) + " §f" + entityName;
    }

    /**
     * True when {@code customName} is a nametag this mod owns: one of our
     * Tier/split colours followed by text ending in the entity type name.
     * Covers both render shapes ({@code colour + entity} and
     * {@code colour + abilities + " §f" + entity}).
     * <p>
     * Guards compat with mods that name mobs themselves (e.g. polymerized
     * spiders): foreign names are never overwritten or cleared. Plain string
     * logic so unit tests pin ownership without bootstrap.
     */
    static boolean owns(String customName, String entityName) {
        if (customName == null || entityName == null || entityName.isEmpty()) return false;
        if (!customName.endsWith(entityName)) return false;
        return customName.startsWith("§a")
                || customName.startsWith("§e")
                || customName.startsWith("§c")
                || customName.startsWith("§7");
    }
}
