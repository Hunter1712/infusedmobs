package io.github.hunter1712.infusedmobs.command;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.config.ModConfig;
import io.github.hunter1712.infusedmobs.tier.MobTier;
import io.github.hunter1712.infusedmobs.tier.MobTierManager;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Registers and handles the {@code /infusedmobs} command tree.
 * <p>
 * All subcommands require gamemaster-level permissions.
 */
public final class InfusedMobsCommand {

    private static final int MAX_TRACE_DISTANCE = 30;

    private InfusedMobsCommand() {}

    // ========================================
    // Registration
    // ========================================

    /** Binds the command to the Fabric event bus. Call once during mod init. */
    public static void register() {
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                register(dispatcher));
    }

    private static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
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

                // --- info ---
                .then(Commands.literal("info")
                        .executes(InfusedMobsCommand::executeInfo))

                // --- reload ---
                .then(Commands.literal("reload")
                        .executes(InfusedMobsCommand::executeReload))

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
                                        "minecraft:zombie"))
                                .then(Commands.argument("entity", StringArgumentType.string())
                                        .executes(ctx -> summon(ctx,
                                                StringArgumentType.getString(ctx, "tier"),
                                                StringArgumentType.getString(ctx, "entity"))))))
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
                "§f/infusedmobs info §7— show tier and abilities of looked-at mob"));
        source.sendSystemMessage(Component.literal(
                "§f/infusedmobs reload §7— reload config from disk"));
        source.sendSystemMessage(Component.literal(
                "§f/infusedmobs summon <tier> [entity] §7— spawn a tiered mob (default: zombie)"));
        source.sendSystemMessage(Component.literal(
                "§8Tiers: cinder, shade, doom"));
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
    // /infusedmobs info
    // ========================================

    /** Shows tier, abilities, and stats for the mob the player is looking at. */
    private static int executeInfo(CommandContext<CommandSourceStack> ctx) {
        CommandSourceStack source = ctx.getSource();
        if (!(source.getEntity() instanceof ServerPlayer player)) {
            source.sendFailure(Component.literal("§cOnly players can use this command."));
            return 0;
        }

        Mob target = findLookedAtMob(player);
        if (target == null) {
            source.sendFailure(Component.literal(
                    "§cNo mob in sight within " + MAX_TRACE_DISTANCE + " blocks."));
            return 0;
        }

        ServerLevel level = source.getLevel();

        MobTier tier = MobTierManager.getTier(target);
        if (tier == null) {
            String name = target.getType().getDescription().getString();
            source.sendSystemMessage(Component.literal("§7" + name + " — §fno tier assigned"));
            return 1;
        }

        String colour = tier.colourCode();

        String entityName = target.getType().getDescription().getString();
        double health = target.getHealth();
        double maxHealth = target.getMaxHealth();
        int xp = target.getExperienceReward(level, player);

        source.sendSystemMessage(Component.literal(
                "§e--- " + colour + tier.name() + " §e" + entityName + " §e---"));
        source.sendSystemMessage(Component.literal(
                "§7HP: §f" + String.format("%.1f", health)
                        + "§7/§f" + String.format("%.1f", maxHealth)
                        + "  §7XP: §f" + xp));
        source.sendSystemMessage(Component.literal(
                "§7Multiplier: §f" + String.format("%.1f", tier.healthMultiplier())
                        + "× HP, §f" + String.format("%.1f", tier.xpMultiplier()) + "× XP"));

        List<Ability> abilities = MobTierManager.getAllAbilities(target);
        if (!abilities.isEmpty()) {
            StringBuilder sb = new StringBuilder("§7Abilities: ");
            for (int i = 0; i < abilities.size(); i++) {
                Ability a = abilities.get(i);
                if (i > 0) sb.append("§7, ");
                sb.append("§f").append(a.name())
                        .append(" §8[").append(triggerLabel(a.trigger())).append("§8]");
            }
            source.sendSystemMessage(Component.literal(sb.toString()));
        }

        return 1;
    }

    /** Simple ray-trace to find the first mob the player is looking at. */
    private static Mob findLookedAtMob(ServerPlayer player) {
        Vec3 eyePos = player.getEyePosition();
        Vec3 lookVec = player.getViewVector(1.0F);
        Vec3 endPos = eyePos.add(
                lookVec.x * MAX_TRACE_DISTANCE,
                lookVec.y * MAX_TRACE_DISTANCE,
                lookVec.z * MAX_TRACE_DISTANCE);
        AABB box = player.getBoundingBox().expandTowards(lookVec).inflate(2.0);

        // Collect candidate mobs in the search volume, then pick the closest one
        return player.level().getEntities(
                player, box, e -> e instanceof Mob mob && mob.isAlive() && !e.isSpectator()
        ).stream()
                .map(e -> (Mob) e)
                .filter(mob -> mob.getBoundingBox().clip(eyePos, endPos).isPresent())
                .min((a, b) -> {
                    double da = a.distanceToSqr(player);
                    double db = b.distanceToSqr(player);
                    return Double.compare(da, db);
                })
                .orElse(null);
    }

    // ========================================
    // /infusedmobs summon <tier> [entity]
    // ========================================

    /** Spawns a tiered mob of the given type in front of the player. */
    private static int summon(CommandContext<CommandSourceStack> ctx, String tierName, String entityArg) {
        CommandSourceStack source = ctx.getSource();

        // Parse tier
        MobTier tier = parseTier(tierName);
        if (tier == null) {
            source.sendFailure(Component.literal(
                    "§cUnknown tier: " + tierName + ". Use: cinder, shade, or doom."));
            return 0;
        }

        // Parse entity type
        Identifier id = Identifier.tryParse(entityArg);
        if (id == null) {
            source.sendFailure(Component.literal("§cInvalid entity identifier: " + entityArg));
            return 0;
        }
        EntityType<?> entityType = BuiltInRegistries.ENTITY_TYPE.getValue(id);
        if (entityType == null) {
            source.sendFailure(Component.literal("§cUnknown entity: " + entityArg));
            return 0;
        }

        ServerLevel level = source.getLevel();

        // Spawn the entity
        Vec3 pos = source.getPosition();
        Entity raw = entityType.create(level, EntitySpawnReason.COMMAND);
        if (!(raw instanceof Mob mob)) {
            source.sendFailure(Component.literal("§c" + entityArg + " is not a mob."));
            return 0;
        }

        mob.setPos(pos.x, pos.y, pos.z);
        // Apply the tier
        MobTierManager.assignTier(mob);
        if (!level.addFreshEntity(mob)) {
            source.sendFailure(Component.literal("§cFailed to spawn entity."));
            return 0;
        }

        String entityName = entityType.getDescription().getString();
        source.sendSuccess(() -> Component.literal(
                "§aSpawned §f" + tier.colourCode() + tier.name()
                        + " §f" + entityName + " §aat your location."), true);
        return 1;
    }

    // ========================================
    // Helpers
    // ========================================

    private static MobTier parseTier(String name) {
        return switch (name.toLowerCase()) {
            case "cinder" -> MobTier.CINDER;
            case "shade" -> MobTier.SHADE;
            case "doom" -> MobTier.DOOM;
            default -> null;
        };
    }

    private static String triggerLabel(TriggerType type) {
        return switch (type) {
            case HURT -> "§cHurt";
            case TICK -> "§bTick";
            case DEATH -> "§dDeath";
        };
    }
}
