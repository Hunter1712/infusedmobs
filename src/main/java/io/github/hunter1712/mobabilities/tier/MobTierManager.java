package io.github.hunter1712.mobabilities.tier;

import io.github.hunter1712.mobabilities.ability.Ability;
import io.github.hunter1712.mobabilities.ability.AbilityRegistry;
import io.github.hunter1712.mobabilities.ability.TriggerType;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.*;

/**
 * Manages per-mob tier assignments and ability tracking.
 * <p>
 * Each hostile mob can be assigned a tier on spawn, which grants it
 * a random subset of abilities and stat multipliers.
 * Tracking is cleaned up when the mob dies.
 */
public final class MobTierManager {

    private static final Map<UUID, MobTier> TIERS = new HashMap<>();
    private static final Map<UUID, List<Ability>> ABILITIES = new HashMap<>();

    /** UUIDs of mobs spawned by the Split ability — they get no tier. */
    private static final Set<UUID> SPLIT_COPIES = new HashSet<>();

    private MobTierManager() {}

    // ========================================
    // Tier assignment
    // ========================================

    /**
     * Rolls for a tier and assigns it to the mob. If a tier is assigned,
     * random abilities are selected, health is multiplied, and the mob is
     * fully healed to its new max. Split copies are skipped entirely.
     */
    public static void assignTier(Mob mob) {
        if (mob.level() instanceof net.minecraft.server.level.ServerLevel) {
            if (SPLIT_COPIES.contains(mob.getUUID())) return;
        } else {
            return;
        }

        for (MobTier tier : MobTier.values()) {
            if (mob.getRandom().nextDouble() < tier.spawnChance()) {
                TIERS.put(mob.getUUID(), tier);

                int range = tier.maxAbilities() - tier.minAbilities() + 1;
                int count = tier.minAbilities() + mob.getRandom().nextInt(range);
                List<Ability> abilities = AbilityRegistry.getRandomAbilities(count);
                ABILITIES.put(mob.getUUID(), abilities);

                // Apply health multiplier
                var attribute = mob.getAttribute(Attributes.MAX_HEALTH);
                if (attribute != null) {
                    attribute.setBaseValue(attribute.getBaseValue() * tier.healthMultiplier());
                    mob.setHealth(mob.getMaxHealth());
                }
                return;
            }
        }
    }

    // ========================================
    // Queries
    // ========================================

    /** Returns the tier assigned to this mob, or null. */
    public static MobTier getTier(Mob mob) {
        return TIERS.get(mob.getUUID());
    }

    /** Returns all abilities assigned to this mob (empty list if none). */
    public static List<Ability> getAbilities(Mob mob) {
        return ABILITIES.getOrDefault(mob.getUUID(), List.of());
    }

    /**
     * Returns abilities assigned to this mob matching the given trigger type.
     */
    public static List<Ability> getAbilitiesByTrigger(Mob mob, TriggerType trigger) {
        List<Ability> abilities = ABILITIES.get(mob.getUUID());
        if (abilities == null) return List.of();
        return abilities.stream()
                .filter(a -> a.trigger() == trigger)
                .toList();
    }

    // ========================================
    // Cleanup
    // ========================================

    /** Removes all tracking for this mob (called on death). */
    public static void removeMob(Mob mob) {
        TIERS.remove(mob.getUUID());
        ABILITIES.remove(mob.getUUID());
        SPLIT_COPIES.remove(mob.getUUID());
    }

    /** Marks a mob as a split copy so it won't receive tier assignment. */
    public static void markSplitCopy(UUID uuid) {
        SPLIT_COPIES.add(uuid);
    }
}
