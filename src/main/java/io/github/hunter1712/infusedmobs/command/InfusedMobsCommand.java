package io.github.hunter1712.infusedmobs.command;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.config.ModConfig;
import io.github.hunter1712.infusedmobs.tier.MobTier;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.commands.arguments.ResourceArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Registers and handles the {@code /infusedmobs} command tree.
 * <p>
 * All subcommands require gamemaster-level permissions.
 */
public final class InfusedMobsCommand {

    private InfusedMobsCommand() {}

    // ========================================
    // Registration
    // ========================================

    /** Binds the command to the Fabric event bus. Call once during mod init. */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, buildContext, selection) ->
                register(dispatcher, buildContext));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher, CommandBuildContext buildContext) {
        dispatcher.register(Commands.literal("infusedmobs")
                .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))

                // --- help ---
                .then(Commands.literal("help")
                        .executes(InfusedMobsCommand::executeHelp))
                .executes(InfusedMobsCommand::executeHelp)

                // --- nametag ---
                .then(Commands.literal("nametag")
                        .then(Commands.literal("on")
                                .executes(ctx -> setNametags(ctx, true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> setNametags(ctx, false)))
                        .executes(InfusedMobsCommand::showNametagStatus))

                // --- announce ---
                .then(Commands.literal("announce")
                        .then(Commands.literal("on")
                                .executes(ctx -> setAnnouncements(ctx, true)))
                        .then(Commands.literal("off")
                                .executes(ctx -> setAnnouncements(ctx, false)))
                        .executes(InfusedMobsCommand::showAnnouncementStatus))

                // --- world ---
                .then(Commands.literal("world")
                        .then(Commands.literal("add")
                                .then(Commands.argument("world", IdentifierArgument.id())
                                        .suggests(InfusedMobsCommand::suggestWorldIds)
                                        .executes(InfusedMobsCommand::worldAdd)))
                        .then(Commands.literal("remove")
                                .then(Commands.argument("world", IdentifierArgument.id())
                                        .suggests(InfusedMobsCommand::suggestWorldIds)
                                        .executes(InfusedMobsCommand::worldRemove)))
                        .then(Commands.literal("list")
                                .executes(InfusedMobsCommand::worldList))
                        .executes(InfusedMobsCommand::worldList))

                // --- reload ---
                .then(Commands.literal("reload")
                        .executes(InfusedMobsCommand::executeReload))

                // --- list ---
                .then(Commands.literal("list")
                        .executes(InfusedMobsCommand::executeList))

                // --- summon ---
                .then(Commands.literal("summon")
                        .then(Commands.argument("tier", StringArgumentType.word())
                                .suggests((ctx, builder) -> {
                                    builder.suggest("cinder");
                                    builder.suggest("shade");
                                    builder.suggest("doom");
                                    return builder.buildFuture();
                                })
                                .executes(ctx -> summon(ctx,
                                        StringArgumentType.getString(ctx, "tier"),
                                        null)) // default zombie
                                .then(Commands.argument("entity",
                                                ResourceArgument.resource(buildContext, Registries.ENTITY_TYPE))
                                        .suggests((ctx, builder) -> {
                                            for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
                                                if (type.canSummon()
                                                        && type.getCategory() == MobCategory.MONSTER) {
                                                    Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                                                    if (id != null) {
                                                        builder.suggest(id.toString());
                                                    }
                                                }
                                            }
                                            return builder.buildFuture();
                                        })
                                        .executes(ctx -> summon(ctx,
                                                StringArgumentType.getString(ctx, "tier"),
                                                ResourceArgument.getSummonableEntityType(ctx, "entity")
                                                        .value()))
                                        .then(Commands.argument("abilities",
                                                         StringArgumentType.greedyString())
                                                 .suggests(InfusedMobsCommand::suggestAbilityIds)
                                                 .executes(ctx -> {
                                                     List<Ability> abils = parseAbilities(ctx);
                                                     if (abils == null) return 0;
                                                     return summon(ctx,
                                                             StringArgumentType.getString(ctx, "tier"),
                                                             ResourceArgument.getSummonableEntityType(ctx, "entity")
                                                                     .value(),
                                                             abils);
                                                  })))))
        );
    }

    // ========================================
    // Command handlers
    // ========================================

    /** Shows available commands. */
    private static int executeHelp(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        source.sendSystemMessage(Component.literal(
                "§e--- InfusedMobs Commands ---"));
        source.sendSystemMessage(Component.literal(
                "§f/infusedmobs §7— show this help"));
        source.sendSystemMessage(Component.literal(
                "§f/infusedmobs nametag [on|off] §7— show or set nametag visibility"));
        source.sendSystemMessage(Component.literal(
                "§f/infusedmobs announce [on|off] §7— show or set chat announcements"));
        source.sendSystemMessage(Component.literal(
                "§f/infusedmobs world add|remove <world> §7— blacklist a world (disables the mod there)"));
        source.sendSystemMessage(Component.literal(
                "§f/infusedmobs world list §7— show blacklisted worlds"));
        source.sendSystemMessage(Component.literal(
                "§f/infusedmobs reload §7— reload config from disk"));
        source.sendSystemMessage(Component.literal(
                "§f/infusedmobs list §7— list all hostile mob types that can be infused"));
        source.sendSystemMessage(Component.literal(
                "§f/infusedmobs summon <tier> [entity] [abilities] §7— spawn a tiered mob at crosshair"));
        source.sendSystemMessage(Component.literal(
                "§8Tiers: cinder, shade, doom §8| Abilities: space-separated IDs (e.g., bane thorns)"));
        return 1;
    }

    // ========================================
    // /infusedmobs nametag [on|off]
    // ========================================

    /** Reports the current nametag visibility setting. */
    private static int showNametagStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        boolean current = ModConfig.get().showNametags();
        source.sendSuccess(() -> Component.literal(
                "§eNametags are currently §f" + (current ? "§aON" : "§cOFF")), false);
        return 1;
    }

    /** Sets nametag visibility and refreshes all loaded tiered mobs. */
    private static int setNametags(CommandContext<CommandSourceStack> ctx, boolean show) {
        CommandSourceStack source = ctx.getSource();
        ModConfig.Instance updated = ModConfig.get().withShowNametags(show);
        ModConfig.swapInstance(updated);
        MobTierManager.refreshNametags(source.getServer());
        source.sendSuccess(() -> Component.literal(
                "§eNametags turned " + (show ? "§aON" : "§cOFF")), true);
        return 1;
    }

    // ========================================
    // /infusedmobs announce [on|off]
    // ========================================

    /** Reports the current announcement setting. */
    private static int showAnnouncementStatus(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        boolean current = ModConfig.get().showAnnouncements();
        source.sendSuccess(() -> Component.literal(
                "§eChat announcements are currently §f" + (current ? "§aON" : "§cOFF")), false);
        return 1;
    }

    /** Toggles chat announcements on/off and persists to config. */
    private static int setAnnouncements(CommandContext<CommandSourceStack> ctx, boolean show) {
        CommandSourceStack source = ctx.getSource();
        ModConfig.Instance updated = ModConfig.get().withShowAnnouncements(show);
        ModConfig.swapInstance(updated);
        source.sendSuccess(() -> Component.literal(
                "§eChat announcements turned " + (show ? "§aON" : "§cOFF")), true);
        return 1;
    }

    // ========================================
    // /infusedmobs world add|remove <world> | list
    // ========================================

    /** Adds a world to the blacklist and persists. */
    private static int worldAdd(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String worldId = IdentifierArgument.getId(ctx, "world").toString();

        ModConfig.Instance current = ModConfig.get();
        if (current.isWorldBlacklisted(worldId)) {
            source.sendSuccess(() -> Component.literal(
                    "§eWorld §f" + worldId + " §eis already on the blacklist."), false);
            return 1;
        }

        List<String> updated = new ArrayList<>(current.worldBlacklist());
        updated.add(worldId);
        ModConfig.swapInstance(current.withWorldBlacklist(updated));

        source.sendSuccess(() -> Component.literal(
                "§eAdded §f" + worldId + " §eto the blacklist. "
                        + "The mod is now disabled there."), true);
        return 1;
    }

    /** Removes a world from the blacklist and persists. */
    private static int worldRemove(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        String worldId = IdentifierArgument.getId(ctx, "world").toString();

        ModConfig.Instance current = ModConfig.get();
        if (!current.isWorldBlacklisted(worldId)) {
            source.sendFailure(Component.literal(
                    "§cWorld §f" + worldId + " §cis not on the blacklist."));
            return 0;
        }

        List<String> updated = new ArrayList<>(current.worldBlacklist());
        updated.removeIf(worldId::equals);
        ModConfig.swapInstance(current.withWorldBlacklist(updated));

        source.sendSuccess(() -> Component.literal(
                "§eRemoved §f" + worldId + " §efrom the blacklist. "
                        + "The mod is now active there."), true);
        return 1;
    }

    /** Lists all worlds currently on the blacklist. */
    private static int worldList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        List<String> blacklist = ModConfig.get().worldBlacklist();

        if (blacklist.isEmpty()) {
            source.sendSuccess(() -> Component.literal(
                    "§eThe world blacklist is empty — the mod is active in all worlds."), false);
            return 1;
        }

        source.sendSuccess(() -> Component.literal(
                "§eBlacklisted worlds (" + blacklist.size() + "):"), false);
        for (String world : blacklist) {
            source.sendSystemMessage(Component.literal("§f - " + world));
        }
        return 1;
    }

    /** Tab-completion provider suggesting loaded world dimension ids + the current world. */
    private static CompletableFuture<Suggestions> suggestWorldIds(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        CommandSourceStack source = ctx.getSource();

        // Always suggest the world the caller is standing in first.
        String currentWorld = source.getLevel().dimension().identifier().toString();
        builder.suggest(currentWorld);

        // Then suggest every other loaded level's dimension id.
        for (ServerLevel level : source.getServer().getAllLevels()) {
            String id = level.dimension().identifier().toString();
            if (!id.equals(currentWorld)) {
                builder.suggest(id);
            }
        }
        return builder.buildFuture();
    }

    // ========================================
    // /infusedmobs reload
    // ========================================

    /** Reloads config from disk and refreshes nametags. */
    private static int executeReload(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        ModConfig.load();
        MobTierManager.refreshNametags(source.getServer());
        source.sendSuccess(() -> Component.literal("§aConfig reloaded from disk."), true);
        return 1;
    }

    // ========================================
    // Spawn position helpers
    // ========================================

    /** Maximum raycast distance for determining spawn position. */
    private static final double RAYCAST_DISTANCE = 10.0;

    /**
     * Returns the block position the player is looking at (on the block face),
     * or the command source's position for non-player callers (console).
     */
    private static Vec3 resolveSpawnPosition(CommandSourceStack source) {
        if (source.getEntity() instanceof ServerPlayer player) {
            HitResult hit = player.pick(RAYCAST_DISTANCE, 1.0F, false);
            if (hit instanceof BlockHitResult blockHit) {
                return blockHit.getLocation();
            }
        }
        return source.getPosition();
    }

    // ========================================
    // /infusedmobs summon <tier> [entity] [abilities]
    // ========================================

    /** Convenience overload: no specific abilities (draws random ones per tier config). */
    private static int summon(CommandContext<CommandSourceStack> ctx, String tierName, EntityType<?> entityType) {
        return summon(ctx, tierName, entityType, null);
    }

    /**
     * Spawns a tiered mob at the command source's crosshair/position,
     * optionally with specific abilities (or random if null/empty).
     */
    private static int summon(CommandContext<CommandSourceStack> ctx, String tierName,
                              EntityType<?> entityType, List<Ability> abilities) {
        CommandSourceStack source = ctx.getSource();

        // Parse tier
        MobTier tier = parseTier(tierName);
        if (tier == null) {
            source.sendFailure(Component.literal(
                    "§cUnknown tier: " + tierName + ". Use: cinder, shade, or doom."));
            return 0;
        }

        // If entity is null (convenience overload), default to zombie
        if (entityType == null) {
            entityType = BuiltInRegistries.ENTITY_TYPE.getValue(Identifier.tryParse("minecraft:zombie"));
        }

        Vec3 spawnPos = resolveSpawnPosition(source);
        ServerLevel level = source.getLevel();

        // Refuse to summon in blacklisted worlds — the blacklist is authoritative.
        if (MobTierManager.isWorldBlacklisted(level)) {
            source.sendFailure(Component.literal(
                    "§cThis world is on the infused-mobs blacklist. "
                            + "Remove it with §f/infusedmobs world remove "
                            + level.dimension().identifier() + "§c to summon here."));
            return 0;
        }

        Entity raw = entityType.create(level, EntitySpawnReason.COMMAND);
        if (!(raw instanceof Mob mob)) {
            source.sendFailure(Component.literal(
                    "§c" + entityType.getDescription().getString() + " is not a mob."));
            return 0;
        }

        mob.setPos(spawnPos.x, spawnPos.y, spawnPos.z);

        // Use specific abilities if provided, otherwise draw random ones per tier config
        List<Ability> finalAbilities = (abilities != null && !abilities.isEmpty())
                ? abilities
                : AbilityRegistry.getRandomAbilities(
                        ModConfig.get().forTier(tier).abilityCount());
        MobTierManager.assignSpecificTier(mob, tier, finalAbilities);
        if (!level.addFreshEntity(mob)) {
            source.sendFailure(Component.literal("§cFailed to spawn entity."));
            return 0;
        }

        String entityName = entityType.getDescription().getString();
        source.sendSuccess(() -> Component.literal(
                "§aSpawned §f" + tier.colourCode() + tier.name()
                        + " §f" + entityName + " §aat " + formatPos(spawnPos) + "."), true);
        return 1;
    }

    /**
     * Parses a space-separated ability string and validates every ID.
     *
     * @return the resolved abilities, or {@code null} if any ID was unknown
     *         (the error message is already sent to the source in that case)
     */
    private static List<Ability> parseAbilities(CommandContext<CommandSourceStack> ctx) {
        String raw = StringArgumentType.getString(ctx, "abilities");
        if (raw == null || raw.isBlank()) return List.of();
        String[] parts = raw.trim().split("\\s+");
        List<String> ids = new ArrayList<>();
        for (String part : parts) {
            if (!part.isEmpty()) ids.add(part);
        }
        if (ids.isEmpty()) return List.of();

        // resolve — silently skips unknown
        List<Ability> abilities = AbilityRegistry.getAbilitiesByIds(ids);
        List<String> foundIds = abilities.stream()
                .map(Ability::id)
                .toList();

        // find unknown IDs
        List<String> notFound = new ArrayList<>(ids);
        notFound.removeAll(foundIds);

        if (!notFound.isEmpty()) {
            List<String> allIds = AbilityRegistry.getAllAbilityIds();
            StringBuilder msg = new StringBuilder();
            msg.append("§cUnknown ability ID(s): ").append(String.join(", ", notFound)).append("\n");
            for (String bad : notFound) {
                String closest = findClosest(bad, allIds);
                if (closest != null) {
                    msg.append("§eDid you mean §f").append(closest).append("§e?\n");
                }
            }
            msg.append("§7Valid IDs: §f").append(String.join("§7, §f", allIds));
            ctx.getSource().sendFailure(Component.literal(msg.toString()));
            return null;
        }

        return abilities;
    }

    /** Formats a Vec3 as a concise coordinate string. */
    private static String formatPos(Vec3 pos) {
        return String.format("§7[%.1f, %.1f, %.1f]§r", pos.x, pos.y, pos.z);
    }

    // ========================================
    // /infusedmobs list
    // ========================================

    /** Lists all hostile mob types that can be infused with tiers. */
    private static int executeList(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        StringBuilder sb = new StringBuilder("§e--- Hostile Mobs ---\n§7");
        int count = 0;
        for (EntityType<?> type : BuiltInRegistries.ENTITY_TYPE) {
            if (type.canSummon() && type.getCategory() == MobCategory.MONSTER) {
                Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
                if (id != null) {
                    if (count > 0) sb.append("§7, ");
                    sb.append(id.toString());
                    count++;
                }
            }
        }
        source.sendSystemMessage(Component.literal(sb.toString()));
        source.sendSystemMessage(Component.literal(
                "§8" + count + " hostile mob type(s) — "
                        + "use with §f/infusedmobs summon <tier> <entity>§8"));
        return 1;
    }

    // ========================================
    // Helpers
    // ========================================

    /** Tab-completion provider for space-separated ability IDs. */
    private static CompletableFuture<Suggestions> suggestAbilityIds(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        // getRemaining() returns text AFTER the cursor position, NOT the full argument value.
        // When cursor is at end of input, getRemaining() is "" even when text was typed.
        // Use getInput() + getStart() to reconstruct the actual argument content.
        String input = builder.getInput();
        int start = builder.getStart();

        // Find the beginning of this argument's value by walking back from start
        // to find the previous word boundary (space)
        int argStart = 0;
        for (int i = start - 1; i >= 0; i--) {
            if (input.charAt(i) == ' ') {
                argStart = i + 1;
                break;
            }
        }

        // Full text from the argument value start to end of input
        // Keep trailing space so we can detect when user finished a word
        String fullValue = input.substring(argStart);
        boolean hasTrailingSpace = fullValue.endsWith(" ");
        fullValue = fullValue.trim();

        List<String> allIds = AbilityRegistry.getAllAbilityIds();

        // Nothing typed yet — show all abilities
        if (fullValue.isEmpty()) {
            for (String id : allIds) {
                builder.suggest(id);
            }
            return builder.buildFuture();
        }

        // User finished a word and pressed space — suggest the next unused ability
        if (hasTrailingSpace) {
            String prefix = fullValue + " ";
            for (String id : allIds) {
                if (fullValue.contains(id)) continue;
                builder.suggest(prefix + id);
            }
            return builder.buildFuture();
        }

        // Determine the current word being typed (last segment)
        int lastSpace = fullValue.lastIndexOf(' ');
        String prefix = lastSpace >= 0 ? fullValue.substring(0, lastSpace + 1) : "";
        String currentWord = lastSpace >= 0 ? fullValue.substring(lastSpace + 1) : fullValue;

        // Only suggest when the current segment matches the start of an ability ID
        for (String id : allIds) {
            if (fullValue.contains(id)) continue; // already picked
            if (id.startsWith(currentWord)) {
                builder.suggest(prefix + id);
            }
        }

        return builder.buildFuture();
    }

    private static MobTier parseTier(String name) {
        return switch (name.toLowerCase()) {
            case "cinder" -> MobTier.CINDER;
            case "shade" -> MobTier.SHADE;
            case "doom" -> MobTier.DOOM;
            default -> null;
        };
    }

    /**
     * Finds the closest-matching string from {@code candidates} using
     * Levenshtein distance. Returns {@code null} if nothing is close enough.
     */
    static String findClosest(String input, List<String> candidates) {
        String best = null;
        int bestDist = Integer.MAX_VALUE;
        String lower = input.toLowerCase();
        for (String candidate : candidates) {
            String cLower = candidate.toLowerCase();
            // Exact prefix match wins immediately
            if (cLower.startsWith(lower)) return candidate;
            int dist = levenshtein(lower, cLower);
            if (dist < bestDist) {
                bestDist = dist;
                best = candidate;
            }
        }
        // Only suggest if the distance is small enough
        return bestDist <= 2 ? best : null;
    }

    /** Computes Levenshtein edit distance between two strings. */
    private static int levenshtein(String a, String b) {
        int[][] dp = new int[a.length() + 1][b.length() + 1];
        for (int i = 0; i <= a.length(); i++) dp[i][0] = i;
        for (int j = 0; j <= b.length(); j++) dp[0][j] = j;
        for (int i = 1; i <= a.length(); i++) {
            for (int j = 1; j <= b.length(); j++) {
                dp[i][j] = Math.min(
                        Math.min(dp[i - 1][j] + 1, dp[i][j - 1] + 1),
                        dp[i - 1][j - 1] + (a.charAt(i - 1) == b.charAt(j - 1) ? 0 : 1));
            }
        }
        return dp[a.length()][b.length()];
    }

}
