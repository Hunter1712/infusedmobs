package io.github.hunter1712.infusedmobs.gamerules;

import net.fabricmc.fabric.api.gamerule.v1.GameRuleFactory;
import net.fabricmc.fabric.api.gamerule.v1.GameRuleRegistry;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.GameRules;

/**
 * Custom gamerules for Infused Mobs, registered via Fabric's
 * {@link GameRuleRegistry} during mod initialisation.
 * <p>
 * Gamerules give per-world-save control that survives restarts and can be
 * set at launch by modpack makers (datapacks, gamerule-modifying mods, or
 * the in-game {@code /gamerule} command):
 * <ul>
 *   <li>{@code infusedmobs:enabled} — master switch for the mod in a world.
 *       When {@code false}, mobs spawn as vanilla (no tiers, abilities,
 *       nametags) and summoning is refused.</li>
 * </ul>
 * The rule is combined with the config blacklist by
 * {@link io.github.hunter1712.infusedmobs.tier.MobTierManager#canInfuse}.
 */
public final class ModGameRules {

    /** Master switch — set false to disable the mod in this world save. */
    public static final GameRules.Key<GameRules.BooleanValue> ENABLED =
            GameRuleRegistry.register("infusedmobs:enabled", GameRules.Category.MOBS,
                    GameRuleFactory.createBooleanRule(true));

    private ModGameRules() {}

    /**
     * Registers the rule. Must be called from {@code onInitialize}.
     * (Fabric's registry handles actual registration at class-load;
     * this method exists for API parity with other versions.)
     */
    public static void register() {
        // No-op: ENABLED is registered via GameRuleRegistry at class init.
    }

    /** Version-agnostic check for the master switch. */
    public static boolean isEnabled(MinecraftServer server) {
        return server.getGameRules().getBoolean(ENABLED);
    }

    // ========================================
    // Pure resolution helper (unit-testable without Minecraft bootstrap)
    // ========================================

    /** Returns {@code stored} when set, otherwise {@code defaultValue}. */
    public static boolean resolveRule(Boolean stored, boolean defaultValue) {
        return stored != null ? stored : defaultValue;
    }

    /** Default value of the master switch (matches the registered rule). */
    public static boolean defaultValue() {
        return true;
    }
}
