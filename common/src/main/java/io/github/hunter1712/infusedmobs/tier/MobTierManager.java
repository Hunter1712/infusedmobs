package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.config.ModConfig;
import io.github.hunter1712.infusedmobs.platform.Platform;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.Attributes;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Rolls Tiers for hostile mobs and restores persisted rolls on reload.
 * <p>
 * Each hostile mob can be assigned a Tier on spawn, which grants it
 * a random subset of Abilities and stat multipliers. The full roll
 * (Tier, abilities, or split-copy status) is persisted via
 * {@link TierSavedData} and restored exactly on world/chunk reloads.
 * <p>
 * Rolls are a single uniform decision in {@link TierRoll} (Doom, then
 * Shade, then Cinder intervals), so the effective shares equal the
 * configured spawn chances — 40% Cinder / 20% Shade / 10% Doom with
 * 30% vanilla at defaults.
 * <p>
 * Gating decisions live in {@link InfusionGate}; this module is the single
 * Infusion interface callers cross (rolls, persistence, health, queries,
 * nametags), backed by a shared {@link InfusedRegistry}. In-memory state
 * is cleaned up when the mob dies or despawns.
 */
public final class MobTierManager {

    private static final float SPLIT_HEALTH_FRACTION = 0.6f;

    private static final InfusedRegistry SHARED = new InfusedRegistry();

    /** Greyscale colour code for Rupture split-copy nametags (no Tier). */
    private static final String SPLIT_COPY_COLOUR = "§7";

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
     * entry, so they are skipped here and never become Infused Mobs.
     * <p>
     * Worlds where the mod is inactive are skipped entirely — no tier,
     * no abilities, no nametag.
     */
    public static void assignTier(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        if (mob.getType().getCategory() != MobCategory.MONSTER) return;
        if (isMobBlacklisted(mob)) return;  // Config Mob Blacklist — these types never infuse
        if (InfusionGate.status(serverLevel) != InfusionGate.Status.ACTIVE) return;
        if (SHARED.find(mob.getUUID()) != null) return;  // Already assigned — prevents stacking

        UUID uuid = mob.getUUID();

        // If this UUID already rolled in a previous session, restore that result exactly
        Rolled rolled = Platform.hooks().loadRoll(serverLevel, uuid);
        if (rolled != null) {
            restoreRolled(mob, rolled);
            return;
        }

        ModConfig.Instance config = ModConfig.get();

        MobTier tier = TierRoll.decide(
                mob.getRandom().nextDouble(),
                config.forTier(MobTier.CINDER).spawnChance(),
                config.forTier(MobTier.SHADE).spawnChance(),
                config.forTier(MobTier.DOOM).spawnChance());
        if (tier != null) {
            ModConfig.TierConfig tierConfig = config.forTier(tier);
            List<Ability> abilities = AbilityRegistry.getRandomAbilities(tierConfig.abilityCount());
            Platform.hooks().storeRoll(serverLevel, uuid, trackTiered(mob, tier, abilities));
            return;
        }

        // Rolled nothing — persist so we never roll again for this UUID
        Platform.hooks().storeRoll(serverLevel, uuid, new Rolled.Nothing());
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
        ServerLevel serverLevel = mob.level() instanceof ServerLevel server ? server : null;
        if (serverLevel != null && InfusionGate.status(serverLevel) != InfusionGate.Status.ACTIVE) {
            return false;
        }
        Rolled.Tiered rolled = trackTiered(mob, tier, abilities);

        if (serverLevel != null) {
            Platform.hooks().storeRoll(serverLevel, mob.getUUID(), rolled);
        }
        return true;
    }

    /** Tracks a tiered roll: registry entry, health multiplier, tier nametag. Returns the persisted roll. */
    private static Rolled.Tiered trackTiered(Mob mob, MobTier tier, List<Ability> abilities) {
        Rolled.Tiered rolled = new Rolled.Tiered(tier, idsOf(abilities));
        SHARED.track(mob.getUUID(), rolled);
        applyHealthMultiplier(mob, ModConfig.get().forTier(tier));
        setTierNametag(mob, tier, abilities);
        return rolled;
    }

