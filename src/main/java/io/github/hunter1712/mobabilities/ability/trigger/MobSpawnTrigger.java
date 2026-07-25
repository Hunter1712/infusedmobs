package io.github.hunter1712.mobabilities.ability.trigger;

import io.github.hunter1712.mobabilities.ability.Ability;
import io.github.hunter1712.mobabilities.ability.AbilityRegistry;
import io.github.hunter1712.mobabilities.ability.TriggerType;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * Handles ability activation when a hostile mob spawns into the world.
 *
 * <p>Called from a mixin on {@code Mob#finalizeSpawn} when a fresh hostile
 * mob is created.  Immediate potion effects (Regeneration, Resistance, etc.)
 * are applied here with spawn-appropriate durations, while persistent state
 * (e.g., Thorns tracking) is recorded for use by other event handlers.
 */
public final class MobSpawnTrigger {

    // ========================================
    // Thorns tracking
    // ========================================

    private static final Set<UUID> THORNS_MOBS = new HashSet<>();

    public static Predicate<LivingEntity> hasThorns() {
        return entity -> THORNS_MOBS.contains(entity.getUUID());
    }

    // ========================================
    // Entry point called from mixin
    // ========================================

    /**
     * Called from a mixin when a {@link Monster} spawns into the world.
     *
     * @param mob   the freshly-spawned mob
     * @param level the server level
     */
    public static void onMobSpawn(Mob mob, ServerLevel level) {
        if (!(mob instanceof Monster)) return;

        AbilityRegistry.selectRandomAbility(mob, TriggerType.SPAWN)
                .ifPresent(ability -> {
                    ability.effectLogic().accept(mob, null);
                    applyImmediateSpawnEffects(mob, level, ability);
                });
    }

    // ========================================
    // Immediate effect application
    // ========================================

    private static void applyImmediateSpawnEffects(Mob mob, ServerLevel level, Ability ability) {
        switch (ability.name()) {
            case "Regenerator" -> {
                mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 600, 0));
            }
            case "Corrupted Presence" -> {
                level.players().forEach(player ->
                        player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 200, 0)));
            }
            case "Thorns" -> {
                THORNS_MOBS.add(mob.getUUID());
            }
            case "Bone Armor" -> {
                mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 600, 0));
            }
            case "Bannerman" -> {
                mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 600, 0));
            }
            default -> {}
        }
    }

    // ========================================
    // Lifecycle hooks (called from mod init)
    // ========================================

    /**
     * Registers event handlers for cleanup logic. Currently hooks the
     * death event to remove Thorns mob entries, preventing unbounded
     * growth of the {@link #THORNS_MOBS} set.
     */
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((entity, source) -> {
            THORNS_MOBS.remove(entity.getUUID());
        });
    }

    private MobSpawnTrigger() {}
}
