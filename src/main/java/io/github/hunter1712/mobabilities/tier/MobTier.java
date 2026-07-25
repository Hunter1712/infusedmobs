package io.github.hunter1712.mobabilities.tier;

/**
 * Tiers that a hostile mob can spawn with.
 * Each tier defines its spawn probability, ability count range,
 * and health/XP multipliers.
 */
public enum MobTier {

    BLIGHTED(0.01,  1, 2, 3.0, 3.0,  "Blighted"),
    CORRUPTED(0.001, 3, 4, 5.0, 5.0, "Corrupted"),
    FORSAKEN(0.0001, 5, 7, 8.0, 8.0, "Forsaken");

    private final double spawnChance;
    private final int minAbilities;
    private final int maxAbilities;
    private final double healthMultiplier;
    private final double xpMultiplier;
    private final String displayName;

    MobTier(double spawnChance, int minAbilities, int maxAbilities,
            double healthMultiplier, double xpMultiplier, String displayName) {
        this.spawnChance = spawnChance;
        this.minAbilities = minAbilities;
        this.maxAbilities = maxAbilities;
        this.healthMultiplier = healthMultiplier;
        this.xpMultiplier = xpMultiplier;
        this.displayName = displayName;
    }

    public double spawnChance() { return spawnChance; }
    public int minAbilities() { return minAbilities; }
    public int maxAbilities() { return maxAbilities; }
    public double healthMultiplier() { return healthMultiplier; }
    public double xpMultiplier() { return xpMultiplier; }
    public String displayName() { return displayName; }
}
