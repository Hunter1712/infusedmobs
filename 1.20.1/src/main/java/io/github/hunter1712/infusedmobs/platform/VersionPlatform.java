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
import net.minecraft.commands.arguments.ResourceLocationArgument;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.UUID;
import java.util.function.Predicate;

/**
 * Platform adapter for 1.20.1 — ResourceLocation dimension and command ids,
 * raw-effect handles, legacy pre-mitigation damage event, plain entity
 * creation and legacy armor handling.
 * <p>
 * Adapter size accepted: a single thin file per Versioned Source Set.
 * Duplication across versions is inherent — Mojang/Fabric APIs diverge
 * (Identifier versus ResourceLocation, Holder versus raw effects,
 * AFTER_DAMAGE versus ALLOW_DAMAGE, reasoned versus plain spawns,
 * hurtServer versus hurt, SavedDataType versus Factory/Function) — and no
 * further thinning is possible without a preprocessor.
 * <p>
 * Accepted divergences from newer versions (exact parity is impossible
 * without the post-mitigation event): damage amounts are pre-mitigation and
 * shield blocks are not reported, so a fully-blocked hit still fires HURT
 * abilities. The handler never cancels damage. User docs scope shield
 * negation to 1.21.1/26.2 for this reason.
 */
public final class VersionPlatform implements PlatformHooks {
    @Override
    public String dimensionId(ServerLevel level) {
        return level.dimension().location().toString();
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> T spawnEntity(EntityType<T> type, ServerLevel level) {
        return (T) type.create(level);
    }

    @Override
    public Entity spawnForCommand(EntityType<?> type, ServerLevel level) {
        return type.create(level);
    }

    @Override
    public String entityKey(EntityType<?> type) {
        ResourceLocation id = BuiltInRegistries.ENTITY_TYPE.getKey(type);
        return id == null ? null : id.toString();
    }

    @Override
    public EntityType<?> defaultEntity() {
        return EntityType.ZOMBIE;
    }

    @Override
    public Predicate<CommandSourceStack> gamemasterPermission() {
        return src -> src.hasPermission(Commands.LEVEL_GAMEMASTERS);
    }

    @Override
    public RequiredArgumentBuilder<CommandSourceStack, ?> worldIdArgument(String name) {
        return Commands.argument(name, ResourceLocationArgument.id());
    }

    @Override
    public String worldIdFromCommand(CommandContext<CommandSourceStack> ctx, String name) {
        return ResourceLocationArgument.getId(ctx, name).toString();
    }

    @Override
    public EffectToken slowness() { return new RawToken(MobEffects.MOVEMENT_SLOWDOWN); }

    @Override
    public EffectToken resistance() { return new RawToken(MobEffects.DAMAGE_RESISTANCE); }

    @Override
    public EffectToken strength() { return new RawToken(MobEffects.DAMAGE_BOOST); }

    @Override
    public EffectToken speed() { return new RawToken(MobEffects.MOVEMENT_SPEED); }

    @Override
    public EffectToken poison() { return new RawToken(MobEffects.POISON); }

    @Override
    public EffectToken wither() { return new RawToken(MobEffects.WITHER); }

    @Override
    public EffectToken weakness() { return new RawToken(MobEffects.WEAKNESS); }

    @Override
    public EffectToken regeneration() { return new RawToken(MobEffects.REGENERATION); }

    @Override
    public void damageArmor(ServerPlayer player, ServerLevel level, int amount) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            player.getItemBySlot(slot).hurtAndBreak(amount, player, item -> {});
        }
    }

    @Override
    public void reflectThorns(Player attacker, Mob mob, float reflected, ServerLevel level) {
        attacker.hurt(attacker.damageSources().thorns(mob), reflected);
    }

    @Override
    public void hurtFromExplosion(LivingEntity living, ServerLevel level, DamageSource source, float amount) {
        living.hurt(source, amount);
    }

    @Override
    public void ignite(LivingEntity target, int seconds) {
        target.setSecondsOnFire(seconds);
    }

    @Override
    public void registerHurtTrigger(MobHurtTrigger.HurtHandler handler) {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            handler.onHurt(entity, source, amount, amount, false);
            return true;
        });
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
     * Typed token carrying a legacy raw effect — no casts at the boundary.
     */
    private record RawToken(MobEffect effect) implements EffectToken {
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
