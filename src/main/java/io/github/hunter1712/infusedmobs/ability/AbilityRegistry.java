package io.github.hunter1712.infusedmobs.ability;

import io.github.hunter1712.infusedmobs.ability.effect.SplitEffect;
import io.github.hunter1712.infusedmobs.trigger.DamageContext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

/**
 * Central registry containing the global mob ability pool.
 * <p>
 * Abilities are stored in a flat list and randomly sampled when a mob
 * is assigned a tier on spawn. There is no per-mob-type registration.
 */
public final class AbilityRegistry {

    private static final float COMBUST_POWER = 4.0f;
    private static final List<Ability> ALL_ABILITIES = new ArrayList<>();

    private AbilityRegistry() {}

    // ========================================
    // Registration
    // ========================================

    /**
     * Populates the global ability pool. Must be called during mod init.
     * All abilities use vanilla effects — no custom status effects needed.
     */
    public static void registerAll() {
        // ---- HURT abilities (fire when the mob melee-hits a player) ----

        all("venom",    "Venom",
                TriggerType.HURT, (mob, target) ->
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1)));

        all("freeze",   "Freeze",
                TriggerType.HURT, (mob, target) ->
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 100, 2)));

        all("decay",    "Decay",
                TriggerType.HURT, (mob, target) ->
                        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 1)));

        all("inferno",  "Inferno",
                TriggerType.HURT, (mob, target) ->
                        target.igniteForSeconds(5));

        all("siphon",   "Siphon",
                TriggerType.HURT, (mob, target) -> {
                    float amount = DamageContext.getAndClear();
                    if (amount > 0) mob.heal(amount);
                });

        all("acid",     "Acid",
                TriggerType.HURT, AbilityRegistry::damageArmor);

        all("hex",      "Hex",
                TriggerType.HURT, (mob, target) ->
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1)));

        // ---- TICK abilities (passive, refresh every 2 seconds while alive) ----

        all("fortify",  "Fortify",
                TriggerType.TICK, (mob, target) ->
                        mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 60, 1)));

        all("fury",     "Fury",
                TriggerType.TICK, (mob, target) ->
                        mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 1)));

        all("gust",     "Gust",
                TriggerType.TICK, (mob, target) ->
                        mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 60, 1)));

        all("bloom",    "Bloom",
                TriggerType.TICK, (mob, target) ->
                        mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 60, 1)));

        // ---- DEATH abilities ----

        all("fission",  "Fission",
                TriggerType.DEATH, (mob, target) ->
                        SplitEffect.apply(mob));

        all("combust",  "Combust",
                TriggerType.DEATH, (mob, target) -> {
                    if (mob.level() instanceof ServerLevel level) {
                        level.explode(mob, mob.getX(), mob.getY(), mob.getZ(),
                                COMBUST_POWER, Level.ExplosionInteraction.MOB);
                    }
                });
    }

    /**
     * Damages all 4 armor slots by 4 durability each.
     * {@code mob} param unused — required by {@code BiConsumer} signature.
     */
    private static void damageArmor(LivingEntity mob, LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        player.getItemBySlot(EquipmentSlot.HEAD).hurtAndBreak(4, level, player, item -> {});
        player.getItemBySlot(EquipmentSlot.CHEST).hurtAndBreak(4, level, player, item -> {});
        player.getItemBySlot(EquipmentSlot.LEGS).hurtAndBreak(4, level, player, item -> {});
        player.getItemBySlot(EquipmentSlot.FEET).hurtAndBreak(4, level, player, item -> {});
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
     * Returns a fixed set of abilities with the specified distribution
     * of trigger types. Within each type, abilities are picked randomly.
     *
     * @return a shuffled, unmodifiable list
     */
    public static List<Ability> getRandomAbilities(int hurt, int tick, int death) {
        ThreadLocalRandom rng = ThreadLocalRandom.current();
        List<Ability> result = new ArrayList<>();

        pickRandom(result, TriggerType.HURT, hurt, rng);
        pickRandom(result, TriggerType.TICK, tick, rng);
        pickRandom(result, TriggerType.DEATH, death, rng);

        if (result.size() > 1) Collections.shuffle(result, rng);
        return Collections.unmodifiableList(result);
    }

    /** Picks {@code count} random abilities of the given trigger type into target. */
    private static void pickRandom(List<Ability> target, TriggerType type, int count, ThreadLocalRandom rng) {
        if (count <= 0) return;
        List<Ability> candidates = new ArrayList<>();
        for (Ability a : ALL_ABILITIES) {
            if (a.trigger() == type) candidates.add(a);
        }
        Collections.shuffle(candidates, rng);
        target.addAll(candidates.subList(0, Math.min(count, candidates.size())));
    }
}
