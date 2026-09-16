package io.github.hunter1712.infusedmobs.util;

import io.github.hunter1712.infusedmobs.ability.EffectToken;

import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Version shim for ability effect handling (26.2 — modern Holder API).
 * Handles effect holders vs raw effects, damage sources and item handling
 * without diverging combat behaviour.
 * <p>
 * Effect handles are opaque {@link EffectToken}s: 26.2/1.21.1 wrap
 * {@code Holder<MobEffect>} while 1.20.1 wraps raw {@code MobEffect}, and
 * shared sources pass tokens through without inspecting them.
 */
public final class AbilityHelper {
    private AbilityHelper() {}

    @SuppressWarnings("unchecked")
    public static void applyHurtEffect(LivingEntity target, EffectToken effect, int duration, int amplifier) {
        target.addEffect(new MobEffectInstance((Holder<MobEffect>) effect.handle(), duration, amplifier));
    }

    @SuppressWarnings("unchecked")
    public static void applyTickEffect(LivingEntity mob, EffectToken effect, int duration, int amplifier) {
        mob.addEffect(new MobEffectInstance((Holder<MobEffect>) effect.handle(), duration, amplifier, false, false, false));
    }

    public static void damageArmor(ServerPlayer player, ServerLevel level, int dmg) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            player.getItemBySlot(slot).hurtAndBreak(dmg, level, player, item -> {});
        }
    }

    public static void reflectThorns(net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.Mob mob, float reflected, ServerLevel level) {
        if (player instanceof ServerPlayer sp) {
            sp.hurtServer(level, sp.damageSources().thorns(mob), reflected);
        } else {
            player.hurtServer(level, player.damageSources().thorns(mob), reflected);
        }
    }

    public static void hurtFromExplosion(LivingEntity living, ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        living.hurtServer(level, source, amount);
    }

    public static void ignite(LivingEntity target, int seconds) {
        target.igniteForSeconds(seconds);
    }

    // ========================================
    // Effect handles (26.2 names)
    // ========================================

    public static EffectToken slowness() { return EffectToken.of(MobEffects.SLOWNESS); }

    public static EffectToken resistance() { return EffectToken.of(MobEffects.RESISTANCE); }

    public static EffectToken strength() { return EffectToken.of(MobEffects.STRENGTH); }

    public static EffectToken speed() { return EffectToken.of(MobEffects.SPEED); }

    public static EffectToken poison() { return EffectToken.of(MobEffects.POISON); }

    public static EffectToken wither() { return EffectToken.of(MobEffects.WITHER); }

    public static EffectToken weakness() { return EffectToken.of(MobEffects.WEAKNESS); }

    public static EffectToken regeneration() { return EffectToken.of(MobEffects.REGENERATION); }
}
