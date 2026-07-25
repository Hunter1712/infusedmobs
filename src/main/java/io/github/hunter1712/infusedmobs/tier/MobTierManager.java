package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.config.ModConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
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

    private static final float SPLIT_HEALTH_FRACTION = 0.6f;

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
        if (!(mob.level() instanceof ServerLevel)) return;
        if (mob.getType().getCategory() != MobCategory.MONSTER) return;
        if (SPLIT_COPIES.contains(mob.getUUID())) return;

        ModConfig.Instance cfg = ModConfig.get();

        for (MobTier tier : MobTier.values()) {
            ModConfig.TierConfig tc = cfg.forTier(tier);
            if (!(mob.getRandom().nextDouble() < tc.spawnChance())) continue;

            TIERS.put(mob.getUUID(), tier);

            List<Ability> abilities = AbilityRegistry.getRandomAbilities(
                    tc.hurtAbilities(), tc.tickAbilities(), tc.deathAbilities());
            ABILITIES.put(mob.getUUID(), abilities);

            applyHealthMultiplier(mob, tc);
            setTierNametag(mob, tier, abilities);
            return;
        }
    }

    private static void applyHealthMultiplier(Mob mob, ModConfig.TierConfig tc) {
        var attribute = mob.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        attribute.setBaseValue(attribute.getBaseValue() * tc.healthMultiplier());
        mob.setHealth(mob.getMaxHealth());
    }

    // ========================================
    // Split copy handling
    // ========================================

    /** Marks a mob as a split copy so it won't receive tier assignment. */
    public static void markSplitCopy(UUID uuid) {
        SPLIT_COPIES.add(uuid);
    }

    /**
     * Applies full Ember-tier stats and abilities to a fission split copy:
     * <ul>
     *   <li>Ember health multiplier (3.0× base)</li>
     *   <li>60% of boosted max health</li>
     *   <li>1 random HURT ability (no Fission to prevent infinite recursion)</li>
     *   <li>Greyscale nametag</li>
     * </ul>
     */
    public static void applyEmberTierToSplitCopy(Mob copy) {
        ModConfig.TierConfig ember = ModConfig.get().forTier(MobTier.EMBER);

        var attribute = copy.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(attribute.getBaseValue() * ember.healthMultiplier());
            copy.setHealth(copy.getMaxHealth() * SPLIT_HEALTH_FRACTION);
        }

        List<Ability> abilities = AbilityRegistry.getRandomAbilities(
                ember.hurtAbilities(), ember.tickAbilities(), ember.deathAbilities())
                .stream()
                .filter(a -> a.trigger() != TriggerType.DEATH)
                .toList();

        ABILITIES.put(copy.getUUID(), abilities);
        setSplitCopyNametag(copy, abilities);
    }

    // ========================================
    // Queries
    // ========================================

    /** Returns the tier assigned to this mob, or null. */
    public static MobTier getTier(Mob mob) {
        return TIERS.get(mob.getUUID());
    }

    /** Returns abilities assigned to this mob matching the given trigger type. */
    public static List<Ability> getAbilitiesByTrigger(Mob mob, TriggerType trigger) {
        List<Ability> abilities = ABILITIES.get(mob.getUUID());
        if (abilities == null) return List.of();
        return abilities.stream()
                .filter(a -> a.trigger() == trigger)
                .toList();
    }

    /** Returns all abilities assigned to this mob (empty list if none). */
    public static List<Ability> getAllAbilities(Mob mob) {
        return ABILITIES.getOrDefault(mob.getUUID(), List.of());
    }

    /**
     * Returns all tracked mob UUIDs that have abilities assigned.
     * Used by {@link io.github.hunter1712.infusedmobs.ability.trigger.MobTickTrigger}
     * to iterate only mobs that actually have TICK abilities.
     */
    public static Set<UUID> getTrackedMobUUIDs() {
        return ABILITIES.keySet();
    }

    // ========================================
    // Cleanup
    // ========================================

    /** Removes all tracking for this mob (called on death). */
    public static void removeMob(Mob mob) {
        UUID uuid = mob.getUUID();
        TIERS.remove(uuid);
        ABILITIES.remove(uuid);
        SPLIT_COPIES.remove(uuid);
    }

    // ========================================
    // Nametag helpers
    // ========================================

    private static void setTierNametag(Mob mob, MobTier tier, List<Ability> abilities) {
        String colour = switch (tier) {
            case EMBER -> "§a";
            case SURGE -> "§e";
            case TEMPEST -> "§c";
        };
        setNametag(mob, colour, abilities);
    }

    private static void setSplitCopyNametag(Mob mob, List<Ability> abilities) {
        setNametag(mob, "§7", abilities);
    }

    private static void setNametag(Mob mob, String colour, List<Ability> abilities) {
        String abilityList = String.join("§7, ", abilities.stream().map(Ability::name).toList());
        mob.setCustomName(Component.literal(colour + abilityList + " §f" + mob.getName().getString()));
        mob.setCustomNameVisible(true);
    }
}
