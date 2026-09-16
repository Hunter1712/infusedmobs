package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.config.ModConfig;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * Rolls Tiers for hostile mobs and restores persisted rolls on reload.
 * <p>
 * Each hostile mob can be assigned a Tier on spawn, which grants it
 * a random subset of abilities and stat multipliers. The full roll
 * (Tier, abilities, or split-copy status) is persisted via
 * {@link TierSavedData} and restored exactly on world/chunk reloads.
 * <p>
 * Gating decisions live in {@link InfusionGate} and in-memory tracking plus
 * nametag presentation in {@link InfusedTracker}; this module coordinates
 * rolls, persistence and health between them. Tracking is cleaned up when
 * the mob dies.
 */
public final class MobTierManager {

    private static final float SPLIT_HEALTH_FRACTION = 0.6f;

    private MobTierManager() {}

    // ========================================
    // Tier assignment
    // ========================================

    /**
     * Rolls for a tier and assigns it to the mob. If a tier is assigned,
     * random abilities are selected, health is multiplied, and the mob is
     * fully healed to its new max.
     * <p>
     * The result (tier + ability ids, or "nothing") is persisted via
     * {@link TierSavedData}: the same mob (same UUID) restores the exact
     * same result on world or chunk reload — never re-rolled.
     * <p>
     * Rupture split copies hold a persisted {@link Rolled.Split}
     * entry, so they are skipped here and never become tiered mobs.
     * <p>
     * Worlds where the mod is inactive are skipped entirely — no tier,
     * no abilities, no nametag.
     */
    public static void assignTier(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        if (mob.getType().getCategory() != MobCategory.MONSTER) return;
        if (InfusionGate.status(serverLevel) != InfusionGate.Status.ACTIVE) return;
        if (InfusedTracker.find(mob.getUUID()) != null) return;  // Already assigned — prevents stacking

        TierSavedData savedData = TierSavedData.get(serverLevel);
        UUID uuid = mob.getUUID();

        // If this UUID already rolled in a previous session, restore that result exactly
        Rolled rolled = savedData.getRolled(uuid);
        if (rolled != null) {
            restoreRolled(mob, uuid, rolled);
            return;
        }

        ModConfig.Instance cfg = ModConfig.get();

        for (MobTier tier : MobTier.values()) {
            ModConfig.TierConfig tc = cfg.forTier(tier);
            if (!(mob.getRandom().nextDouble() < tc.spawnChance())) continue;

            List<Ability> abilities = AbilityRegistry.getRandomAbilities(tc.abilityCount());
            InfusedTracker.track(uuid, InfusedMob.tiered(tier, abilities));
            savedData.setRolled(uuid, new Rolled.Tiered(tier, idsOf(abilities)));

            applyHealthMultiplier(mob, tc);
            InfusedTracker.setTierNametag(mob, tier, abilities);
            return;
        }

        // Rolled nothing — persist so we never roll again for this UUID
        savedData.setRolled(uuid, new Rolled.Nothing());
    }

    /**
     * Assigns a specific tier and ability list to a mob, bypassing random rolls.
     * Used by the summon command where the player chooses the tier and abilities.
     * <p>
     * Skips the MONSTER category restriction so any summoned mob can receive a tier.
     * Also skips the "already assigned" check since this is for fresh command-spawned mobs.
     * <p>
     * The assignment is persisted via {@link TierSavedData} so the exact summon
     * (tier + abilities) is restored on chunk reload or world restart — without
     * this the mob would be re-rolled randomly on load.
     * <p>
     * Returns {@code false} (without modifying the mob) if the mod is
     * inactive in the mob's level. The summon command checks this beforehand
     * via {@link InfusionGate#status(ServerLevel)} and shows a clear failure message,
     * but this guard protects against any future callers.
     */
    public static boolean assignSpecificTier(Mob mob, MobTier tier, List<Ability> abilities) {
        ServerLevel serverLevel = mob.level() instanceof ServerLevel sl ? sl : null;
        if (serverLevel != null && InfusionGate.status(serverLevel) != InfusionGate.Status.ACTIVE) {
            return false;
        }
        UUID uuid = mob.getUUID();
        InfusedTracker.track(uuid, InfusedMob.tiered(tier, abilities));

        ModConfig.TierConfig tc = ModConfig.get().forTier(tier);
        applyHealthMultiplier(mob, tc);
        InfusedTracker.setTierNametag(mob, tier, abilities);

        if (serverLevel != null) {
            TierSavedData.get(serverLevel)
                    .setRolled(uuid, new Rolled.Tiered(tier, idsOf(abilities)));
        }
        return true;
    }

