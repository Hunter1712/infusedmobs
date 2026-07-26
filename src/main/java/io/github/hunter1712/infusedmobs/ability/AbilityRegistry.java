package io.github.hunter1712.infusedmobs.ability;

import io.github.hunter1712.infusedmobs.ability.effect.SplitEffect;
import io.github.hunter1712.infusedmobs.config.ModConfig;
import io.github.hunter1712.infusedmobs.trigger.DamageContext;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

/**
 * Central registry containing the global mob ability pool.
 * <p>
 * Abilities are stored in a flat list and randomly sampled when a mob
 * is assigned a tier on spawn. Abilities are also indexed by trigger
 * type for fast filtering during tier assignment.
 */
public final class AbilityRegistry {

    private static final List<Ability> ALL_ABILITIES = new ArrayList<>();
    private static final Map<TriggerType, List<Ability>> BY_TRIGGER = new EnumMap<>(TriggerType.class);
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private AbilityRegistry() {}

    // ========================================
    // Registration
    // ========================================

    /**
     * Populates the global ability pool. Must be called during mod init.
     * All abilities use vanilla effects — no custom status effects needed.
     */
    public static void registerAll() {
        ModConfig.Instance cfg = ModConfig.get();

        // ---- HURT abilities (fire when the mob melee-hits a player) ----

        registerHurtEffect("venom",  "Bane",   MobEffects.POISON,   cfg.hurtEffectDuration(), cfg.hurtEffectAmplifier());
        registerHurtEffect("freeze", "Chill",  MobEffects.SLOWNESS, cfg.hurtEffectDuration(), 2);
        registerHurtEffect("decay",  "Decay",  MobEffects.WITHER,   cfg.hurtEffectDuration(), cfg.hurtEffectAmplifier());
        registerHurtEffect("hex",    "Hex",    MobEffects.WEAKNESS, cfg.hurtEffectDuration(), cfg.hurtEffectAmplifier());

        all("inferno", "Hellfire", TriggerType.HURT, (mob, target) ->
                target.igniteForSeconds(cfg.infernoFireSeconds()));

        all("siphon", "Siphon", TriggerType.HURT, (mob, target) -> {
            float amount = DamageContext.getAndClear();
            if (amount > 0) mob.heal(amount);
        });

        all("acid", "Vitriol", TriggerType.HURT, AbilityRegistry::damageArmor);

        // ---- TICK abilities (passive, refresh every 2 seconds while alive) ----

        registerTickEffect("fortify", "Ward",  MobEffects.RESISTANCE,   cfg.tickEffectDuration(), cfg.tickEffectAmplifier());
        registerTickEffect("fury",    "Frenzy", MobEffects.STRENGTH,    cfg.tickEffectDuration(), cfg.tickEffectAmplifier());
        registerTickEffect("gust",    "Wraith", MobEffects.SPEED,       cfg.tickEffectDuration(), cfg.tickEffectAmplifier());
        registerTickEffect("bloom",   "Blight", MobEffects.REGENERATION, cfg.tickEffectDuration(), cfg.tickEffectAmplifier());

        // Thorns: reactive TICK ability — no status effect, reflection handled in MobHurtTrigger
        all("thorns", "Thorns", TriggerType.TICK, (mob, target) -> {});

        // ---- DEATH abilities ----

        all("fission", "Rupture", TriggerType.DEATH, (mob, target) -> SplitEffect.apply(mob));

        all("combust", "Combust", TriggerType.DEATH, (mob, target) -> {
            if (mob.level() instanceof ServerLevel level) {
                double radius = cfg.combustExplosionPower() * 2.0;
                var entities = level.getEntities(mob, mob.getBoundingBox().inflate(radius));
                var dmgSource = level.damageSources().explosion(null, null);
                for (var entity : entities) {
                    if (entity instanceof LivingEntity living && entity != mob) {
                        double dist = entity.distanceTo(mob);
                        if (dist <= radius) {
                            float damage = (float) (4.0 * (1.0 - dist / radius));
                            living.hurt(dmgSource, Math.max(damage, 1.0f));
                        }
                    }
                }
                // Explosion sound without particles or block damage
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        });
    }

    /**
     * Registers a HURT ability that applies a status effect to the target.
     */
    private static void registerHurtEffect(String id, String name, Holder<net.minecraft.world.effect.MobEffect> effect,
                                           int duration, int amplifier) {
        all(id, name, TriggerType.HURT, (mob, target) ->
                target.addEffect(new MobEffectInstance(effect, duration, amplifier)));
    }

    /**
     * Registers a TICK ability that applies a status effect to the mob itself.
     */
    private static void registerTickEffect(String id, String name, Holder<net.minecraft.world.effect.MobEffect> effect,
                                           int duration, int amplifier) {
        all(id, name, TriggerType.TICK, (mob, target) ->
                mob.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, false)));
    }

    /**
     * Damages all 4 armor slots by the configurable durability amount.
     * {@code mob} param unused — required by {@code BiConsumer} signature.
     */
    private static void damageArmor(LivingEntity mob, LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) return;
        // instanceof already verified ServerPlayer, so cast is safe
        ServerLevel level = (ServerLevel) player.level();
        int dmg = ModConfig.get().acidArmorDamage();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            player.getItemBySlot(slot).hurtAndBreak(dmg, level, player, item -> {});
        }
    }

    /** Convenience: builds and registers a single ability. */
    private static void all(String id, String name, TriggerType trigger,
                            BiConsumer<LivingEntity, LivingEntity> effect) {
        Ability ability = new Ability(id, name, trigger, effect);
        ALL_ABILITIES.add(ability);
        BY_TRIGGER.computeIfAbsent(trigger, t -> new ArrayList<>()).add(ability);
    }

    // ========================================
    // Query
    // ========================================

    /**
     * Returns a fixed set of abilities with the specified distribution
     * of trigger types. Within each type, abilities are picked randomly.
     *
     * @return a shuffled, unmodifiable list
     */
    public static List<Ability> getRandomAbilities(int hurt, int tick, int death) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        List<Ability> result = new ArrayList<>(hurt + tick + death);

        pickRandom(result, TriggerType.HURT, hurt, rng);
        pickRandom(result, TriggerType.TICK, tick, rng);
        pickRandom(result, TriggerType.DEATH, death, rng);

        if (result.size() > 1) Collections.shuffle(result, rng);
        return Collections.unmodifiableList(result);
    }

    /** Picks {@code count} random abilities of the given trigger type into target. */
    private static void pickRandom(List<Ability> target, TriggerType type, int count, ThreadLocalRandom rng) {
        if (count <= 0) return;
        List<Ability> candidates = BY_TRIGGER.get(type);
        if (candidates == null || candidates.isEmpty()) return;

        // Copy so we don't permanently shuffle the cached index
        List<Ability> pool = new ArrayList<>(candidates);
        Collections.shuffle(pool, rng);
        target.addAll(pool.subList(0, Math.min(count, pool.size())));
    }
}
