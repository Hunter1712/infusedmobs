package io.github.hunter1712.infusedmobs.tier;

/**
 * Single Tier-roll decision point behind the roll coordinator.
 * <p>
 * The effective Cinder / Shade / Doom shares match the documented Tier
 * contract directly: one uniform roll in {@code [0,1)} is compared against
 * disjoint intervals sized by each Tier's configured spawn chance, so the
 * effective share of each Tier equals its configured chance and the
 * remainder rolls nothing.
 * <p>
 * With defaults (Cinder 0.4, Shade 0.2, Doom 0.1) the intervals are
 * Doom {@code [0,0.1)}, Shade {@code [0.1,0.3)}, Cinder {@code [0.3,0.7)},
 * nothing {@code [0.7,1)} — effective shares 10% / 20% / 40%.
 * <p>
 * Rarest first (Doom, then Shade, then Cinder) so over-budget configs
 * truncate the common Tier first, never the rare one. Pure and
 * Minecraft-free for seeded-distribution tests.
 */
public final class TierRoll {

    private TierRoll() {}

    /**
     * Decides the Tier for a single uniform roll.
     *
     * @param roll uniform roll in {@code [0,1)}
     * @param cinderChance configured Cinder spawn chance
     * @param shadeChance configured Shade spawn chance
     * @param doomChance configured Doom spawn chance
     * @return the rolled Tier, or null for nothing
     */
    public static MobTier decide(double roll, double cinderChance, double shadeChance, double doomChance) {
        if (roll < doomChance) return MobTier.DOOM;
        if (roll < doomChance + shadeChance) return MobTier.SHADE;
        if (roll < doomChance + shadeChance + cinderChance) return MobTier.CINDER;
        return null;
    }
}
