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
 * Version shim for ability effect handling (1.21.1 — Holder API with legacy hurt).
 * Effect holders match 26.2, but damage uses {@code hurt(DamageSource, float)}
 * — 1.21.1 has no {@code hurtServer(ServerLevel, DamageSource, float)}.
 * Four {@code MobEffects} fields were renamed in 26.x, so common code reads
 * them through the accessors below.
 */
public final class AbilityHelper {
    private AbilityHelper() {}

    public static void applyHurtEffect(LivingEntity target, Holder<MobEffect> effect, int duration, int amplifier) {
        target.addEffect(new MobEffectInstance(effect, duration, amplifier));
    }

    public static void applyTickEffect(LivingEntity mob, Holder<MobEffect> effect, int duration, int amplifier) {
        mob.addEffect(new MobEffectInstance(effect, duration, amplifier, false, false, false));
    }

    public static void damageArmor(ServerPlayer player, ServerLevel level, int dmg) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            player.getItemBySlot(slot).hurtAndBreak(dmg, level, player, item -> {});
        }
    }

    public static void reflectThorns(net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.Mob mob, float reflected, ServerLevel level) {
        player.hurt(player.damageSources().thorns(mob), reflected);
    }

    public static void hurtFromExplosion(LivingEntity living, ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        living.hurt(source, amount);
    }

    // ========================================
    // MobEffect renames (1.21.1 MojMap names)
    // ========================================

    public static Holder<MobEffect> slowness() { return MobEffects.MOVEMENT_SLOWDOWN; }

    public static Holder<MobEffect> resistance() { return MobEffects.DAMAGE_RESISTANCE; }

    public static Holder<MobEffect> strength() { return MobEffects.DAMAGE_BOOST; }

    public static Holder<MobEffect> speed() { return MobEffects.MOVEMENT_SPEED; }
}
