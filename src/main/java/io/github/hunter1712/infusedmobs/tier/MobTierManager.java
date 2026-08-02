package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.config.ModConfig;
import io.github.hunter1712.infusedmobs.gamerules.ModGameRules;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Manages per-mob tier assignments and ability tracking.
 * <p>
 * Each hostile mob can be assigned a tier on spawn, which grants it
 * a random subset of abilities and stat multipliers. The full roll
 * (tier, abilities, or split-copy status) is persisted via
 * {@link TierSavedData} and restored exactly on world/chunk reloads.
 * Tracking is cleaned up when the mob dies.
 */
public final class MobTierManager {

    private static final Map<UUID, InfusedMob> INFUSED = new HashMap<>();

    private static final float SPLIT_HEALTH_FRACTION = 0.6f;

    private MobTierManager() {}

    /**
     * Why the mod is (or isn't) active in a level — used by summon to give
     * precise feedback and by assignment to gate infusion.
     */
    public enum InfuseStatus {
        /** Mod active: not blacklisted and the {@code infusedmobs:enabled} rule is on. */
        ACTIVE,
        /** The level's dimension is on the config blacklist. */
        WORLD_BLACKLISTED,
        /** The {@code infusedmobs:enabled} gamerule is off. */
        RULE_DISABLED
    }

    // ========================================
    // Infusion gating
    // ========================================

    /** Status of the mod in the given level. */
    public static InfuseStatus canInfuse(ServerLevel level) {
        return canInfuse(
                ModConfig.get().isWorldBlacklisted(level.dimension().identifier().toString()),
                ModGameRules.readRule(level.getServer(), ModGameRules.ENABLED));
    }

