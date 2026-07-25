package io.github.hunter1712.mobabilities.tier;

import io.github.hunter1712.mobabilities.ability.Ability;
import io.github.hunter1712.mobabilities.ability.AbilityRegistry;
import io.github.hunter1712.mobabilities.ability.TriggerType;

import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
        if (!(mob.level() instanceof net.minecraft.server.level.ServerLevel)) return;
        if (mob.getType().getCategory() != MobCategory.MONSTER) return;
        if (SPLIT_COPIES.contains(mob.getUUID())) return;

        for (MobTier tier : MobTier.values()) {
            if (!(mob.getRandom().nextDouble() < tier.spawnChance())) continue;

            TIERS.put(mob.getUUID(), tier);

            int count = rollAbilityCount(tier, mob);
            List<Ability> abilities = AbilityRegistry.getRandomAbilities(count);
            ABILITIES.put(mob.getUUID(), abilities);

            applyHealthMultiplier(mob, tier);
            setMobNameTag(mob, tier, abilities);
            return;
        }
    }

    private static int rollAbilityCount(MobTier tier, Mob mob) {
        int range = tier.maxAbilities() - tier.minAbilities() + 1;
        return tier.minAbilities() + mob.getRandom().nextInt(range);
    }

    private static void applyHealthMultiplier(Mob mob, MobTier tier) {
        var attribute = mob.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        attribute.setBaseValue(attribute.getBaseValue() * tier.healthMultiplier());
        mob.setHealth(mob.getMaxHealth());
    }

    // ========================================
    // Queries
    // ========================================

    /** Returns the tier assigned to this mob, or null. */
    public static MobTier getTier(Mob mob) {
        return TIERS.get(mob.getUUID());
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

    /** Returns all abilities assigned to this mob (empty list if none). */
    public static List<Ability> getAllAbilities(Mob mob) {
        return ABILITIES.getOrDefault(mob.getUUID(), List.of());
    }

    /** Marks a mob as a split copy so it won't receive tier assignment. */
    public static void markSplitCopy(UUID uuid) {
        SPLIT_COPIES.add(uuid);
    }

    // ========================================
    // Nametag helpers
    // ========================================

    private static void setMobNameTag(Mob mob, MobTier tier, List<Ability> abilities) {
        // Tier colour: green → yellow → red
        String colour = switch (tier) {
            case EMBER -> "§a";
            case SURGE -> "§e";
            case TEMPEST -> "§c";
        };

        String abilityList = String.join("§7, ", abilities.stream().map(Ability::name).toList());
        mob.setCustomName(Component.literal(colour + abilityList + " §f" + mob.getName().getString()));
        mob.setCustomNameVisible(true);
    }
}
