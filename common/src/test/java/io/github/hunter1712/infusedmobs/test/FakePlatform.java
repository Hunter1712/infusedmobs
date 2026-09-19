package io.github.hunter1712.infusedmobs.test;

import io.github.hunter1712.infusedmobs.ability.EffectToken;
import io.github.hunter1712.infusedmobs.ability.trigger.MobHurtTrigger;
import io.github.hunter1712.infusedmobs.tier.Rolled;
import io.github.hunter1712.infusedmobs.tier.TierRollStore;
import io.github.hunter1712.infusedmobs.platform.PlatformHooks;

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
 * In-memory fake behind the platform seam for executed conformance tests.
 * <p>
 * Every method tolerates null Minecraft objects so the suite stays hermetic
 * with no bootstrap: dimension identity is fixed, spawns record calls,
 * effect tokens record applications, combat helpers record calls, the HURT
 * handler is captured for later invocation, and persistence is an isolated
 * {@link TierRollStore} ignoring the level.
 */
public final class FakePlatform implements PlatformHooks {

    /** Fixed dimension id returned regardless of level (may be null). */
    public String dimensionId = "minecraft:overworld";

    /** Recorded calls for spawn/combat conformance. */
    public int spawnCalls;
    public SpawnKind lastSpawnKind;
    public int hurtApplications;
    public int tickApplications;
    public int damageArmorCalls;
    public int reflectThornsCalls;
    public int explosionCalls;
    public int igniteCalls;
    public float lastReflected;
    public int lastIgniteSeconds;

    /** Captured HURT handler for blocked-behaviour conformance. */
    public MobHurtTrigger.HurtHandler hurtHandler;

    private final TierRollStore rolls = new TierRollStore();

    /** Records token applications without touching Minecraft state. */
    public final class FakeToken implements EffectToken {
        public final String name;

        FakeToken(String name) {
            this.name = name;
        }

        @Override
        public void applyHurt(LivingEntity target, int duration, int amplifier) {
            hurtApplications++;
        }

        @Override
        public void applyTick(LivingEntity mob, int duration, int amplifier) {
            tickApplications++;
        }
    }

    @Override
    public String dimensionId(ServerLevel level) {
        return dimensionId;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Entity> T spawn(EntityType<T> type, ServerLevel level, SpawnKind kind) {
        spawnCalls++;
        lastSpawnKind = kind;
        return null;
    }

    @Override
    public String entityKey(EntityType<?> type) {
        return null;
    }

    @Override
    public EntityType<?> defaultEntity() {
        return null;
    }

    @Override
    public Predicate<CommandSourceStack> gamemasterPermission() {
        return src -> true;
    }

    @Override
    public RequiredArgumentBuilder<CommandSourceStack, ?> worldIdArgument(String name) {
        return null;
    }

    @Override
    public String worldIdFromCommand(CommandContext<CommandSourceStack> ctx, String name) {
        return null;
    }

    @Override
    public EffectToken effectToken(String id) {
        return new FakeToken(id);
    }

    @Override
    public void damageArmor(ServerPlayer player, ServerLevel level, int amount) {
        damageArmorCalls++;
    }

    @Override
    public void reflectThorns(Player attacker, Mob mob, float reflected, ServerLevel level) {
        reflectThornsCalls++;
        lastReflected = reflected;
    }

    @Override
    public void hurtFromExplosion(LivingEntity living, ServerLevel level, DamageSource source, float amount) {
        explosionCalls++;
    }

    @Override
    public void ignite(LivingEntity target, int seconds) {
        igniteCalls++;
        lastIgniteSeconds = seconds;
    }

    @Override
    public void registerHurtTrigger(MobHurtTrigger.HurtHandler handler) {
        hurtHandler = handler;
    }

    @Override
    public Rolled loadRoll(ServerLevel level, UUID uuid) {
        return rolls.get(uuid);
    }

    @Override
    public void storeRoll(ServerLevel level, UUID uuid, Rolled rolled) {
        rolls.put(uuid, rolled);
    }

    @Override
    public void clearRoll(ServerLevel level, UUID uuid) {
        rolls.remove(uuid);
    }
}
