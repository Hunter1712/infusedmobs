package io.github.hunter1712.mobabilities.ability.trigger;

import io.github.hunter1712.mobabilities.ability.Ability;
import io.github.hunter1712.mobabilities.ability.AbilityRegistry;
import io.github.hunter1712.mobabilities.ability.TriggerType;
import io.github.hunter1712.mobabilities.effect.ModEffects;
import io.github.hunter1712.mobabilities.MobAbilitiesMod;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.AreaEffectCloud;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.Level;

import java.util.List;
import java.util.Optional;

/**
 * Handles the {@link TriggerType#DEATH DEATH} trigger for hostile mobs.
 *
 * <p>Listens for {@link ServerLivingEntityEvents#AFTER_DEATH}, filters
 * for {@link Monster} entities, queries the ability registry for
 * DEATH-triggered abilities, selects one at random (weighted), and
 * executes its effect logic with the killer as the second argument.
 *
 * <p>Register by calling {@link #register()} from the mod initialiser.
 */
public final class MobDeathTrigger {

    private MobDeathTrigger() {
        // static-only utility class
    }

    /**
     * Registers the {@code AFTER_DEATH} event handler.
     *
     * <p>For each dying {@link Monster} the handler:
     * <ol>
     *   <li>Queries {@link AbilityRegistry#getAbilitiesForMob} for
     *       DEATH-triggered abilities (logged for debugging),
     *   <li>Selects one via {@link AbilityRegistry#selectRandomAbility},
     *   <li>Resolves the killer as the target, and
     *   <li>Invokes the ability's {@code effectLogic} bi-consumer.
     * </ol>
     */
    public static void register() {
        ServerLivingEntityEvents.AFTER_DEATH.register((LivingEntity entity, DamageSource source) -> {
            // Only process hostile mobs (Monster is the base class for
            // hostile creatures such as Zombie, Creeper, Skeleton, etc.)
            if (!(entity instanceof Monster mob)) {
                return;
            }

            // 1. Query available DEATH abilities (for debugging / logging)
            List<Ability> abilities =
                    AbilityRegistry.getAbilitiesForMob(mob, TriggerType.DEATH);
            if (abilities.isEmpty()) {
                return;
            }

            if (MobAbilitiesMod.LOGGER.isDebugEnabled()) {
                MobAbilitiesMod.LOGGER.debug(
                        "MobDeathTrigger: {} has {} death ability candidates",
                        BuiltInRegistries.ENTITY_TYPE.getKey(mob.getType()),
                        abilities.size());
            }

            // 2. Weighted-random selection of one DEATH ability
            Optional<Ability> selected =
                    AbilityRegistry.selectRandomAbility(mob, TriggerType.DEATH);
            if (selected.isEmpty()) {
                return;
            }

            // 3. Resolve the killer as the target (may be null if the
            //    death was caused by environment damage, e.g. falling)
            LivingEntity target = source.getEntity() instanceof LivingEntity living
                    ? living
                    : null;

            // 4. Execute the registered effect logic
            selected.get().effectLogic().accept(mob, target);
        });
    }

    // ============================================================
    // Static effect implementations
    //
    // These are the concrete BiConsumer bodies that should replace
    // the corresponding TODO stubs in AbilityRegistry.registerAll().
    //
    // Each implementation follows the convention:
    //   (LivingEntity mob, LivingEntity target) -> { effect body }
    // ============================================================

    /**
     * Corrosive Splash — creates an {@link AreaEffectCloud} at the
     * creeper's death position that applies {@link ModEffects#ACID}
     * to any entity inside the cloud.
     *
     * <p>Intended for {@link EntityTypes#CREEPER}.
     *
     * @param mob   the dying mob (must be a Creeper)
     * @param target the killer (may be {@code null})
     */
    static void applyCorrosiveSplash(LivingEntity mob, LivingEntity target) {
        Level level = mob.level();
        AreaEffectCloud cloud = new AreaEffectCloud(level,
                mob.getX(), mob.getY(), mob.getZ());
        cloud.setRadius(3.0f);
        cloud.setRadiusOnUse(0.5f);
        cloud.setWaitTime(10);
        cloud.setDuration(200);            // 10 seconds
        cloud.setRadiusPerTick(-0.005f);
        cloud.addEffect(new MobEffectInstance(ModEffects.ACID, 100, 0));
        level.addFreshEntity(cloud);
    }

    /**
     * Split — spawns two smaller copies of the same {@link EntityType}
     * at 50 % of the original's maximum health with a small random
     * offset to prevent overlap.
     *
     * <p>Applies to any {@link Monster}.
     *
     * @param mob   the dying mob
     * @param target the killer (may be {@code null})
     */
    static void applySplit(LivingEntity mob, LivingEntity target) {
        // TODO: Implement in Batch 3 — spawn 2 copies of same EntityType at 50% HP
        // EntityType<?> type = mob.getType();
        // for (int i = 0; i < 2; i++) { ... type.spawn(level, ...) ... }
    }

    /**
     * Horde Caller — spawns 2–3 {@link Zombie} reinforcements at 50 %
     * health around the death location.
     *
     * <p>Intended for {@link EntityTypes#ZOMBIE}.
     *
     * @param mob   the dying mob (must be a Zombie)
     * @param target the killer (may be {@code null})
     */
    static void applyHordeCaller(LivingEntity mob, LivingEntity target) {
        // TODO: Implement in Batch 3 — spawn 2-3 Zombies at 50% HP
        // Level level = mob.level();
        // for (int i = 0; i < count; i++) { ... EntityTypes.ZOMBIE.spawn(...) ... }
    }
}
