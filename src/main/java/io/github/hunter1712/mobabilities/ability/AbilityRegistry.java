package io.github.hunter1712.mobabilities.ability;

import io.github.hunter1712.mobabilities.ability.effect.SplitEffect;
import io.github.hunter1712.mobabilities.trigger.DamageContext;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiConsumer;

/**
 * Central registry containing all 12 global mob abilities.
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
     * All 12 abilities use vanilla effects — no custom status effects needed.
     */
    public static void registerAll() {
        // ---- HURT abilities (fire when the mob melee-hits a player) ----

        all("blight",    "Blight",
                TriggerType.HURT, (mob, target) ->
                        target.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0)));

        all("shackle",   "Shackle",
                TriggerType.HURT, (mob, target) ->
                        target.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 60, 0)));

        all("rot",       "Rot",
                TriggerType.HURT, (mob, target) ->
                        target.addEffect(new MobEffectInstance(MobEffects.WITHER, 60, 0)));

        all("hellfire",  "Hellfire",
                TriggerType.HURT, (mob, target) ->
                        target.igniteForSeconds(3));

        all("drain",     "Drain",
                TriggerType.HURT, (mob, target) -> {
                    float amount = DamageContext.getAndClear();
                    if (amount > 0) mob.heal(amount * 0.5f);
                });

        all("corrode",   "Corrode",
                TriggerType.HURT, (mob, target) -> {
                    if (target instanceof ServerPlayer player && player.level() instanceof ServerLevel level) {
                        player.getItemBySlot(EquipmentSlot.HEAD).hurtAndBreak(2, level, player, item -> {});
                        player.getItemBySlot(EquipmentSlot.CHEST).hurtAndBreak(2, level, player, item -> {});
                        player.getItemBySlot(EquipmentSlot.LEGS).hurtAndBreak(2, level, player, item -> {});
                        player.getItemBySlot(EquipmentSlot.FEET).hurtAndBreak(2, level, player, item -> {});
                    }
                });

        all("cripple",   "Cripple",
                TriggerType.HURT, (mob, target) ->
                        target.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, 0)));

        // ---- TICK abilities (passive, refresh every 2 seconds while alive) ----

        all("bone",      "Bone",
                TriggerType.TICK, (mob, target) ->
                        mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0)));

        all("wrath",     "Wrath",
                TriggerType.TICK, (mob, target) ->
                        mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 0)));

        all("haste",     "Haste",
                TriggerType.TICK, (mob, target) ->
                        mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, 0)));

        all("flesh",     "Flesh",
                TriggerType.TICK, (mob, target) ->
                        mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0)));

        // ---- DEATH abilities ----

        all("rend",      "Rend",
                TriggerType.DEATH, (mob, target) -> {
                    if (mob.level() instanceof ServerLevel serverLevel) {
                        SplitEffect.apply(mob, serverLevel);
                    }
                });
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
