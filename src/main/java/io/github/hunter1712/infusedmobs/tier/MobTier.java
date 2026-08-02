package io.github.hunter1712.infusedmobs.tier;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;

import io.github.hunter1712.infusedmobs.config.ModConfig;

/**
 * Occult-themed tiers that a hostile mob can spawn with.
 * Each tier defines its spawn probability, total ability count,
 * and health/XP multipliers. Abilities are drawn from the unified pool.
 * <p>
 * These values are the single source of truth — config defaults are
 * derived from them via {@link #defaultConfig()}.
 */
public enum MobTier {

    CINDER(0.4,  1, 1.5, 1.5),
    SHADE(0.2,   2, 2.0, 2.0),
    DOOM(0.1,    3, 4.0, 4.0);

    /**
     * Codec for serialising tier values to / from disk.
     * <p>
     * Decoding an unknown tier name (corrupted or hand-edited save data)
     * reports a {@link DataResult#error} instead of throwing — callers
     * degrade gracefully instead of failing world load.
     */
    public static final Codec<MobTier> CODEC = Codec.STRING.flatXmap(
            MobTier::parse,
            tier -> DataResult.success(tier.name()));

    private static DataResult<MobTier> parse(String name) {
        try {
            return DataResult.success(valueOf(name));
        } catch (IllegalArgumentException e) {
            return DataResult.error(() -> "Unknown mob tier: '" + name + "'");
        }
    }

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

    /**
     * Returns the config defaults for this tier.
     * {@link ModConfig.Instance#defaults()} derives its per-tier values
     * from here, keeping the two in sync.
     */
    public ModConfig.TierConfig defaultConfig() {
        return new ModConfig.TierConfig(spawnChance, abilityCount, healthMultiplier, xpMultiplier);
    }

    /** Returns the Minecraft colour code used for this tier's nametag and UI. */
    public String colourCode() {
        return switch (this) {
            case CINDER -> "§a";
            case SHADE -> "§e";
            case DOOM -> "§c";
        };
    }
}
