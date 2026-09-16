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
        if (ModConfig.get().isWorldBlacklisted(Platform.hooks().dimensionId(level))) return Status.WORLD_BLACKLISTED;
        if (!ModGameRules.isEnabled(level.getServer())) return Status.RULE_DISABLED;
        return Status.ACTIVE;
    }

    /** Pure decision helper — unit-testable without Minecraft bootstrap. */
    static Status status(boolean worldBlacklisted, Boolean storedEnabled) {
        if (worldBlacklisted) return Status.WORLD_BLACKLISTED;
        if (!ModGameRules.resolveRule(storedEnabled, ModGameRules.defaultValue())) {
            return Status.RULE_DISABLED;
        }
        return Status.ACTIVE;
    }
}
