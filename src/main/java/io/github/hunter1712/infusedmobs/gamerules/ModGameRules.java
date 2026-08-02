package io.github.hunter1712.infusedmobs.gamerules;

import io.github.hunter1712.infusedmobs.InfusedMobsMod;

import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.serialization.Codec;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleMap;
import net.minecraft.world.level.gamerules.GameRuleType;
import net.minecraft.world.flag.FeatureFlagSet;

/**
 * Custom gamerules for Infused Mobs, registered in
 * {@link BuiltInRegistries#GAME_RULE} during mod initialisation.
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
    public static final GameRule<Boolean> ENABLED =
            createRule(GameRuleCategory.MOBS, true);

    private ModGameRules() {}

    /**
     * Registers the rule into {@link BuiltInRegistries#GAME_RULE}.
     * Must be called from {@code onInitialize} — the registry is frozen
     * later during {@code Bootstrap.bootStrap()}.
     */
    public static void register() {
        Registry.register(BuiltInRegistries.GAME_RULE, id("enabled"), ENABLED);
    }

    /**
     * Reads the current value of a boolean rule for the server's world save.
     * Never uses {@code GameRules.get(GameRule)} directly — that throws for
     * rules not yet stored in the save (e.g. fresh worlds or rules added by
     * an upgrade); a missing value resolves to the rule's default instead.
     */
    public static boolean readRule(MinecraftServer server, GameRule<Boolean> rule) {
        return resolveRule(stored(server, rule), rule.defaultValue());
    }

    // ========================================
    // Pure resolution helper (unit-testable without Minecraft bootstrap)
    // ========================================

    /** Returns {@code stored} when set, otherwise {@code defaultValue}. */
    public static boolean resolveRule(Boolean stored, boolean defaultValue) {
        return stored != null ? stored : defaultValue;
    }

    // ========================================
    // Helpers
    // ========================================

    private static GameRule<Boolean> createRule(GameRuleCategory category, boolean defaultValue) {
        return new GameRule<>(
                category,
                GameRuleType.BOOL,
                BoolArgumentType.bool(),
                (visitor, rule) -> visitor.visitBoolean(rule),
                Codec.BOOL,
                value -> 1,
                defaultValue,
                FeatureFlagSet.of()
        );
    }

    private static Identifier id(String name) {
        return Identifier.fromNamespaceAndPath(InfusedMobsMod.MOD_ID, name);
    }

    /**
     * Returns the stored value of the rule from the server's saved
     * game-rule data, or {@code null} when unset.
     */
    private static Boolean stored(MinecraftServer server, GameRule<Boolean> rule) {
        GameRuleMap map = server.getDataStorage().computeIfAbsent(GameRuleMap.TYPE);
        return map.get(rule);
    }
}
