package io.github.hunter1712.infusedmobs.util;

import io.github.hunter1712.infusedmobs.ability.EffectToken;

import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

/**
 * Version shim for ability effect handling (1.20.1 — raw MobEffect API).
 * 1.20.1 has no {@code Holder}-based {@code MobEffectInstance} constructors
 * and no {@code hurtServer(ServerLevel, DamageSource, float)}: damage uses
 * {@code hurt(DamageSource, float)}, armour uses the generic
 * {@code hurtAndBreak(int, LivingEntity, Consumer)}, and igniting uses
 * {@code setSecondsOnFire(int)}.
 * <p>
 * Effect handles are opaque {@link EffectToken}s wrapping raw effects here.
 */
public final class AbilityHelper {
    private AbilityHelper() {}

    public static void applyHurtEffect(LivingEntity target, EffectToken effect, int duration, int amplifier) {
        target.addEffect(new MobEffectInstance((MobEffect) effect.handle(), duration, amplifier));
    }

    public static void applyTickEffect(LivingEntity mob, EffectToken effect, int duration, int amplifier) {
        mob.addEffect(new MobEffectInstance((MobEffect) effect.handle(), duration, amplifier, false, false, false));
    }

    public static void damageArmor(ServerPlayer player, ServerLevel level, int dmg) {
        for (EquipmentSlot slot : new EquipmentSlot[]{EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET}) {
            player.getItemBySlot(slot).hurtAndBreak(dmg, player, item -> {});
        }
    }

    public static void reflectThorns(net.minecraft.world.entity.player.Player player, net.minecraft.world.entity.Mob mob, float reflected, ServerLevel level) {
        player.hurt(player.damageSources().thorns(mob), reflected);
    }

    public static void hurtFromExplosion(LivingEntity living, ServerLevel level, net.minecraft.world.damagesource.DamageSource source, float amount) {
        living.hurt(source, amount);
    }

    public static void ignite(LivingEntity target, int seconds) {
        target.setSecondsOnFire(seconds);
    }

    // ========================================
    // Effect handles (1.20.1 MojMap names — same renames as 1.21.1)
    // ========================================

    public static EffectToken slowness() { return EffectToken.of(MobEffects.MOVEMENT_SLOWDOWN); }

    public static EffectToken resistance() { return EffectToken.of(MobEffects.DAMAGE_RESISTANCE); }

    public static EffectToken strength() { return EffectToken.of(MobEffects.DAMAGE_BOOST); }

    public static EffectToken speed() { return EffectToken.of(MobEffects.MOVEMENT_SPEED); }

    public static EffectToken poison() { return EffectToken.of(MobEffects.POISON); }

    public static EffectToken wither() { return EffectToken.of(MobEffects.WITHER); }

    public static EffectToken weakness() { return EffectToken.of(MobEffects.WEAKNESS); }

    public static EffectToken regeneration() { return EffectToken.of(MobEffects.REGENERATION); }
}
