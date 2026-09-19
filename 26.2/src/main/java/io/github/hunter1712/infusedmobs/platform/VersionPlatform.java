package io.github.hunter1712.infusedmobs.platform;

import io.github.hunter1712.infusedmobs.ability.EffectToken;
import io.github.hunter1712.infusedmobs.ability.trigger.MobHurtTrigger;
import io.github.hunter1712.infusedmobs.tier.Rolled;
import io.github.hunter1712.infusedmobs.tier.TierSavedData;

import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.IdentifierArgument;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Platform adapter for 26.2 — modern identifier, Holder-based effects,
 * post-mitigation damage event and reasoned spawns.
 * <p>
 * Adapter size accepted: a single thin file per Versioned Source Set.
 * Duplication across versions is inherent — Mojang/Fabric APIs diverge
 * (Identifier versus ResourceLocation, Holder versus raw effects,
 * AFTER_DAMAGE versus ALLOW_DAMAGE, reasoned versus plain spawns,
 * hurtServer versus hurt, SavedDataType versus Factory/Function) — and no
 * further thinning is possible without a preprocessor.
 */
public final class VersionPlatform implements PlatformHooks {
    @Override
    public String dimensionId(ServerLevel level) {
        return level.dimension().identifier().toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> T spawnEntity(EntityType<T> type, ServerLevel level) {
        return (T) type.create(level, EntitySpawnReason.REINFORCEMENT);
    }

    @Override
    public Entity spawnForCommand(EntityType<?> type, ServerLevel level) {
        return type.create(level, EntitySpawnReason.COMMAND);
    }

    @Override
    public String entityKey(EntityType<?> type) {
        Identifier id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id == null ? null : id.toString();
    }

    @Override
    public EntityType<?> defaultEntity() {
        return BuiltInRegistries.ENTITY_TYPE.getValue(
                Identifier.fromNamespaceAndPath("minecraft", "zombie"));
    }

    @Override
    public Predicate<CommandSourceStack> gamemasterPermission() {
        return Commands.hasPermission(Commands.LEVEL_GAMEMASTERS);
    }

    @Override
    public RequiredArgumentBuilder<CommandSourceStack, ?> worldIdArgument(String name) {
        return Commands.argument(name, IdentifierArgument.id());
    }

    @Override
    public String worldIdFromCommand(CommandContext<CommandSourceStack> ctx, String name) {
        return IdentifierArgument.getId(ctx, name).toString();
    }

    @Override
    public EffectToken slowness() { return new HolderToken(MobEffects.SLOWNESS); }

    @Override
    public EffectToken resistance() { return new HolderToken(MobEffects.RESISTANCE); }

    @Override
    public EffectToken strength() { return new HolderToken(MobEffects.STRENGTH); }

    @Override
    public EffectToken speed() { return new HolderToken(MobEffects.SPEED); }

    @Override
    public EffectToken poison() { return new HolderToken(MobEffects.POISON); }

    @Override
    public EffectToken wither() { return new HolderToken(MobEffects.WITHER); }

    @Override
    public EffectToken weakness() { return new HolderToken(MobEffects.WEAKNESS); }

    @Override
    public EffectToken regeneration() { return new HolderToken(MobEffects.REGENERATION); }

    @Override
    public void damageArmor(ServerPlayer player, ServerLevel level, int amount) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            player.getItemBySlot(slot).hurtAndBreak(amount, level, player, item -> {});
        }
    }

    @Override
    public void reflectThorns(Player attacker, Mob mob, float reflected, ServerLevel level) {
        if (attacker instanceof ServerPlayer sp) {
            sp.hurtServer(level, sp.damageSources().thorns(mob), reflected);
        } else {
            attacker.hurtServer(level, attacker.damageSources().thorns(mob), reflected);
        }
    }

    @Override
    public void hurtFromExplosion(LivingEntity living, ServerLevel level, DamageSource source, float amount) {
        living.hurtServer(level, source, amount);
    }

    @Override
    public void ignite(LivingEntity target, int seconds) {
        target.igniteForSeconds(seconds);
    }

    @Override
    public void registerHurtTrigger(MobHurtTrigger.HurtHandler handler) {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(handler::onHurt);
    }

    @Override
    public Rolled loadRoll(ServerLevel level, UUID uuid) {
        return TierSavedData.get(level).getRolled(uuid);
    }

    @Override
    public void storeRoll(ServerLevel level, UUID uuid, Rolled rolled) {
        TierSavedData.get(level).setRolled(uuid, rolled);
    }

    @Override
    public void clearRoll(ServerLevel level, UUID uuid) {
        TierSavedData.get(level).remove(uuid);
    }

    /**
     * Typed token carrying a modern holder effect — no casts at the boundary.
     */
    private record HolderToken(Holder<MobEffect> effect) implements EffectToken {
        @Override
        public void applyHurt(LivingEntity target, int duration, int amplifier) {
            target.addEffect(new MobEffectInstance(effect, duration, amplifier));
        }

        @Override
        public void applyTick(LivingEntity mob, int duration, int amplifier) {
            mob.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, false));
        }
    }
}
