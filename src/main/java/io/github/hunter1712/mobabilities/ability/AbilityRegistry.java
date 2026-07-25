package io.github.hunter1712.mobabilities.ability;

import io.github.hunter1712.mobabilities.ability.effect.SplitEffect;
import io.github.hunter1712.mobabilities.trigger.DamageContext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;

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
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0)));

        all("freeze",   "Freeze",
                TriggerType.HURT, (mob, target) ->
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0)));

        all("decay",    "Decay",
                TriggerType.HURT, (mob, target) ->
                        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0)));

        all("inferno",  "Inferno",
                TriggerType.HURT, (mob, target) ->
                        target.igniteForSeconds(3));

        all("siphon",   "Siphon",
                TriggerType.HURT, (mob, target) -> {
                    float amount = DamageContext.getAndClear();
                    if (amount > 0) mob.heal(amount * 0.5f);
                });

        all("acid",     "Acid",
                TriggerType.HURT, AbilityRegistry::damageArmor);

        all("hex",      "Hex",
                TriggerType.HURT, (mob, target) ->
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0)));

        // ---- TICK abilities (passive, refresh every 2 seconds while alive) ----

        all("fortify",  "Fortify",
                TriggerType.TICK, (mob, target) ->
                        mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0)));

        all("fury",     "Fury",
                TriggerType.TICK, (mob, target) ->
                        mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 0)));

        all("gust",     "Gust",
                TriggerType.TICK, (mob, target) ->
                        mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0)));

        all("bloom",    "Bloom",
                TriggerType.TICK, (mob, target) ->
                        mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0)));

        // ---- DEATH abilities ----

        all("fission",  "Fission",
                TriggerType.DEATH, (mob, target) ->
                        SplitEffect.apply(mob));
    }

    /** Damages all 4 armor slots by 2 durability each. */
    private static void damageArmor(LivingEntity mob, LivingEntity target) {
        if (!(target instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        player.getItemBySlot(EquipmentSlot.HEAD).hurtAndBreak(2, level, player, item -> {});
        player.getItemBySlot(EquipmentSlot.CHEST).hurtAndBreak(2, level, player, item -> {});
        player.getItemBySlot(EquipmentSlot.LEGS).hurtAndBreak(2, level, player, item -> {});
        player.getItemBySlot(EquipmentSlot.FEET).hurtAndBreak(2, level, player, item -> {});
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
     * Returns {@code count} randomly selected abilities from the global pool.
     * Abilities are shuffled uniformly (no weights).
     *
     * @param count  how many abilities to pick
     * @return a shuffled, non-modifiable list of size {@code min(count, pool size)}
     */
    public static List<Ability> getRandomAbilities(int count) {
        List<Ability> pool = new ArrayList<>(ALL_ABILITIES);
        Collections.shuffle(pool, ThreadLocalRandom.current());
        return Collections.unmodifiableList(
                pool.subList(0, Math.min(count, pool.size())));
    }
}
