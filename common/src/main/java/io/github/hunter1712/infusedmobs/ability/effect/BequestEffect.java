package io.github.hunter1712.infusedmobs.ability.effect;

import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.config.ModConfig;
import io.github.hunter1712.infusedmobs.platform.Platform;
import io.github.hunter1712.infusedmobs.tier.InfusionGate;
import io.github.hunter1712.infusedmobs.tier.MobTier;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Converts nearby vanilla mobs into Cinder-tiered mobs carrying the dead mob's
 * abilities. Mirrors {@link SplitEffect}: the Ability pool registers, this module hits.
 * <p>
 * Bequest is consumed by the dead mob and never migrates: the dead mob's first
 * non-Bequest ability is granted to each eligible vanilla mob within range.
 * Each convert receives exactly one ability (Cinder's cap) and is persisted so
 * it keeps its tier across reloads.
 * <p>
 * The pure conversion plan behind {@link #apply} is
 * {@link #planConversions(List, List, ModConfig.Instance, boolean)}: plain
 * data, no Minecraft bootstrap, unit-tested directly.
 */
public final class BequestEffect {

    /** Fixed heir-search radius — decoupled from Combust tuning on purpose. */
    private static final double HEIR_RADIUS = 8.0;

    /** One Bequest conversion: ally index gains one ability id. */
    public record Conversion(int allyIndex, String abilityId) {}

    /** A plain-data snapshot of a nearby mob, for the conversion plan. */
    public record MobCandidate(String entityKey, String category, boolean tiered, boolean splitCopy,
                               double distance) {}

    private BequestEffect() {}

    /**
     * Converts nearby vanilla mobs into Cinder-tiered mobs. Does nothing
     * off-server, when infusion is inactive in the dimension, or when the dead
     * mob has no transferable ability.
     *
     * @param mob the dying mob
     */
    public static void apply(Mob mob) {
        if (!(mob.level() instanceof ServerLevel level)) return;
        if (InfusionGate.status(level) != InfusionGate.Status.ACTIVE) return;

        List<String> deadIds = MobTierManager.abilityIdsOf(mob);

        List<Mob> allies = new ArrayList<>();
        for (var entity : level.getEntities(mob, mob.getBoundingBox().inflate(HEIR_RADIUS))) {
            if (!(entity instanceof Mob ally) || ally == mob || !ally.isAlive()) continue;
            if (ally.distanceTo(mob) > HEIR_RADIUS) continue;
            allies.add(ally);
        }
        allies.sort(Comparator.comparingDouble(a -> a.distanceTo(mob)));

        List<MobCandidate> candidates = new ArrayList<>();
        for (Mob ally : allies) {
            candidates.add(new MobCandidate(
                    Platform.hooks().entityKey(ally.getType()),
                    ally.getType().getCategory().name(),
                    MobTierManager.getTier(ally) != null,
                    MobTierManager.isSplitCopy(ally),
                    ally.distanceTo(mob)
            ));
        }

        ModConfig.Instance config = ModConfig.get();
        for (Conversion conversion : planConversions(deadIds, candidates, config, true)) {
            Mob ally = allies.get(conversion.allyIndex());
            MobTierManager.assignSpecificTier(ally, MobTier.CINDER,
                    List.of(AbilityRegistry.getById(conversion.abilityId())));
        }
    }

    /**
     * Pure conversion plan behind {@link #apply}: selects the dead mob's
     * transferable (non-Bequest) abilities, then hands them out one at a time
     * to each eligible ally — not already tiered, not a split copy, a hostile
     * monster, not blacklisted — nearest first. Each ally receives at most one
     * ability (Cinder's cap); once the dead mob's abilities are exhausted the
     * remaining allies convert with none.
     *
     * @param deadIds   the dead mob's persisted ability ids
     * @param allies    nearby mobs, pre-sorted nearest first
     * @param config    live config (for the mob blacklist and tier caps)
     * @param gateActive whether infusion is active in this dimension
     * @return one conversion per eligible ally, in the same order as {@code allies}
     */
    static List<Conversion> planConversions(List<String> deadIds, List<MobCandidate> allies,
                                            ModConfig.Instance config, boolean gateActive) {
        if (!gateActive) return List.of();

        List<String> transferable = deadIds.stream()
                .filter(id -> !id.equals(AbilityRegistry.BEQUEST_ID))
                .distinct()
                .toList();
        if (transferable.isEmpty()) return List.of();

        List<Conversion> conversions = new ArrayList<>();
        int abilityIndex = 0;
        for (int i = 0; i < allies.size(); i++) {
            MobCandidate ally = allies.get(i);
            if (ally.tiered()) continue;
            if (ally.splitCopy()) continue;
            if (!MobCategory.MONSTER.name().equals(ally.category())) continue;
            if (ally.entityKey() != null && config.isMobBlacklisted(ally.entityKey())) continue;
            if (abilityIndex >= transferable.size()) continue;  // exhausted — no ability left to give
            conversions.add(new Conversion(i, transferable.get(abilityIndex++)));
        }
        return conversions;
    }
}
