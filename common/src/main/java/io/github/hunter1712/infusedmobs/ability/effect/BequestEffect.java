package io.github.hunter1712.infusedmobs.ability.effect;

import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Migrates a dying mob's abilities onto nearby Infused Mob allies — one per
 * heir, nearest first, sliding past duplicates. Mirrors {@link SplitEffect}:
 * the Ability pool registers, this module hits the Infusion seam.
 */
public final class BequestEffect {

    /** Fixed heir-search radius — decoupled from Combust tuning on purpose. */
    private static final double HEIR_RADIUS = 8.0;

    private BequestEffect() {}

    /**
     * Passes the dead mob's own abilities (minus Bequest itself) to living
     * Tiered allies in range. Split copies are never eligible either way —
     * they hold no Tier — and either-side excess fizzles. Does nothing
     * off-server or when there is nothing to migrate.
     *
     * @param mob the dying mob
     */
    public static void apply(Mob mob) {
        if (!(mob.level() instanceof ServerLevel)) return;
        List<String> deadIds = MobTierManager.abilityIdsOf(mob);
        if (deadIds.isEmpty()) return;

        List<Mob> heirs = new ArrayList<>();
        for (var entity : mob.level().getEntities(mob, mob.getBoundingBox().inflate(HEIR_RADIUS))) {
            if (entity instanceof Mob ally && ally != mob && ally.isAlive()
                    && ally.distanceTo(mob) <= HEIR_RADIUS
                    && MobTierManager.getTier(ally) != null) {
                heirs.add(ally);
            }
        }
        if (heirs.isEmpty()) return;
        heirs.sort(Comparator.comparingDouble(a -> a.distanceTo(mob)));

        List<Set<String>> heirIds = new ArrayList<>(heirs.size());
        for (Mob heir : heirs) {
            heirIds.add(Set.copyOf(MobTierManager.abilityIdsOf(heir)));
        }
        int poolCap = AbilityRegistry.getAllAbilityIds().size();
        for (MobTierManager.Grant grant : MobTierManager.planBequest(deadIds, heirIds, poolCap)) {
            MobTierManager.grantAbility(heirs.get(grant.heirIndex()), grant.abilityId(), poolCap);
        }
    }
}
