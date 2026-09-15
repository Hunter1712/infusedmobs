package io.github.hunter1712.infusedmobs.util;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.lang.reflect.Method;

/**
 * Version shim for ability effect handling on 1.20.1 (legacy raw MobEffect API).
 * Uses reflection so it compiles against 26.2's Holder-based mappings (workaround)
 * but at runtime on 1.20.1 correctly uses raw MobEffect and legacy hurtAndBreak.
 *
 * TODO: When 1.20.1 is compiled against its real mappings, replace reflection
 * with direct {@code new MobEffectInstance(MobEffect, duration, amplifier)} and
 * legacy {@code ItemStack.hurt(int, RandomSource, ServerPlayer)}.
 */
public final class AbilityHelper {
    private AbilityHelper() {}

    @SuppressWarnings("unchecked")
    public static void applyHurtEffect(LivingEntity target, Object effect, int duration, int amplifier) {
        MobEffectInstance instance = createInstance(effect, duration, amplifier, false);
        if (instance != null) target.addEffect(instance);
    }

    public static void applyTickEffect(LivingEntity mob, Object effect, int duration, int amplifier) {
        MobEffectInstance instance = createInstance(effect, duration, amplifier, true);
        if (instance != null) mob.addEffect(instance);
    }

    @SuppressWarnings("unchecked")
    private static MobEffectInstance createInstance(Object effect, int duration, int amplifier, boolean tick) {
        // Try modern Holder path first (26.2 workaround)
        try {
            Class<?> holderClass = Class.forName("net.minecraft.core.Holder");
            if (holderClass.isInstance(effect)) {
                if (tick) {
                    var ctor = MobEffectInstance.class.getConstructor(holderClass, int.class, int.class, boolean.class, boolean.class, boolean.class);
                    return (MobEffectInstance) ctor.newInstance(effect, duration, amplifier, false, false, false);
                } else {
                    var ctor = MobEffectInstance.class.getConstructor(holderClass, int.class, int.class);
                    return (MobEffectInstance) ctor.newInstance(effect, duration, amplifier);
                }
            }
        } catch (Exception ignored) {}
        // Legacy raw MobEffect path
        if (effect instanceof MobEffect mobEffect) {
            try {
                if (tick) {
                    var ctorLegacy = MobEffectInstance.class.getConstructor(MobEffect.class, int.class, int.class, boolean.class, boolean.class, boolean.class);
                    return (MobEffectInstance) ctorLegacy.newInstance(mobEffect, duration, amplifier, false, false, false);
                } else {
                    var ctorLegacy = MobEffectInstance.class.getConstructor(MobEffect.class, int.class, int.class);
                    return (MobEffectInstance) ctorLegacy.newInstance(mobEffect, duration, amplifier);
                }
            } catch (Exception ignored) {}
        }
        // Fallback: try direct holder cast
        try {
            if (tick) {
                return new MobEffectInstance((net.minecraft.core.Holder<MobEffect>) effect, duration, amplifier, false, false, false);
            } else {
                return new MobEffectInstance((net.minecraft.core.Holder<MobEffect>) effect, duration, amplifier);
            }
        } catch (Exception ignored2) {
            return null;
        }
    }

    public static void damageArmor(ServerPlayer player, ServerLevel level, int dmg) {
        // Try modern hurtAndBreak(ServerLevel, ServerPlayer, Consumer) first
        try {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                var stack = player.getItemBySlot(slot);
                // Modern: hurtAndBreak(int, ServerLevel, ServerPlayer, Consumer)
                try {
                    Method m = stack.getClass().getMethod("hurtAndBreak", int.class, ServerLevel.class, ServerPlayer.class, java.util.function.Consumer.class);
                    m.invoke(stack, dmg, level, player, (java.util.function.Consumer<net.minecraft.world.item.Item>) item -> {});
                    continue;
                } catch (NoSuchMethodException ignored) {}
                // Legacy 1.20.1: hurtAndBreak(int, RandomSource, ServerPlayer)
                try {
                    Method m2 = stack.getClass().getMethod("hurtAndBreak", int.class, net.minecraft.util.RandomSource.class, ServerPlayer.class);
                    m2.invoke(stack, dmg, player.getRandom(), player);
                    continue;
                } catch (NoSuchMethodException ignored2) {}
                // Fallback
                stack.hurtAndBreak(dmg, level, player, item -> {});
            }
        } catch (Exception e) {
            for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
                try { player.getItemBySlot(slot).hurtAndBreak(dmg, level, player, item -> {}); } catch (Exception ignored) {}
            }
        }
    }

    public static void reflectThorns(net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.Mob mob, float reflected, ServerLevel level) {
        try {
            // Modern: hurtServer(ServerLevel, DamageSource, float)
            Method m = player.getClass().getMethod("hurtServer", ServerLevel.class, net.minecraft.world.damagesource.DamageSource.class, float.class);
            Object dmgSource = player.damageSources().thorns(mob);
            m.invoke(player, level, dmgSource, reflected);
            return;
        } catch (Exception ignored) {}
        try {
            // Legacy: hurt(DamageSource, float)
            Method m2 = player.getClass().getMethod("hurt", net.minecraft.world.damagesource.DamageSource.class, float.class);
            Object dmgSource = player.damageSources().thorns(mob);
            m2.invoke(player, dmgSource, reflected);
        } catch (Exception ignored2) {}
    }

    public static void hurtFromExplosion(LivingEntity living, ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        try {
            Method m = living.getClass().getMethod("hurtServer", ServerLevel.class, net.minecraft.world.damagesource.DamageSource.class, float.class);
            m.invoke(living, level, source, amount);
            return;
        } catch (Exception ignored) {}
        try {
            Method m2 = living.getClass().getMethod("hurt", net.minecraft.world.damagesource.DamageSource.class, float.class);
            m2.invoke(living, source, amount);
        } catch (Exception ignored2) {}
    }

    // ========================================
    // MobEffect accessors (compile against 26.2 names; 1.20.1 runtime
    // resolves via reflection in createInstance above)
    // ========================================

    public static net.minecraft.core.Holder<MobEffect> slowness() {
        return net.minecraft.world.effect.MobEffects.SLOWNESS;
    }

    public static net.minecraft.core.Holder<MobEffect> resistance() {
        return net.minecraft.world.effect.MobEffects.RESISTANCE;
    }

    public static net.minecraft.core.Holder<MobEffect> strength() {
        return net.minecraft.world.effect.MobEffects.STRENGTH;
    }

    public static net.minecraft.core.Holder<MobEffect> speed() {
        return net.minecraft.world.effect.MobEffects.SPEED;
    }
}
