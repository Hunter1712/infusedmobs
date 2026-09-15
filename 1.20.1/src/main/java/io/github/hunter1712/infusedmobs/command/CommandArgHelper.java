package io.github.hunter1712.infusedmobs.command;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;

/**
 * Version shim for command argument types (1.20.1 — ResourceLocation).
 * 1.20.1 has no {@code IdentifierArgument}; world ids use
 * {@code ResourceLocationArgument} returning {@code ResourceLocation}.
 */
public final class CommandArgHelper {
    private CommandArgHelper() {}

    /** Permission predicate for gamemaster-level subcommands. */
    public static java.util.function.Predicate<CommandSourceStack> gamemaster() {
        return src -> src.hasPermission(Commands.LEVEL_GAMEMASTERS);
    }

    /** Argument node for a world/dimension id. */
    public static RequiredArgumentBuilder<CommandSourceStack, ?> worldId(String name) {
        return Commands.argument(name, ResourceLocationArgument.id());
    }

    /** Extracts the world id string from context. */
    public static String getWorldId(CommandContext<CommandSourceStack> ctx, String name) {
        return ResourceLocationArgument.getId(ctx, name).toString();
    }

    /** Default summon target (zombie) for {@code /infusedmobs summon <tier>}. */
    public static EntityType<?> defaultEntity() {
        return EntityType.ZOMBIE;
    }

    /** Registry key of an entity type as string, or null if unregistered. */
    public static String entityKey(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id == null ? null : id.toString();
    }

    /** Creates an entity for the summon command (plain create; no spawn-reason overload). */
    public static Entity createForCommand(EntityType<?> type, ServerLevel level) {
        return type.create(level);
    }
}
