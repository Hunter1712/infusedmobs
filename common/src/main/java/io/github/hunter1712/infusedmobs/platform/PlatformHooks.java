package io.github.hunter1712.infusedmobs.platform;

import io.github.hunter1712.infusedmobs.ability.EffectToken;
import io.github.hunter1712.infusedmobs.ability.trigger.MobHurtTrigger;
import io.github.hunter1712.infusedmobs.tier.Rolled;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Version-neutral seam for every call shared code makes into
 * version-divergent Minecraft or Fabric APIs.
 * <p>
 * One implementation per Versioned Source Set carries the divergent bodies;
 * shared code calls only this interface via {@link Platform#hooks()}.
 * Persistence mechanics and gamerule registration stay version-owned —
 * only the per-mob roll operations cross this seam.
 */
public interface PlatformHooks {

    // ---- Dimension ----

    /** String form of the dimension id for gating checks. */
    String dimensionId(ServerLevel level);

    // ---- Spawn ----

    /** Creates a split-copy entity in the level. */
    <T extends Entity> T spawnEntity(EntityType<T> type, ServerLevel level);

    /** Creates an entity for the summon command. */
    Entity spawnForCommand(EntityType<?> type, ServerLevel level);

    /** Registry key of an entity type as string, or null if unregistered. */
    String entityKey(EntityType<?> type);

    /** Default summon target, or null if missing from the registry. */
    EntityType<?> defaultEntity();

    // ---- Command arguments ----

    /** Permission predicate for gamemaster-level subcommands. */
    Predicate<CommandSourceStack> gamemasterPermission();

    /** Argument node for a world/dimension id. */
    RequiredArgumentBuilder<CommandSourceStack, ?> worldIdArgument(String name);

    /** Extracts the world id string from command context. */
    String worldIdFromCommand(CommandContext<CommandSourceStack> ctx, String name);

    // ---- Status-effect tokens ----

    EffectToken slowness();

    EffectToken resistance();

    EffectToken strength();

    EffectToken speed();

    EffectToken poison();

    EffectToken wither();

    EffectToken weakness();

    EffectToken regeneration();

    /** Applies a HURT effect token to the target. */
    void applyHurtEffect(LivingEntity target, EffectToken effect, int duration, int amplifier);

    /** Applies a TICK effect token to the mob itself. */
    void applyTickEffect(LivingEntity mob, EffectToken effect, int duration, int amplifier);

    // ---- Combat ----

    /** Damages all worn armor by the given durability amount. */
    void damageArmor(ServerPlayer player, ServerLevel level, int amount);

    /** Reflects Thorns damage back at the attacker. */
    void reflectThorns(Player attacker, Mob mob, float reflected, ServerLevel level);

    /** Hurts a living entity from an explosion source. */
    void hurtFromExplosion(LivingEntity living, ServerLevel level, DamageSource source, float amount);

    /** Ignites the target for the given seconds. */
    void ignite(LivingEntity target, int seconds);

    /** Registers the HURT trigger against the version's damage event. */
    void registerHurtTrigger(MobHurtTrigger.HurtHandler handler);

    // ---- Persisted rolls ----

    /** Stored roll for this UUID, or null if never rolled. */
    Rolled loadRoll(ServerLevel level, UUID uuid);

    /** Records the roll result for this UUID. */
    void storeRoll(ServerLevel level, UUID uuid, Rolled rolled);

    /** Removes stored state for this UUID. */
    void clearRoll(ServerLevel level, UUID uuid);
}