    /** Tracks a split copy: registry entry, Cinder stats, greyscale nametag. Returns the persisted roll. */
    private static Rolled.Split trackSplit(Mob mob, List<Ability> abilities) {
        Rolled.Split rolled = new Rolled.Split(idsOf(abilities));
        SHARED.track(mob.getUUID(), rolled);
        applyCinderStats(mob);
        setSplitCopyNametag(mob, abilities);
        return rolled;
    }

    /** Restores the persisted roll exactly — tier, abilities, or split-copy status. */
    private static void restoreRolled(Mob mob, Rolled rolled) {
        if (rolled instanceof Rolled.Tiered tieredRoll) {
            trackTiered(mob, tieredRoll.tier(), resolveAbilities(tieredRoll.abilityIds()));
        } else if (rolled instanceof Rolled.Split splitRoll) {
            trackSplit(mob, resolveAbilities(splitRoll.abilityIds()));
        } else if (rolled instanceof Rolled.Nothing) {
            // Rolled nothing — leave the mob vanilla.
        }
    }

    private static List<Ability> resolveAbilities(List<String> abilityIds) {
        return AbilityRegistry.getAbilitiesByIds(abilityIds);
    }

    private static List<String> idsOf(List<Ability> abilities) {
        return abilities.stream().map(Ability::id).toList();
    }

    /** True when the mob's entity type is on the config Mob Blacklist (unregistered types never match). */
    private static boolean isMobBlacklisted(Mob mob) {
        String key = Platform.hooks().entityKey(mob.getType());
        return key != null && ModConfig.get().isMobBlacklisted(key);
    }

    private static void applyHealthMultiplier(Mob mob, ModConfig.TierConfig tierConfig) {
        applyHealthBoost(mob, tierConfig.healthMultiplier(), 1.0);
    }

