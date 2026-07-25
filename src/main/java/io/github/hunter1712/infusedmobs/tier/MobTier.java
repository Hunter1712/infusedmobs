package io.github.hunter1712.infusedmobs.tier;

/**
 * Elemental-themed tiers that a hostile mob can spawn with.
 * Each tier defines its spawn probability, how many abilities of
 * each trigger type it grants, and health/XP multipliers.
 */
public enum MobTier {

    EMBER(0.5,   1, 0, 0, 3.0, 3.0),
    SURGE(0.3,   1, 1, 0, 5.0, 5.0),
    TEMPEST(0.15, 1, 1, 1, 8.0, 8.0);

    private final double spawnChance;
    private final int hurtAbilities;
    private final int tickAbilities;
    private final int deathAbilities;
    private final double healthMultiplier;
    private final double xpMultiplier;

    MobTier(double spawnChance, int hurtAbilities, int tickAbilities,
            int deathAbilities, double healthMultiplier, double xpMultiplier) {
        this.spawnChance = spawnChance;
        this.hurtAbilities = hurtAbilities;
        this.tickAbilities = tickAbilities;
        this.deathAbilities = deathAbilities;
        this.healthMultiplier = healthMultiplier;
        this.xpMultiplier = xpMultiplier;
    }

    public double spawnChance() { return spawnChance; }
    public int hurtAbilities() { return hurtAbilities; }
    public int tickAbilities() { return tickAbilities; }
    public int deathAbilities() { return deathAbilities; }
    public double healthMultiplier() { return healthMultiplier; }
    public double xpMultiplier() { return xpMultiplier; }
}
