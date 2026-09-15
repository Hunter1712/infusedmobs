package io.github.hunter1712.infusedmobs.command;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;

/**
 * Version shim for command argument types (1.20.1 — Identifier).
 * 1.20.1 compiles against 26.2 mappings (workaround), so this mirrors the
 * 26.2 shim; when compiled against real 1.20.1 mappings it must switch to
 * {@code ResourceLocation} argument types (see 1.21.1 shim).
 */
public final class CommandArgHelper {
    private CommandArgHelper() {}

    /** Permission predicate for gamemaster-level subcommands. */
    public static java.util.function.Predicate<CommandSourceStack> gamemaster() {
        return net.minecraft.commands.Commands.hasPermission(
                net.minecraft.commands.Commands.LEVEL_GAMEMASTERS);
    }

    /** Argument node for a world/dimension id. */
    public static RequiredArgumentBuilder<CommandSourceStack, ?> worldId(String name) {
        return net.minecraft.commands.Commands.argument(name, IdentifierArgument.id());
    }

    /** Extracts the world id string from context. */
    public static String getWorldId(CommandContext<CommandSourceStack> ctx, String name) {
        return IdentifierArgument.getId(ctx, name).toString();
    }

    /** Default summon target (zombie) for {@code /infusedmobs summon <tier>}. */
    public static EntityType<?> defaultEntity() {
        return BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "zombie"));
    }

    /** Registry key of an entity type as string, or null if unregistered. */
    public static String entityKey(EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id == null ? null : id.toString();
    }

    /** Creates an entity for the summon command (COMMAND spawn reason). */
    public static net.minecraft.world.entity.Entity createForCommand(EntityType<?> type, ServerLevel level) {
        return type.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
    }
}
