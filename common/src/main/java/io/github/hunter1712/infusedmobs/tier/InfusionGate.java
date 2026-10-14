package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.config.ModConfig;
import io.github.hunter1712.infusedmobs.gamerules.ModGameRules;
import io.github.hunter1712.infusedmobs.platform.Platform;

import net.minecraft.server.level.ServerLevel;

/**
 * Decides whether infusion is active in a level.
 * <p>
 * The Gamerule Gate ({@code infusedmobs:enabled}, per-save) is combined per
 * dimension with the World Blacklist (config): infusion runs only when the
 * dimension is not blacklisted and the gamerule is on — the blacklist
 * dominates.
 */
public final class InfusionGate {

    private InfusionGate() {}

    /**
     * Why the mod is (or isn't) active in a level — used by summon to give
     * precise feedback and by assignment to gate infusion.
     */
    public enum Status {
        /** Mod active: not blacklisted and the {@code infusedmobs:enabled} rule is on. */
        ACTIVE,
        /** The level's dimension is on the config blacklist. */
        WORLD_BLACKLISTED,
        /** The {@code infusedmobs:enabled} gamerule is off. */
        RULE_DISABLED
    }

    /** Status of the mod in the given level. */
    public static Status status(ServerLevel level) {
        boolean blacklisted = ModConfig.get().isWorldBlacklisted(Platform.hooks().dimensionId(level));
        boolean enabled = ModGameRules.isEnabled(level.getServer());
        return status(blacklisted, enabled);
    }

    /** Pure decision helper — unit-testable without Minecraft bootstrap. */
    static Status status(boolean worldBlacklisted, Boolean storedEnabled) {
        if (worldBlacklisted) return Status.WORLD_BLACKLISTED;
        boolean enabled = ModGameRules.resolveRule(storedEnabled, ModGameRules.defaultValue());
        return enabled ? Status.ACTIVE : Status.RULE_DISABLED;
    }

    /**
     * Summon refusal text for the given status, or null when active.
     * Behind the Gating seam so the summon command never branches on
     * statuses itself — one place owns both the decision and its wording.
     *
     * @param status the gate decision for the target level
     * @param dimensionId string form of the target dimension, named in the
     *                    blacklist fix hint
     * @return refusal text, or null when {@code status} is active
     */
    public static String refusalMessage(Status status, String dimensionId) {
        return switch (status) {
            case ACTIVE -> null;
            case WORLD_BLACKLISTED -> "§cThis world is on the infused-mobs blacklist. "
                    + "Remove it with §f/infusedmobs world remove "
                    + dimensionId + "§c to summon here.";
            case RULE_DISABLED -> "§cInfused mobs are disabled in this world. "
                    + "Enable them with §f/gamerule infusedmobs:enabled true§c.";
        };
    }
}