    /** Restores the persisted roll exactly — tier, abilities, or split-copy status. */
    private static void restoreRolled(Mob mob, UUID uuid, Rolled rolled) {
        if (rolled instanceof Rolled.Tiered t) {
            restoreTiered(mob, uuid, t);
        } else if (rolled instanceof Rolled.Split s) {
            restoreSplit(mob, uuid, s);
        } else if (rolled instanceof Rolled.Nothing) {
            // Rolled nothing — leave the mob vanilla.
        }
    }

    private static void restoreTiered(Mob mob, UUID uuid, Rolled.Tiered t) {
        List<Ability> abilities = resolveAbilities(t.abilityIds());
        InfusedTracker.track(uuid, InfusedMob.tiered(t.tier(), abilities));
        ModConfig.TierConfig tc = ModConfig.get().forTier(t.tier());
        applyHealthMultiplier(mob, tc);
        InfusedTracker.setTierNametag(mob, t.tier(), abilities);
    }

    private static void restoreSplit(Mob mob, UUID uuid, Rolled.Split s) {
        List<Ability> abilities = resolveAbilities(s.abilityIds());
        InfusedTracker.track(uuid, InfusedMob.split(abilities));
        // Re-apply the Cinder HP boost — otherwise a chunk reload
        // silently deflates the copy back to vanilla max health.
        applyCinderStats(mob);
        InfusedTracker.setSplitCopyNametag(mob, abilities);
    }

    private static List<Ability> resolveAbilities(List<String> ids) {
        return ids.stream()
                .map(AbilityRegistry::getById)
                .filter(Objects::nonNull)
                .toList();
    }

    private static List<String> idsOf(List<Ability> abilities) {
        return abilities.stream().map(Ability::id).toList();
    }

    private static void applyHealthMultiplier(Mob mob, ModConfig.TierConfig tc) {
        var attribute = mob.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        attribute.setBaseValue(attribute.getBaseValue() * tc.healthMultiplier());
        mob.setHealth(mob.getMaxHealth());
    }

    // ========================================
    // Rupture copy handling
    // ========================================

    /**
     * Applies full Cinder-tier stats and abilities to a Rupture split copy:
     * <ul>
     *   <li>Cinder health multiplier (1.5× base)</li>
     *   <li>60% of boosted max health</li>
     *   <li>1 random ability — Rupture itself excluded at the draw, so a copy
     *       can never split further; every other ability (including Combust)
     *       is still possible</li>
     *   <li>Greyscale nametag</li>
     * </ul>
     * The copy's status is persisted as {@link Rolled.Split}
     * BEFORE it enters the world, so the spawn handler skips it and chunk
     * reloads restore it as a copy — it can never be re-rolled into a
     * tiered mob.
     */
    public static void applyCinderTierToSplitCopy(Mob copy) {
        applyCinderStats(copy);

        List<Ability> abilities = AbilityRegistry.getRandomAbilities(1, "rupture");

        InfusedTracker.track(copy.getUUID(), InfusedMob.split(abilities));
        if (copy.level() instanceof ServerLevel serverLevel) {
            TierSavedData.get(serverLevel)
                    .setRolled(copy.getUUID(), new Rolled.Split(idsOf(abilities)));
        }
        InfusedTracker.setSplitCopyNametag(copy, abilities);
    }

    /** Applies the Cinder health multiplier and sets the copy to 60% of its boosted max. */
    private static void applyCinderStats(Mob mob) {
        var attribute = mob.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(attribute.getBaseValue()
                    * ModConfig.get().forTier(MobTier.CINDER).healthMultiplier());
            mob.setHealth(mob.getMaxHealth() * SPLIT_HEALTH_FRACTION);
        }
    }

    // ========================================
    // Cleanup
    // ========================================

    /** Removes all tracking for this mob (called on death or despawn). */
    public static void removeMob(Mob mob) {
        UUID uuid = mob.getUUID();
        InfusedTracker.untrack(uuid);

        // Clean up persistent state so it doesn't grow unboundedly
        if (mob.level() instanceof ServerLevel serverLevel) {
            TierSavedData.get(serverLevel).remove(uuid);
        }
    }
}
