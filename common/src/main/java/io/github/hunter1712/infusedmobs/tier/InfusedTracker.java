package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.config.ModConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * In-memory registry of Infused Mobs plus their nametag presentation.
 * <p>
 * Implementation behind the single Infusion interface in
 * {@link MobTierManager}: live code crosses the manager's seam, never this
 * module directly. Tier rolls and persistence coordination live in
 * {@link MobTierManager} and gating in {@link InfusionGate}; this module
 * only tracks which mobs are infused, answers queries about them, and shows
 * or hides their Tier nametags. Tracking is cleaned up when the mob dies
 * or despawns.
 */
public final class InfusedTracker {

    private static final InfusedRegistry SHARED = new InfusedRegistry();

    /** Greyscale colour code for Rupture split-copy nametags (no Tier). */
    private static final String SPLIT_COPY_COLOUR = "§7";

    private InfusedTracker() {}

    // ========================================
    // Registry
    // ========================================

    /** Starts tracking this UUID as the given infused state. */
    public static void track(UUID id, InfusedMob infused) {
        SHARED.track(id, infused);
    }

    /** Returns the tracked state for this UUID, or null if untracked. */
    public static InfusedMob find(UUID id) {
        return SHARED.find(id);
    }

    /**
     * Stops tracking this UUID.
     *
     * @return true if anything was tracked
     */
    public static boolean untrack(UUID id) {
        return SHARED.untrack(id);
    }

    /** Clears all tracking. Test-only — live code untracks per mob. Public so shared test extensions can isolate state. */
    public static void clear() {
        SHARED.clear();
    }

    // ========================================
    // Queries
    // ========================================

    /** Returns the tier assigned to this mob, or null (split copy / untracked). */
    public static MobTier getTier(Mob mob) {
        return extractTier(findInfused(mob));
    }

    private static InfusedMob findInfused(Mob mob) {
        return SHARED.find(mob.getUUID());
    }

    private static MobTier extractTier(InfusedMob infused) {
        if (infused instanceof InfusedMob.Tiered tiered) return tiered.tier();
        return null; // Untracked, SplitCopy and future variants have no tier
    }

    /** Returns true if this UUID is tracked as a Rupture split copy (Cinder stats, no Tier). */
    public static boolean isSplitCopy(UUID id) {
        return SHARED.find(id) instanceof InfusedMob.SplitCopy;
    }

    /** Returns true if this mob is tracked as a Rupture split copy (Cinder stats, no Tier). */
    public static boolean isSplitCopy(Mob mob) {
        return isSplitCopy(mob.getUUID());
    }

    /** Returns abilities assigned to this mob matching the given trigger type. */
    public static List<Ability> getAbilitiesByTrigger(Mob mob, TriggerType trigger) {
        InfusedMob infused = findInfused(mob);
        return infused == null ? List.of() : infused.forTrigger(trigger);
    }

    /** Returns all abilities assigned to this mob (empty list if none). */
    public static List<Ability> getAllAbilities(Mob mob) {
        InfusedMob infused = findInfused(mob);
        return infused == null ? List.of() : infused.abilities();
    }

    /** Returns true if this mob has an ability with the given id. */
    public static boolean hasAbility(Mob mob, String id) {
        InfusedMob infused = findInfused(mob);
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

    // ========================================
    // Nametag presentation
    // ========================================

    /** Shows the Tier nametag for a freshly assigned or restored mob. */
    public static void setTierNametag(Mob mob, MobTier tier, List<Ability> abilities) {
        setNametag(mob, tier.colourCode(), abilities);
    }

    /** Shows the greyscale nametag for a Rupture split copy (Cinder stats, no Tier). */
    public static void setSplitCopyNametag(Mob mob, List<Ability> abilities) {
        setNametag(mob, SPLIT_COPY_COLOUR, abilities);
    }

    private static void applyNametagForInfused(Mob mob, InfusedMob infused) {
        if (infused instanceof InfusedMob.Tiered tiered) {
            setTierNametag(mob, tiered.tier(), tiered.abilities());
        } else if (infused instanceof InfusedMob.SplitCopy splitCopy) {
            setSplitCopyNametag(mob, splitCopy.abilities());
        }
    }

    private static void setNametag(Mob mob, String colour, List<Ability> abilities) {
        if (!ModConfig.get().showNametags()) return;
        if (hasForeignName(mob)) return;
        List<String> names = abilities.stream().map(Ability::name).toList();
        String entityName = entityTypeName(mob);
        mob.setCustomName(Component.literal(NametagFormatter.format(colour, names, entityName)));
        mob.setCustomNameVisible(true);
    }

    /**
     * True when the mob carries a custom name this mod did not set
     * (e.g. another mod's display name) — those are never overwritten.
     */
    private static boolean hasForeignName(Mob mob) {
        if (!mob.hasCustomName()) return false;
        Component customName = mob.getCustomName();
        if (customName == null) return false;
        return !NametagFormatter.owns(customName.getString(), entityTypeName(mob));
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
     * current {@link ModConfig.Instance#showNametags()} setting.
     * Called when the toggle changes via command.
     */
    public static void refreshNametags(MinecraftServer server) {
        boolean show = ModConfig.get().showNametags();
        for (Map.Entry<UUID, InfusedMob> entry : SHARED.snapshot().entrySet()) {
            Mob mob = findMob(server, entry.getKey());
            if (mob == null) continue;
            InfusedMob infused = entry.getValue();

            if (show) {
                applyNametagForInfused(mob, infused);
            } else if (!hasForeignName(mob)) {
                // Only clear nametags this mod owns — foreign display
                // names (other mods) are left alone.
                mob.setCustomName(null);
                mob.setCustomNameVisible(false);
            }
        }
    }
}