    /**
     * Single health-boost routine behind both Tier assignment and split-copy
     * handling: scales max health by {@code multiplier}, then sets health to
     * {@code healthFraction} of the boosted max. Tiered mobs pass 1.0 (full
     * heal); split copies pass 0.6.
     */
    private static void applyHealthBoost(Mob mob, double multiplier, double healthFraction) {
        var attribute = mob.getAttribute(Attributes.MAX_HEALTH);
        if (attribute == null) return;
        attribute.setBaseValue(attribute.getBaseValue() * multiplier);
        mob.setHealth(mob.getMaxHealth() * (float) healthFraction);
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
     * reloads restore it as a copy — it can never be re-rolled into an
     * Infused Mob.
     */
    public static void applyCinderTierToSplitCopy(Mob copy) {
        List<Ability> abilities = AbilityRegistry.getRandomAbilities(1, "rupture");

        Rolled.Split rolled = trackSplit(copy, abilities);
        if (copy.level() instanceof ServerLevel serverLevel) {
            Platform.hooks().storeRoll(serverLevel, copy.getUUID(), rolled);
        }
    }

    /** Applies the Cinder health multiplier and sets the copy to 60% of its boosted max. */
    private static void applyCinderStats(Mob mob) {
        applyHealthBoost(mob, ModConfig.get().forTier(MobTier.CINDER).healthMultiplier(), SPLIT_HEALTH_FRACTION);
    }

    // ========================================
    // Cleanup
    // ========================================

    /** Removes all tracking for this mob (called on death or despawn). */
    public static void removeMob(Mob mob) {
        UUID uuid = mob.getUUID();
        SHARED.untrack(uuid);

        // Clean up persistent state so it doesn't grow unboundedly
        if (mob.level() instanceof ServerLevel serverLevel) {
            Platform.hooks().clearRoll(serverLevel, uuid);
        }
    }

    // ========================================
    // Queries
    // ========================================

    /** Returns the tier assigned to this mob, or null (split copy / untracked). */
    public static MobTier getTier(Mob mob) {
        return SHARED.find(mob.getUUID()) instanceof Rolled.Tiered tiered ? tiered.tier() : null;
    }

    /** Returns true if this mob is tracked as a Rupture split copy (Cinder stats, no Tier). */
    public static boolean isSplitCopy(Mob mob) {
        return SHARED.isSplitCopy(mob.getUUID());
    }

    /** Returns true if this mob has an ability with the given id. */
    public static boolean hasAbility(Mob mob, String id) {
        for (Ability ability : liveAbilities(mob.getUUID())) {
            if (ability.id().equals(id)) return true;
        }
        return false;
    }

    /** Resolves the tracked roll's ability ids to live objects (empty when untracked). */
    private static List<Ability> liveAbilities(UUID uuid) {
        Rolled rolled = SHARED.find(uuid);
        return rolled == null ? List.of() : AbilityRegistry.getAbilitiesByIds(rolled.abilityIds());
    }

    /** Returns abilities assigned to this mob matching the given trigger type. */
    public static List<Ability> getAbilitiesByTrigger(Mob mob, TriggerType trigger) {
        return AbilityRegistry.forTrigger(liveAbilities(mob.getUUID()), trigger);
    }

    /**
     * Returns the UUIDs of tracked mobs that have at least one TICK ability.
     * Used by the TICK trigger to iterate only the mobs it can affect.
     */
    public static Set<UUID> getTickMobUUIDs() {
        return SHARED.tickMobUUIDs();
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

    /** Clears all tracking. Test-only — live code untracks per mob. */
    public static void clearForTests() {
        SHARED.clear();
    }

    // ========================================
    // Nametag presentation
    // ========================================

    /** Shows the Tier nametag for a freshly assigned or restored mob. */
    private static void setTierNametag(Mob mob, MobTier tier, List<Ability> abilities) {
        setNametag(mob, tier.colourCode(), abilities);
    }

    /** Shows the greyscale nametag for a Rupture split copy (Cinder stats, no Tier). */
    private static void setSplitCopyNametag(Mob mob, List<Ability> abilities) {
        setNametag(mob, SPLIT_COPY_COLOUR, abilities);
    }

    private static void setNametag(Mob mob, String colour, List<Ability> abilities) {
        if (!ModConfig.get().showNametags()) return;
        List<String> names = abilities.stream().map(Ability::name).toList();
        String entityName = entityTypeName(mob);
        mob.setCustomName(Component.literal(NametagFormatter.format(colour, names, entityName)));
        mob.setCustomNameVisible(true);
    }

    /**
     * Base entity-type name (e.g. "Zombie") for nametags.
     * Uses the type description rather than {@code getName()}, which would
     * return any previously-set custom name and cause duplication.
     */
    private static String entityTypeName(Mob mob) {
        return mob.getType().getDescription().getString();
    }

    /**
     * Applies or removes nametags for all tracked mobs based on the
     * current nametag setting. Called when the toggle changes via command.
     */
    public static void refreshNametags(MinecraftServer server) {
        boolean show = ModConfig.get().showNametags();
        for (Map.Entry<UUID, Rolled> entry : SHARED.snapshot().entrySet()) {
            Mob mob = findMob(server, entry.getKey());
            if (mob == null) continue;
            Rolled rolled = entry.getValue();

            if (show) {
                List<Ability> abilities = AbilityRegistry.getAbilitiesByIds(rolled.abilityIds());
                if (rolled instanceof Rolled.Tiered tiered) {
                    setTierNametag(mob, tiered.tier(), abilities);
                } else if (rolled instanceof Rolled.Split) {
                    setSplitCopyNametag(mob, abilities);
                }
            } else {
                mob.setCustomName(null);
                mob.setCustomNameVisible(false);
            }
        }
    }
}
