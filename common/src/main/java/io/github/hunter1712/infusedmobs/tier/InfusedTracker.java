package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.config.ModConfig;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * In-memory registry of Infused Mobs plus their nametag presentation.
 * <p>
 * Tier rolls and persistence coordination live in {@link MobTierManager} and
 * gating in {@link InfusionGate}; this module only tracks which mobs are
 * infused, answers queries about them, and shows or hides their Tier
 * nametags. Tracking is cleaned up when the mob dies or despawns.
 */
public final class InfusedTracker {

    private static final Map<UUID, InfusedMob> TRACKED = new HashMap<>();

    private InfusedTracker() {}

    // ========================================
    // Registry
    // ========================================

    /** Starts tracking this UUID as the given infused state. */
    public static void track(UUID id, InfusedMob infused) {
        TRACKED.put(id, infused);
    }

    /** Returns the tracked state for this UUID, or null if untracked. */
    public static InfusedMob find(UUID id) {
        return TRACKED.get(id);
    }

    /**
     * Stops tracking this UUID.
     *
     * @return true if anything was tracked
     */
    public static boolean untrack(UUID id) {
        return TRACKED.remove(id) != null;
    }

    /** Clears all tracking. Test-only — live code untracks per mob. Public so shared test extensions can isolate state. */
    public static void clear() {
        TRACKED.clear();
    }

    // ========================================
    // Queries
    // ========================================

    /** Returns the tier assigned to this mob, or null (split copy / untracked). */
    public static MobTier getTier(Mob mob) {
        InfusedMob infused = TRACKED.get(mob.getUUID());
        return infused == null ? null : extractTier(infused);
    }

    private static MobTier extractTier(InfusedMob infused) {
        if (infused instanceof InfusedMob.TieredMob t) return t.tier();
        return null; // SplitCopyMob and future variants have no tier
    }

    /** Returns abilities assigned to this mob matching the given trigger type. */
    public static List<Ability> getAbilitiesByTrigger(Mob mob, TriggerType trigger) {
        InfusedMob infused = TRACKED.get(mob.getUUID());
        return infused == null ? List.of() : infused.forTrigger(trigger);
    }

    /** Returns all abilities assigned to this mob (empty list if none). */
    public static List<Ability> getAllAbilities(Mob mob) {
        InfusedMob infused = TRACKED.get(mob.getUUID());
        return infused == null ? List.of() : infused.abilities();
    }

    /** Returns true if this mob has an ability with the given id. */
    public static boolean hasAbility(Mob mob, String id) {
        InfusedMob infused = TRACKED.get(mob.getUUID());
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
        return TRACKED.entrySet().stream()
                .filter(e -> !e.getValue().forTrigger(TriggerType.TICK).isEmpty())
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
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

    /** Shows the greyscale nametag for a Rupture split copy. */
    public static void setSplitCopyNametag(Mob mob, List<Ability> abilities) {
        setNametag(mob, "§7", abilities);
    }

    private static void applyNametagForInfused(Mob mob, InfusedMob infused) {
        if (infused instanceof InfusedMob.TieredMob t) {
            setTierNametag(mob, t.tier(), t.abilities());
        } else if (infused instanceof InfusedMob.SplitCopyMob s) {
            setSplitCopyNametag(mob, s.abilities());
        }
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
     * Applies or removes nametags for all tracked mobs based on the
     * current {@link ModConfig.Instance#showNametags()} setting.
     * Called when the toggle changes via command.
     */
    public static void refreshNametags(MinecraftServer server) {
        boolean show = ModConfig.get().showNametags();
        for (Map.Entry<UUID, InfusedMob> entry : TRACKED.entrySet()) {
            Mob mob = findMob(server, entry.getKey());
            if (mob == null) continue;
            InfusedMob infused = entry.getValue();

            if (show) {
                if (infused.abilities().isEmpty()) continue;
                applyNametagForInfused(mob, infused);
            } else {
                mob.setCustomName(null);
                mob.setCustomNameVisible(false);
            }
        }
    }
}