    /** Pure decision helper — unit-testable without Minecraft bootstrap. */
    static InfuseStatus canInfuse(boolean worldBlacklisted, Boolean storedEnabled) {
        if (worldBlacklisted) return InfuseStatus.WORLD_BLACKLISTED;
        if (!ModGameRules.resolveRule(storedEnabled, ModGameRules.ENABLED.defaultValue())) {
            return InfuseStatus.RULE_DISABLED;
        }
        return InfuseStatus.ACTIVE;
    }

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
     * Rupture split copies hold a persisted {@link TierSavedData.Rolled.Split}
     * entry, so they are skipped here and never become tiered mobs.
     * <p>
     * Worlds where the mod is inactive are skipped entirely — no tier,
     * no abilities, no nametag.
     */
    public static void assignTier(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        if (mob.getType().getCategory() != MobCategory.MONSTER) return;
        if (canInfuse(serverLevel) != InfuseStatus.ACTIVE) return;
        if (INFUSED.containsKey(mob.getUUID())) return;  // Already assigned — prevents stacking

        TierSavedData savedData = serverLevel.getDataStorage().computeIfAbsent(TierSavedData.TYPE);
        UUID uuid = mob.getUUID();

        // If this UUID already rolled in a previous session, restore that result exactly
        TierSavedData.Rolled rolled = savedData.getRolled(uuid);
        if (rolled != null) {
            restoreRolled(mob, uuid, rolled);
            return;
        }

        ModConfig.Instance cfg = ModConfig.get();

        for (MobTier tier : MobTier.values()) {
            ModConfig.TierConfig tc = cfg.forTier(tier);
            if (!(mob.getRandom().nextDouble() < tc.spawnChance())) continue;

            List<Ability> abilities = AbilityRegistry.getRandomAbilities(tc.abilityCount());
            INFUSED.put(uuid, InfusedMob.tiered(tier, abilities));
            savedData.setRolled(uuid, new TierSavedData.Rolled.Tiered(tier, idsOf(abilities)));

            applyHealthMultiplier(mob, tc);
            setTierNametag(mob, tier, abilities);
            return;
        }

        // Rolled nothing — persist so we never roll again for this UUID
        savedData.setRolled(uuid, new TierSavedData.Rolled.Nothing());
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
     * via {@link #canInfuse(ServerLevel)} and shows a clear failure message,
     * but this guard protects against any future callers.
     */
    public static boolean assignSpecificTier(Mob mob, MobTier tier, List<Ability> abilities) {
        ServerLevel serverLevel = mob.level() instanceof ServerLevel sl ? sl : null;
        if (serverLevel != null && canInfuse(serverLevel) != InfuseStatus.ACTIVE) {
            return false;
        }
        UUID uuid = mob.getUUID();
        INFUSED.put(uuid, InfusedMob.tiered(tier, abilities));

        ModConfig.TierConfig tc = ModConfig.get().forTier(tier);
        applyHealthMultiplier(mob, tc);
        setTierNametag(mob, tier, abilities);

        if (serverLevel != null) {
            serverLevel.getDataStorage().computeIfAbsent(TierSavedData.TYPE)
                    .setRolled(uuid, new TierSavedData.Rolled.Tiered(tier, idsOf(abilities)));
        }
        return true;
    }

    /** Restores the persisted roll exactly — tier, abilities, or split-copy status. */
    private static void restoreRolled(Mob mob, UUID uuid, TierSavedData.Rolled rolled) {
        switch (rolled) {
            case TierSavedData.Rolled.Tiered t -> {
                List<Ability> abilities = resolveAbilities(t.abilityIds());
                INFUSED.put(uuid, InfusedMob.tiered(t.tier(), abilities));

                ModConfig.TierConfig tc = ModConfig.get().forTier(t.tier());
                applyHealthMultiplier(mob, tc);
                setTierNametag(mob, t.tier(), abilities);
            }
            case TierSavedData.Rolled.Split s -> {
                List<Ability> abilities = resolveAbilities(s.abilityIds());
                INFUSED.put(uuid, InfusedMob.split(abilities));
                // Re-apply the Cinder HP boost — otherwise a chunk reload
                // silently deflates the copy back to vanilla max health.
                applyCinderStats(mob);
                setSplitCopyNametag(mob, abilities);
            }
            case TierSavedData.Rolled.Nothing ignored -> {
                // Rolled nothing — leave the mob vanilla.
            }
        }
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
     * The copy's status is persisted as {@link TierSavedData.Rolled.Split}
     * BEFORE it enters the world, so the spawn handler skips it and chunk
     * reloads restore it as a copy — it can never be re-rolled into a
     * tiered mob.
     */
    public static void applyCinderTierToSplitCopy(Mob copy) {
        applyCinderStats(copy);

        List<Ability> abilities = AbilityRegistry.getRandomAbilities(1, "rupture");

        INFUSED.put(copy.getUUID(), InfusedMob.split(abilities));
        if (copy.level() instanceof ServerLevel serverLevel) {
            serverLevel.getDataStorage().computeIfAbsent(TierSavedData.TYPE)
                    .setRolled(copy.getUUID(), new TierSavedData.Rolled.Split(idsOf(abilities)));
        }
        setSplitCopyNametag(copy, abilities);
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
    // Queries
    // ========================================

    /** Returns the tier assigned to this mob, or null (split copy / untracked). */
    public static MobTier getTier(Mob mob) {
        InfusedMob infused = INFUSED.get(mob.getUUID());
        return switch (infused) {
            case InfusedMob.TieredMob t -> t.tier();
            case InfusedMob.SplitCopyMob s -> null;
            case null -> null;
        };
    }

    /** Returns abilities assigned to this mob matching the given trigger type. */
    public static List<Ability> getAbilitiesByTrigger(Mob mob, TriggerType trigger) {
        InfusedMob infused = INFUSED.get(mob.getUUID());
        return infused == null ? List.of() : infused.forTrigger(trigger);
    }

    /** Returns all abilities assigned to this mob (empty list if none). */
    public static List<Ability> getAllAbilities(Mob mob) {
        InfusedMob infused = INFUSED.get(mob.getUUID());
        return infused == null ? List.of() : infused.abilities();
    }

    /** Returns true if this mob has an ability with the given id. */
    public static boolean hasAbility(Mob mob, String id) {
        InfusedMob infused = INFUSED.get(mob.getUUID());
        if (infused == null) return false;
        for (Ability ability : infused.abilities()) {
            if (ability.id().equals(id)) return true;
        }
        return false;
    }

    /**
     * Returns the UUIDs of tracked mobs that have at least one TICK ability.
     * Used by {@link io.github.hunter1712.infusedmobs.ability.trigger.MobTickTrigger}
     * to iterate only the mobs it can actually affect.
     * <p>
     * Returns a snapshot so concurrent removal during iteration (mob death
     * mid-scan) cannot throw a {@code ConcurrentModificationException}.
     */
    public static Set<UUID> getTickMobUUIDs() {
        return INFUSED.entrySet().stream()
                .filter(e -> !e.getValue().forTrigger(TriggerType.TICK).isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
    }

    // ========================================
    // Cleanup
    // ========================================

    /** Removes all tracking for this mob (called on death or despawn). */
    public static void removeMob(Mob mob) {
        UUID uuid = mob.getUUID();
        INFUSED.remove(uuid);

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
        for (Map.Entry<UUID, InfusedMob> entry : INFUSED.entrySet()) {
            Mob mob = findMob(server, entry.getKey());
            if (mob == null) continue;
            InfusedMob infused = entry.getValue();

            if (show) {
                if (infused.abilities().isEmpty()) continue;
                switch (infused) {
                    case InfusedMob.TieredMob t -> setTierNametag(mob, t.tier(), t.abilities());
                    case InfusedMob.SplitCopyMob s -> setSplitCopyNametag(mob, s.abilities());
                }
            } else {
                mob.setCustomName(null);
                mob.setCustomNameVisible(false);
            }
        }
    }
}
