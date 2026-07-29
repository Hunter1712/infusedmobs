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
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

/**
 * Central registry containing the global mob ability pool.
 * <p>
 * Abilities are stored in a flat list and randomly sampled when a mob
 * is assigned a tier on spawn.
 */
public final class AbilityRegistry {

    private static final List<Ability> ALL_ABILITIES = new ArrayList<>();
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

        // ---- HURT abilities (fire when the mob hits a player — melee or projectile) ----

        registerHurtEffect("bane",     "Bane",     MobEffects.POISON,   cfg.hurtEffectDuration(), cfg.hurtEffectAmplifier());
        registerHurtEffect("chill",    "Chill",    MobEffects.SLOWNESS, cfg.hurtEffectDuration(), cfg.hurtEffectAmplifier());
        registerHurtEffect("decay",    "Decay",    MobEffects.WITHER,   cfg.hurtEffectDuration(), cfg.hurtEffectAmplifier());
        registerHurtEffect("hex",      "Hex",      MobEffects.WEAKNESS, cfg.hurtEffectDuration(), cfg.hurtEffectAmplifier());

        all("hellfire", "Hellfire", TriggerType.HURT, (mob, target) ->
                target.igniteForSeconds(cfg.infernoFireSeconds()));

        all("siphon", "Siphon", TriggerType.HURT, (mob, target) -> {
            float amount = DamageContext.getAndClear();
            if (amount > 0) mob.heal(amount);
        });

        all("vitriol", "Vitriol", TriggerType.HURT, AbilityRegistry::damageArmor);

        // ---- TICK abilities (passive, refresh every 1 second while alive) ----

        registerTickEffect("ward",    "Ward",    MobEffects.RESISTANCE,   cfg.tickEffectDuration(), cfg.tickEffectAmplifier());
        registerTickEffect("frenzy",   "Frenzy",  MobEffects.STRENGTH,    cfg.tickEffectDuration(), cfg.tickEffectAmplifier());
        registerTickEffect("wraith",   "Wraith",  MobEffects.SPEED,       cfg.tickEffectDuration(), cfg.tickEffectAmplifier());
        registerTickEffect("blight",   "Blight",  MobEffects.REGENERATION, cfg.tickEffectDuration(), cfg.tickEffectAmplifier());

        // Thorns: reactive TICK ability — no status effect, reflection handled in MobHurtTrigger
        all("thorns", "Thorns", TriggerType.TICK, (mob, target) -> {});

        // ---- DEATH abilities ----

        all("rupture", "Rupture", TriggerType.DEATH, (mob, target) -> SplitEffect.apply(mob));

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
                            living.hurtServer(level, dmgSource, Math.max(damage, 1.0f));
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
        ServerLevel level = (ServerLevel) player.level();
        int dmg = ModConfig.get().acidArmorDamage();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            player.getItemBySlot(slot).hurtAndBreak(dmg, level, player, item -> {});
        }
    }

    /** Convenience: builds and registers a single ability. */
    private static void all(String id, String name, TriggerType trigger,
                            BiConsumer<LivingEntity, LivingEntity> effect) {
        ALL_ABILITIES.add(new Ability(id, name, trigger, effect));
    }

    // ========================================
    // Query
    // ========================================

    /**
     * Returns {@code count} random abilities drawn from the unified
     * ability pool (all trigger types mixed together).
     *
     * @return a shuffled, unmodifiable list
     */
    public static List<Ability> getRandomAbilities(int count) {
        if (count <= 0 || ALL_ABILITIES.isEmpty()) return List.of();

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        List<Ability> pool = new ArrayList<>(ALL_ABILITIES);
        Collections.shuffle(pool, rng);
        List<Ability> result = pool.subList(0, Math.min(count, pool.size()));

        return Collections.unmodifiableList(result);
    }

    /**
     * Looks up abilities by their unique ID from the global pool.
     *
     * @param ids the ability IDs to look up
     * @return list of matching abilities (skips unknown IDs)
     */
    public static List<Ability> getAbilitiesByIds(List<String> ids) {
        Set<String> idSet = new HashSet<>(ids);
        List<Ability> result = new ArrayList<>();
        for (Ability ability : ALL_ABILITIES) {
            if (idSet.contains(ability.id())) {
                result.add(ability);
            }
        }
        return result;
    }

    /**
     * Returns all registered ability IDs (e.g., "bane", "thorns", "rupture").
     * Useful for command tab-completions.
     */
    public static List<String> getAllAbilityIds() {
        return ALL_ABILITIES.stream()
                .map(Ability::id)
                .toList();
    }
}
