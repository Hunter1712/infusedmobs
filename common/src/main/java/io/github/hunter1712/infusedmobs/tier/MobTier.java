package io.github.hunter1712.infusedmobs.tier;

import java.util.Locale;

/**
 * Occult-themed tiers that a hostile mob can spawn with.
 * Each tier defines its spawn probability, total ability count,
 * and health/XP multipliers. Abilities are drawn from the unified pool.
 * Rarest tier last: VOID is the top of the ladder.
 * <p>
 * These values are the single source of truth — config defaults are
 * derived from these getters via {@code ModConfig.Instance.defaults()}.
 */
public enum MobTier {

    CINDER(0.2,  1, 1.0, 1.0),
    SHADE(0.1,   2, 2.0, 2.0),
    DOOM(0.05,   3, 3.0, 3.0),
    VOID(0.025,  4, 4.0, 4.0);

    private final double spawnChance;
    private final int abilityCount;
    private final double healthMultiplier;
    private final double xpMultiplier;

    MobTier(double spawnChance, int abilityCount,
            double healthMultiplier, double xpMultiplier) {
        this.spawnChance = spawnChance;
        this.abilityCount = abilityCount;
        this.healthMultiplier = healthMultiplier;
        this.xpMultiplier = xpMultiplier;
    }

    public double spawnChance() { return spawnChance; }
    public int abilityCount() { return abilityCount; }
    public double healthMultiplier() { return healthMultiplier; }
    public double xpMultiplier() { return xpMultiplier; }

    /** Returns the Minecraft colour code used for this tier's nametag and UI. */
    public String colourCode() {
        return switch (this) {
            case CINDER -> "§a";
            case SHADE -> "§e";
            case DOOM -> "§c";
            case VOID -> "§5";
        };
    }

    /**
     * Parses a Tier name case-insensitively (e.g. summon input).
     * Derived from the enum so adding a Tier works without touching callers.
     *
     * @return the Tier, or null for null/unknown names
     */
    public static MobTier parse(String name) {
        if (name == null) return null;
        try {
            return MobTier.valueOf(name.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
