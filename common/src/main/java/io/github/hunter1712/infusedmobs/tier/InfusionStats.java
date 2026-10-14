package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.config.ModConfig;

/**
 * Single stat decisions behind the Infusion seam.
 *
 * <p>Tier assignment, Rupture split copies, and the XP mixin all share one
 * rule: split copies carry full Cinder stats with no Tier of their own.
 */
public final class InfusionStats {

    /** Fraction of boosted max health a Rupture split copy spawns with. */
    public static final double SPLIT_HEALTH_FRACTION = 0.6;

    private InfusionStats() {}

    /**
     * XP multiplier for a mob: the Tier's configured multiplier, or the
     * Cinder multiplier when the mob is a Tier-less split copy carrying
     * full Cinder stats.
     */
    public static double xpMultiplier(MobTier tier, boolean splitCopy, ModConfig.Instance config) {
        if (tier != null) return config.forTier(tier).xpMultiplier();
        if (splitCopy) return config.forTier(MobTier.CINDER).xpMultiplier();
        return 1.0;
    }
}
