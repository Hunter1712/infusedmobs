package io.github.hunter1712.mobabilities.tier;

/**
 * Elemental-themed tiers that a hostile mob can spawn with.
 * Each tier defines its spawn probability, ability count range,
 * and health/XP multipliers.
 */
public enum MobTier {

    EMBER(0.5,  1, 2, 3.0, 3.0),
    SURGE(0.3, 3, 4, 5.0, 5.0),
    TEMPEST(0.15, 5, 7, 8.0, 8.0);

    private final double spawnChance;
    private final int minAbilities;
    private final int maxAbilities;
    private final double healthMultiplier;
    private final double xpMultiplier;

    MobTier(double spawnChance, int minAbilities, int maxAbilities,
            double healthMultiplier, double xpMultiplier) {
        this.spawnChance = spawnChance;
        this.minAbilities = minAbilities;
        this.maxAbilities = maxAbilities;
        this.healthMultiplier = healthMultiplier;
        this.xpMultiplier = xpMultiplier;
    }

    public double spawnChance() { return spawnChance; }
    public int minAbilities() { return minAbilities; }
    public int maxAbilities() { return maxAbilities; }
    public double healthMultiplier() { return healthMultiplier; }
    public double xpMultiplier() { return xpMultiplier; }
}
