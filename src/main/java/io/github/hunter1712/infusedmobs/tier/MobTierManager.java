package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.config.ModConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
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

    /** UUIDs of mobs spawned by the Rupture ability — they get no tier. */
    private static final Set<UUID> SPLIT_COPIES = new HashSet<>();

    private static final float SPLIT_HEALTH_FRACTION = 0.6f;

    private MobTierManager() {}

    // ========================================
    // Tier assignment
    // ========================================

    /**
     * Rolls for a tier and assigns it to the mob. If a tier is assigned,
     * random abilities are selected, health is multiplied, and the mob is
     * fully healed to its new max. Rupture copies are skipped entirely.
     * <p>
     * Results are persisted to disk via {@link TierSavedData} so that
     * the same mob (same UUID) gets the same result on world reload.
     * <p>
     * Worlds on the config blacklist are skipped entirely — no tier,
     * no abilities, no nametag. The mod is effectively disabled there.
     */
    public static void assignTier(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        if (mob.getType().getCategory() != MobCategory.MONSTER) return;
        if (isWorldBlacklisted(serverLevel)) return;
        if (SPLIT_COPIES.contains(mob.getUUID())) return;
        if (TIERS.containsKey(mob.getUUID())) return;  // Already assigned — prevents stacking on world reload

        TierSavedData savedData = serverLevel.getDataStorage().computeIfAbsent(TierSavedData.TYPE);
        UUID uuid = mob.getUUID();

        // If this UUID already rolled in a previous session, restore that result
        if (savedData.hasRolled(uuid)) {
            MobTier existingTier = savedData.getTier(uuid);
            if (existingTier != null) {
                restoreTier(mob, uuid, existingTier);
            }
            return;
        }

        ModConfig.Instance cfg = ModConfig.get();

        for (MobTier tier : MobTier.values()) {
            ModConfig.TierConfig tc = cfg.forTier(tier);
            if (!(mob.getRandom().nextDouble() < tc.spawnChance())) continue;

            TIERS.put(uuid, tier);
            savedData.setTier(uuid, tier);

            List<Ability> abilities = AbilityRegistry.getRandomAbilities(tc.abilityCount());
            ABILITIES.put(uuid, abilities);

            applyHealthMultiplier(mob, tc);
            setTierNametag(mob, tier, abilities);
            return;
        }

        // Rolled nothing — persist so we never roll again for this UUID
        savedData.markRolledNothing(uuid);
    }

    /**
     * Assigns a specific tier and ability list to a mob, bypassing random rolls.
     * Used by the summon command where the player chooses the tier and abilities.
     * <p>
     * Skips the MONSTER category restriction so any summoned mob can receive a tier.
     * Also skips the "already assigned" check since this is for fresh command-spawned mobs.
     * <p>
     * Returns {@code false} (without modifying the mob) if the mob's level is on
     * the world blacklist — the summon command checks this beforehand and shows
     * a clear failure message, but this guard protects against any future callers.
     */
    public static boolean assignSpecificTier(Mob mob, MobTier tier, List<Ability> abilities) {
        if (mob.level() instanceof ServerLevel serverLevel && isWorldBlacklisted(serverLevel)) {
            return false;
        }
        UUID uuid = mob.getUUID();
        TIERS.put(uuid, tier);
        ABILITIES.put(uuid, abilities);

        ModConfig.TierConfig tc = ModConfig.get().forTier(tier);
        applyHealthMultiplier(mob, tc);
        setTierNametag(mob, tier, abilities);
        return true;
    }

    /**
     * Re-applies tier effects from persistent state when a mob loads
     * into the world after a chunk reload.
     */
    private static void restoreTier(Mob mob, UUID uuid, MobTier tier) {
        TIERS.put(uuid, tier);

        ModConfig.TierConfig tc = ModConfig.get().forTier(tier);
        List<Ability> abilities = AbilityRegistry.getRandomAbilities(tc.abilityCount());
        ABILITIES.put(uuid, abilities);

        applyHealthMultiplier(mob, tc);
        setTierNametag(mob, tier, abilities);
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

    /** Marks a mob as a Rupture copy so it won't receive tier assignment. */
    public static void markSplitCopy(UUID uuid) {
        SPLIT_COPIES.add(uuid);
    }

    /**
     * Applies full Cinder-tier stats and abilities to a Rupture split copy:
     * <ul>
     *   <li>Cinder health multiplier (1.5× base)</li>
     *   <li>60% of boosted max health</li>
     *   <li>1 random non-DEATH ability (prevents infinite Rupture recursion)</li>
     *   <li>Greyscale nametag</li>
     * </ul>
     */
    public static void applyCinderTierToSplitCopy(Mob copy) {
        ModConfig.TierConfig cinder = ModConfig.get().forTier(MobTier.CINDER);

        var attribute = copy.getAttribute(Attributes.MAX_HEALTH);
        if (attribute != null) {
            attribute.setBaseValue(attribute.getBaseValue() * cinder.healthMultiplier());
            copy.setHealth(copy.getMaxHealth() * SPLIT_HEALTH_FRACTION);
        }

        // Draw 1 non-DEATH ability from the unified pool.
        // Retry if DEATH is drawn (prevents empty-ability split copies).
        List<Ability> abilities = drawNonDeathAbility();
        if (abilities.isEmpty()) {
            abilities = drawNonDeathAbility(); // one more try — 0.3% chance both are DEATH
        }

        ABILITIES.put(copy.getUUID(), abilities);
        setSplitCopyNametag(copy, abilities);
    }

    /** Draws 1 ability, filtering out DEATH trigger types. */
    private static List<Ability> drawNonDeathAbility() {
        return AbilityRegistry.getRandomAbilities(1)
                .stream()
                .filter(a -> a.trigger() != TriggerType.DEATH)
                .toList();
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

    /** Returns true if this mob has an ability with the given id. */
    public static boolean hasAbility(Mob mob, String id) {
        List<Ability> abilities = ABILITIES.get(mob.getUUID());
        if (abilities == null) return false;
        for (Ability ability : abilities) {
            if (ability.id().equals(id)) return true;
        }
        return false;
    }

    /**
     * Returns all tracked mob UUIDs that have abilities assigned.
     * Used by {@link io.github.hunter1712.infusedmobs.ability.trigger.MobTickTrigger}
     * to iterate only mobs that actually have TICK abilities.
     */
    public static Set<UUID> getTrackedMobUUIDs() {
        return ABILITIES.keySet();
    }

    /**
     * Returns true if the given server level is on the config world blacklist.
     * Uses the level's dimension resource location (e.g. "minecraft:overworld")
     * matched against {@link ModConfig.Instance#isWorldBlacklisted(String)}.
     */
    public static boolean isWorldBlacklisted(ServerLevel level) {
        return ModConfig.get().isWorldBlacklisted(level.dimension().identifier().toString());
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

        // Clean up persistent state so it doesn't grow unboundedly
        if (mob.level() instanceof ServerLevel serverLevel) {
            serverLevel.getDataStorage().computeIfAbsent(TierSavedData.TYPE).remove(uuid);
        }
    }

    // ========================================
    // Nametag helpers
    // ========================================

    private static void setTierNametag(Mob mob, MobTier tier, List<Ability> abilities) {
        setNametag(mob, tier.colourCode(), abilities);
    }

    private static void setSplitCopyNametag(Mob mob, List<Ability> abilities) {
        setNametag(mob, "§7", abilities);
    }

    private static void setNametag(Mob mob, String colour, List<Ability> abilities) {
        if (!ModConfig.get().showNametags()) return;
        String abilityList = String.join("§7, ", abilities.stream().map(Ability::name).toList());
        // Use the entity type name (e.g. "Parched") rather than getName(),
        // which would return any previously-set custom name and cause duplication.
        String entityName = mob.getType().getDescription().getString();
        mob.setCustomName(Component.literal(colour + abilityList + " §f" + entityName));
        mob.setCustomNameVisible(true);
    }

    /**
     * Looks up a mob by UUID across all loaded server levels.
     * Returns null if the mob is not found or dead.
     */
    public static Mob findMob(MinecraftServer server, UUID uuid) {
        for (ServerLevel level : server.getAllLevels()) {
            if (level.getEntity(uuid) instanceof Mob mob && mob.isAlive()) {
                return mob;
            }
        }
        return null;
    }

    /**
     * Applies or removes nametags for all tracked mobs based on the
     * current {@link ModConfig.Instance#showNametags()} setting.
     * Called when the toggle changes via command.
     */
    public static void refreshNametags(MinecraftServer server) {
        boolean show = ModConfig.get().showNametags();
        for (UUID uuid : ABILITIES.keySet()) {
            Mob mob = findMob(server, uuid);
            if (mob == null) continue;

            if (show) {
                List<Ability> abilities = ABILITIES.get(uuid);
                if (abilities == null || abilities.isEmpty()) continue;

                if (SPLIT_COPIES.contains(uuid)) {
                    setSplitCopyNametag(mob, abilities);
                } else {
                    MobTier tier = TIERS.get(uuid);
                    if (tier != null) {
                        setTierNametag(mob, tier, abilities);
                    }
                }
            } else {
                mob.setCustomName(null);
                mob.setCustomNameVisible(false);
            }
        }
    }
}
