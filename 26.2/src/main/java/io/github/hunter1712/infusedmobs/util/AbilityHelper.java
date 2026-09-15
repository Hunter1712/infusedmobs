package io.github.hunter1712.infusedmobs.util;

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
 * Effect handles are deliberately {@code Object}: 26.2/1.21.1 pass
 * {@code Holder<MobEffect>} while 1.20.1 passes raw {@code MobEffect}, and
 * the shared {@code common} sources must compile against both. Each handle
 * comes from the accessors below and is cast back here.
 */
public final class AbilityHelper {
    private AbilityHelper() {}

    @SuppressWarnings("unchecked")
    public static void applyHurtEffect(LivingEntity target, Object effect, int duration, int amplifier) {
        target.addEffect(new MobEffectInstance((Holder<MobEffect>) effect, duration, amplifier));
    }

    @SuppressWarnings("unchecked")
    public static void applyTickEffect(LivingEntity mob, Object effect, int duration, int amplifier) {
        mob.addEffect(new MobEffectInstance((Holder<MobEffect>) effect, duration, amplifier, false, false, false));
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

    public static Object slowness() { return MobEffects.SLOWNESS; }

    public static Object resistance() { return MobEffects.RESISTANCE; }

    public static Object strength() { return MobEffects.STRENGTH; }

    public static Object speed() { return MobEffects.SPEED; }

    public static Object poison() { return MobEffects.POISON; }

    public static Object wither() { return MobEffects.WITHER; }

    public static Object weakness() { return MobEffects.WEAKNESS; }

    public static Object regeneration() { return MobEffects.REGENERATION; }
}
