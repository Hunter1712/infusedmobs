package io.github.hunter1712.mobabilities.ability.trigger;

import io.github.hunter1712.mobabilities.ability.Ability;
import io.github.hunter1712.mobabilities.ability.AbilityRegistry;
import io.github.hunter1712.mobabilities.ability.TriggerType;

import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.AABB;

import java.util.List;

/**
 * Handles per-tick ability effects for hostile mobs.
 *
 * <p>Registered via {@link ServerTickEvents#END_SERVER_TICK} and executes
 * every 20 ticks (~1 second). For each loaded hostile mob in every server
 * level, it queries {@link AbilityRegistry} for {@link TriggerType#TICK}
 * abilities and applies the corresponding effect logic.
 *
 * <p>Some TICK abilities are re-implemented here for fine-grained control
 * (Frenzy, Berserker, Bulwark, Bannerman, Corrupted Presence, Pack Leader,
 * Regenerator). Others fall through to the registry's {@code effectLogic}
 * BiConsumers.
 */
public final class MobTickTrigger {

    private static int tickCounter = 0;

    private MobTickTrigger() {
        // static-only utility class
    }

    // ========================================
    // Registration
    // ========================================

    /**
     * Registers the end-server-tick event handler that applies TICK
     * abilities to all loaded hostile mobs every 20 ticks (~1 second).
     */
    public static void register() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            // Modular counter — wraps at 20 to avoid int overflow
            tickCounter = (tickCounter + 1) % 20;
            if (tickCounter != 0) return;

            for (ServerLevel level : server.getAllLevels()) {
                // Use type-filtered entity lookup to avoid scanning
                // dropped items, XP orbs, projectiles, etc.
                for (Entity entity : level.getAllEntities()) {
                    if (entity instanceof Monster monster && monster.isAlive()) {
                        processMobTickAbilities(monster);
                    }
                }
            }
        });
    }

    // ========================================
    // Ability dispatch
    // ========================================

    /**
     * Queries the registry for {@link TriggerType#TICK} abilities on this
     * mob and dispatches to the appropriate effect method.
     *
     * <p>Effects that need specific logic (HP thresholds, aura ranges) are
     * handled here. Generic effects fall through to the registry's
     * {@code effectLogic} BiConsumer with a null target.
     */
    private static void processMobTickAbilities(Mob mob) {
        List<Ability> abilities = AbilityRegistry.getAbilitiesForMob(mob, TriggerType.TICK);
        for (Ability ability : abilities) {
            switch (ability.name()) {
                case "Frenzy"              -> applyFrenzy(mob);
                case "Berserker"           -> applyBerserker(mob);
                case "Bulwark"             -> applyBulwark(mob);
                case "Bannerman"           -> applyBannerman(mob);
                case "Corrupted Presence"  -> applyCorruptedPresence(mob);
                case "Pack Leader"         -> applyPackLeader(mob);
                case "Regenerator"         -> applyRegenerator(mob);
                case "Shadowstep"          -> applyShadowstep(mob);
                case "Bone Armor"          -> applyBoneArmor(mob);
                default                    -> ability.effectLogic().accept(mob, null);
            }
        }
    }

    // ============================================================
    // Per-ability effect implementations
    // ============================================================

    /**
     * Frenzy: grants {@link MobEffects#MOVEMENT_SPEED} when the mob's
     * health drops below 50% of its maximum. Amplifier is 1 when health
     * is below 25%, otherwise 0. Duration: 40 ticks (2 seconds).
     */
    private static void applyFrenzy(Mob mob) {
        float healthRatio = mob.getHealth() / mob.getMaxHealth();
        if (healthRatio < 0.5f) {
            int amplifier = healthRatio < 0.25f ? 1 : 0;
            mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, amplifier));
        }
    }

    /**
     * Berserker: grants {@link MobEffects#STRENGTH} when the mob's health
     * drops below 50% of its maximum. Amplifier is 1 when health is below
     * 25%, otherwise 0. Duration: 40 ticks (2 seconds).
     */
    private static void applyBerserker(Mob mob) {
        float healthRatio = mob.getHealth() / mob.getMaxHealth();
        if (healthRatio < 0.5f) {
            int amplifier = healthRatio < 0.25f ? 1 : 0;
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, amplifier));
        }
    }

    /**
     * Bulwark (Zombie): grants {@link MobEffects#RESISTANCE} when at least
     * two other zombies are within 8 blocks of this mob. Duration: 40 ticks
     * (2 seconds).
     */
    private static void applyBulwark(Mob mob) {
        if (mob.getType() != EntityTypes.ZOMBIE) {
            return;
        }

        AABB area = mob.getBoundingBox().inflate(8.0);
        List<Mob> nearby = mob.level().getEntitiesOfClass(Mob.class, area,
                e -> e != mob && e.getType() == EntityTypes.ZOMBIE);

        if (nearby.size() >= 2) {
            mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0));
        }
    }

    /**
     * Bannerman (Pillager): grants {@link MobEffects#STRENGTH} to all
     * pillagers within 15 blocks. Duration: 60 ticks (3 seconds).
     */
    private static void applyBannerman(Mob mob) {
        if (mob.getType() != EntityTypes.PILLAGER) {
            return;
        }

        AABB area = mob.getBoundingBox().inflate(15.0);
        mob.level().getEntitiesOfClass(Mob.class, area,
                        e -> e.getType() == EntityTypes.PILLAGER && e != mob)
                .forEach(p -> p.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 60, 0)));
    }

    /**
     * Corrupted Presence: applies {@link MobEffects#WEAKNESS} to all
     * players within 10 blocks. Duration: 100 ticks (5 seconds).
     */
    private static void applyCorruptedPresence(Mob mob) {
        AABB area = mob.getBoundingBox().inflate(10.0);
        mob.level().getEntitiesOfClass(Player.class, area)
                .forEach(p -> p.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0)));
    }

    /**
     * Pack Leader: grants {@link MobEffects#STRENGTH} to all mobs of the
     * same entity type within 10 blocks. Duration: 40 ticks (2 seconds).
     */
    private static void applyPackLeader(Mob mob) {
        AABB area = mob.getBoundingBox().inflate(10.0);
        mob.level().getEntitiesOfClass(Mob.class, area,
                        e -> e.getType() == mob.getType() && e != mob)
                .forEach(m -> m.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 0)));
    }

    /**
     * Regenerator: grants {@link MobEffects#REGENERATION} directly to the
     * mob. Duration: 40 ticks (2 seconds).
     */
    private static void applyRegenerator(Mob mob) {
        mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0));
    }

    /**
     * Shadowstep: grants {@link MobEffects#DAMAGE_RESISTANCE} as a passive
     * buff while the mob is alive. The actual dodge-teleport mechanic is
     * handled in {@link io.github.hunter1712.mobabilities.mixin.LivingEntityMixin}.
     * Duration: 40 ticks (2 seconds).
     */
    private static void applyShadowstep(Mob mob) {
        mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0));
    }

    /**
     * Bone Armor: grants {@link MobEffects#RESISTANCE} while alive.
     * Duration: 40 ticks (2 seconds).
     */
    private static void applyBoneArmor(Mob mob) {
        mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0));
    }
}
