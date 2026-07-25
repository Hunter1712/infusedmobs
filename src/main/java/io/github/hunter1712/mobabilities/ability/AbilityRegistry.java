package io.github.hunter1712.mobabilities.ability;

import io.github.hunter1712.mobabilities.effect.ModEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;

import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Predicate;

/**
 * Central static registry that maps mob {@link EntityType entity types}
 * to their possible {@link Ability abilities} with configurable selection
 * weights.
 *
 * <p>Provides weighted-random selection and trigger-based filtering
 * for runtime ability resolution.
 */
public final class AbilityRegistry {

    /** Storage: entity type → list of weighted abilities. */
    private static final Map<EntityType<?>, List<WeightedAbility>> REGISTRY = new HashMap<>();

    private AbilityRegistry() {
        // static-only utility class
    }

    // ========================================
    // Registration
    // ========================================

    /**
     * Registers a weighted ability for the given mob type.
     *
     * @param mobType the entity type to associate the ability with
     * @param ability the ability to register
     * @param weight  relative selection weight (higher = more likely)
     */
    public static void registerAbility(EntityType<?> mobType, Ability ability, int weight) {
        REGISTRY.computeIfAbsent(mobType, k -> new ArrayList<>())
                .add(new WeightedAbility(ability, weight));
    }

    // ========================================
    // Query methods
    // ========================================

    /**
     * Returns all abilities registered for the given mob that match the
     * specified trigger type.
     *
     * @param mob    the living entity to query for
     * @param trigger the trigger type to filter by
     * @return list of matching abilities (never {@code null})
     */
    public static List<Ability> getAbilitiesForMob(LivingEntity mob, TriggerType trigger) {
        List<WeightedAbility> weighted = REGISTRY.getOrDefault(mob.getType(), List.of());
        return weighted.stream()
                .map(WeightedAbility::ability)
                .filter(a -> a.trigger() == trigger && a.mobPredicate().test(mob))
                .toList();
    }

    /**
     * Performs weighted-random selection of an ability for the given mob
     * and trigger type. Weights are relative — an ability with weight 20
     * is twice as likely as one with weight 10.
     *
     * @param mob    the living entity to select for
     * @param trigger the trigger type to filter by
     * @return an {@link Optional} containing the selected ability, or empty
     *         if no abilities match
     */
    public static Optional<Ability> selectRandomAbility(LivingEntity mob, TriggerType trigger) {
        List<WeightedAbility> weighted = REGISTRY.getOrDefault(mob.getType(), List.of());

        // Filter by trigger and mob predicate
        List<WeightedAbility> candidates = weighted.stream()
                .filter(wa -> wa.ability().trigger() == trigger
                        && wa.ability().mobPredicate().test(mob))
                .toList();

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        // Weighted random selection: sum weights, pick random, subtract
        int totalWeight = candidates.stream().mapToInt(WeightedAbility::weight).sum();
        int remaining = new Random().nextInt(totalWeight);

        for (WeightedAbility wa : candidates) {
            remaining -= wa.weight();
            if (remaining < 0) {
                return Optional.of(wa.ability());
            }
        }

        // Fallback (shouldn't normally reach here)
        return Optional.of(candidates.getLast().ability());
    }

    // ========================================
    // Registration of all 23 abilities
    // ========================================

    /**
     * Populates the registry with all 23 mob abilities.
     *
     * <p>Each ability is registered under one or more {@link EntityType mob
     * types} with a default weight of 10. Abilities that apply to multiple
     * mob types use a broad {@code mob instanceof Monster} predicate;
     * type-specific abilities check the exact entity type.
     *
     * <p><b>Must be called during mod initialisation.</b>
     */
    public static void registerAll() {
        // ---- Shared predicates ----

        Predicate<LivingEntity> IS_ZOMBIE   = m -> m.getType() == EntityTypes.ZOMBIE;
        Predicate<LivingEntity> IS_SKELETON = m -> m.getType() == EntityTypes.SKELETON;
        Predicate<LivingEntity> IS_SPIDER   = m -> m.getType() == EntityTypes.SPIDER;
        Predicate<LivingEntity> IS_CREEPER  = m -> m.getType() == EntityTypes.CREEPER;
        Predicate<LivingEntity> IS_PILLAGER = m -> m.getType() == EntityTypes.PILLAGER;
        Predicate<LivingEntity> IS_ENDERMAN = m -> m.getType() == EntityTypes.ENDERMAN;

        // Broad predicate for abilities shared across multiple mob types
        Predicate<LivingEntity> IS_MONSTER = m -> m instanceof Monster;

        // ---- Convenience helper for compact registration ----

        BiConsumer<EntityType<?>, Ability> reg = (type, ability) ->
                registerAbility(type, ability, 10);

        // ==================================================================
        // EFFECT LOGIC DEFINITIONS
        // ==================================================================

        // ---- OFFENSIVE (HURT / PROJECTILE_HIT) ----

        BiConsumer<LivingEntity, LivingEntity> PLAGUE_BEARER_EFFECT = (mob, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
            target.addEffect(new MobEffectInstance(MobEffects.HUNGER, 300, 1));
        };

        BiConsumer<LivingEntity, LivingEntity> VENOMOUS_BITE_EFFECT = (mob, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.POISON, 200, 0));
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
        };

        BiConsumer<LivingEntity, LivingEntity> LIFESTRIKE_EFFECT = (mob, target) -> {
            // TODO: implemented in LifestrikeEffect — placeholder healing
            target.addEffect(new MobEffectInstance(MobEffects.INSTANT_HEALTH, 1, 0));
        };

        BiConsumer<LivingEntity, LivingEntity> CURSED_WOUND_EFFECT = (mob, target) -> {
            target.addEffect(new MobEffectInstance(ModEffects.CURSED_WOUND, 200, 0));
        };

        BiConsumer<LivingEntity, LivingEntity> SHIELD_BREAKER_EFFECT = (mob, target) -> {
            // TODO: shield disabling logic
        };

        BiConsumer<LivingEntity, LivingEntity> MIND_SHATTER_EFFECT = (mob, target) -> {
            // TODO: confusion / disorientation effect
        };

        BiConsumer<LivingEntity, LivingEntity> RUST_EFFECT = (mob, target) -> {
            // TODO: durability damage to target's equipment
        };

        BiConsumer<LivingEntity, LivingEntity> DISARM_EFFECT = (mob, target) -> {
            // TODO: force target to drop held item
        };

        BiConsumer<LivingEntity, LivingEntity> CORROSIVE_SPLASH_EFFECT = (mob, target) -> {
            // TODO: acid pool on the ground
        };

        // ---- DEATH triggers ----

        BiConsumer<LivingEntity, LivingEntity> SPLIT_EFFECT = (mob, target) -> {
            // TODO: entity splitting into smaller copies
        };

        BiConsumer<LivingEntity, LivingEntity> HORDE_CALLER_EFFECT = (mob, target) -> {
            // TODO: spawn zombie reinforcements
        };

        // ---- DEFENSIVE / PASSIVE (TICK) ----

        BiConsumer<LivingEntity, LivingEntity> BULWARK_EFFECT = (mob, target) -> {
            mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0));
        };

        BiConsumer<LivingEntity, LivingEntity> BANNERMAN_EFFECT = (mob, target) -> {
            mob.level().getEntitiesOfClass(LivingEntity.class,
                            mob.getBoundingBox().inflate(10.0),
                            e -> e.getType() == mob.getType() && e != mob)
                    .forEach(e -> e.addEffect(
                            new MobEffectInstance(MobEffects.STRENGTH, 40, 0)));
        };

        BiConsumer<LivingEntity, LivingEntity> CORRUPTED_PRESENCE_EFFECT = (mob, target) -> {
            mob.level().players()
                    .forEach(p -> p.addEffect(
                            new MobEffectInstance(MobEffects.WEAKNESS, 100, 0)));
        };

        BiConsumer<LivingEntity, LivingEntity> BONE_ARMOR_EFFECT = (mob, target) -> {
            // TODO: projectile damage reduction
        };

        BiConsumer<LivingEntity, LivingEntity> PACK_LEADER_EFFECT = (mob, target) -> {
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 0));
        };

        BiConsumer<LivingEntity, LivingEntity> SHADOWSTEP_EFFECT = (mob, target) -> {
            // TODO: dodge teleport
        };

        BiConsumer<LivingEntity, LivingEntity> FRENZY_EFFECT = (mob, target) -> {
            // TODO: speed scaling with missing HP
        };

        BiConsumer<LivingEntity, LivingEntity> REGENERATOR_EFFECT = (mob, target) -> {
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0));
        };

        BiConsumer<LivingEntity, LivingEntity> BERSERKER_EFFECT = (mob, target) -> {
            // TODO: attack speed / damage scaling
        };

        BiConsumer<LivingEntity, LivingEntity> VOLLEY_EFFECT = (mob, target) -> {
            // TODO: multi-arrow shot
        };

        BiConsumer<LivingEntity, LivingEntity> THORNS_EFFECT = (mob, target) -> {
            // TODO: reflect damage back to attackers
        };

        BiConsumer<LivingEntity, LivingEntity> GRAVE_DUST_EFFECT = (mob, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
        };

        // ==================================================================
        // REGISTRATIONS — grouped by mob type and trigger
        // ==================================================================

        // ========================
        //  ZOMBIE
        // ========================
        // TriggerType.HURT
        reg.accept(EntityTypes.ZOMBIE, new Ability("plague_bearer",  "Plague Bearer",  IS_ZOMBIE, TriggerType.HURT, PLAGUE_BEARER_EFFECT));
        reg.accept(EntityTypes.ZOMBIE, new Ability("lifestrike",     "Lifestrike",     IS_MONSTER, TriggerType.HURT, LIFESTRIKE_EFFECT));
        reg.accept(EntityTypes.ZOMBIE, new Ability("cursed_wound",   "Cursed Wound",   IS_MONSTER, TriggerType.HURT, CURSED_WOUND_EFFECT));
        reg.accept(EntityTypes.ZOMBIE, new Ability("rust",           "Rust",           IS_MONSTER, TriggerType.HURT, RUST_EFFECT));
        reg.accept(EntityTypes.ZOMBIE, new Ability("disarm",         "Disarm",         IS_MONSTER, TriggerType.HURT, DISARM_EFFECT));
        // TriggerType.TICK
        reg.accept(EntityTypes.ZOMBIE, new Ability("bulwark",        "Bulwark",        IS_ZOMBIE, TriggerType.TICK, BULWARK_EFFECT));
        reg.accept(EntityTypes.ZOMBIE, new Ability("frenzy",         "Frenzy",         IS_MONSTER, TriggerType.TICK, FRENZY_EFFECT));
        reg.accept(EntityTypes.ZOMBIE, new Ability("berserker",      "Berserker",      IS_MONSTER, TriggerType.TICK, BERSERKER_EFFECT));
        reg.accept(EntityTypes.ZOMBIE, new Ability("regenerator",    "Regenerator",    IS_MONSTER, TriggerType.TICK, REGENERATOR_EFFECT));
        reg.accept(EntityTypes.ZOMBIE, new Ability("pack_leader",    "Pack Leader",    IS_MONSTER, TriggerType.TICK, PACK_LEADER_EFFECT));
        // TriggerType.DEATH
        reg.accept(EntityTypes.ZOMBIE, new Ability("split",          "Split",          IS_MONSTER, TriggerType.DEATH, SPLIT_EFFECT));
        reg.accept(EntityTypes.ZOMBIE, new Ability("horde_caller",   "Horde Caller",   IS_ZOMBIE, TriggerType.DEATH, HORDE_CALLER_EFFECT));
        // TriggerType.SPAWN
        reg.accept(EntityTypes.ZOMBIE, new Ability("corrupted_presence", "Corrupted Presence", IS_MONSTER, TriggerType.SPAWN, CORRUPTED_PRESENCE_EFFECT));
        reg.accept(EntityTypes.ZOMBIE, new Ability("regenerator_spawn",  "Regenerator",        IS_MONSTER, TriggerType.SPAWN, REGENERATOR_EFFECT));
        reg.accept(EntityTypes.ZOMBIE, new Ability("thorns_zombie",      "Thorns",             IS_MONSTER, TriggerType.SPAWN, THORNS_EFFECT));

        // ========================
        //  SKELETON
        // ========================
        // TriggerType.PROJECTILE_HIT
        reg.accept(EntityTypes.SKELETON, new Ability("grave_dust",    "Grave Dust",    IS_SKELETON, TriggerType.PROJECTILE_HIT, GRAVE_DUST_EFFECT));
        reg.accept(EntityTypes.SKELETON, new Ability("shield_breaker","Shield Breaker",IS_SKELETON, TriggerType.PROJECTILE_HIT, SHIELD_BREAKER_EFFECT));
        reg.accept(EntityTypes.SKELETON, new Ability("volley",        "Volley",        IS_SKELETON, TriggerType.PROJECTILE_HIT, VOLLEY_EFFECT));
        // TriggerType.TICK
        reg.accept(EntityTypes.SKELETON, new Ability("bone_armor",    "Bone Armor",    IS_SKELETON, TriggerType.TICK, BONE_ARMOR_EFFECT));
        reg.accept(EntityTypes.SKELETON, new Ability("frenzy_skele",  "Frenzy",        IS_MONSTER,  TriggerType.TICK, FRENZY_EFFECT));
        reg.accept(EntityTypes.SKELETON, new Ability("berserker_skele","Berserker",    IS_MONSTER,  TriggerType.TICK, BERSERKER_EFFECT));
        reg.accept(EntityTypes.SKELETON, new Ability("regenerator_skele","Regenerator",IS_MONSTER,  TriggerType.TICK, REGENERATOR_EFFECT));
        reg.accept(EntityTypes.SKELETON, new Ability("pack_leader_skele","Pack Leader", IS_MONSTER, TriggerType.TICK, PACK_LEADER_EFFECT));
        // TriggerType.DEATH
        reg.accept(EntityTypes.SKELETON, new Ability("split_skele",   "Split",         IS_MONSTER, TriggerType.DEATH, SPLIT_EFFECT));
        // TriggerType.SPAWN
        reg.accept(EntityTypes.SKELETON, new Ability("bone_armor_spawn",       "Bone Armor",        IS_SKELETON, TriggerType.SPAWN, BONE_ARMOR_EFFECT));
        reg.accept(EntityTypes.SKELETON, new Ability("corrupted_presence_skele","Corrupted Presence", IS_MONSTER,  TriggerType.SPAWN, CORRUPTED_PRESENCE_EFFECT));
        reg.accept(EntityTypes.SKELETON, new Ability("regenerator_skele_spawn","Regenerator",        IS_MONSTER,  TriggerType.SPAWN, REGENERATOR_EFFECT));
        reg.accept(EntityTypes.SKELETON, new Ability("thorns_skele",           "Thorns",            IS_MONSTER,  TriggerType.SPAWN, THORNS_EFFECT));

        // ========================
        //  SPIDER
        // ========================
        // TriggerType.HURT
        reg.accept(EntityTypes.SPIDER, new Ability("venomous_bite", "Venomous Bite", IS_SPIDER,  TriggerType.HURT, VENOMOUS_BITE_EFFECT));
        reg.accept(EntityTypes.SPIDER, new Ability("lifestrike_spider","Lifestrike", IS_MONSTER, TriggerType.HURT, LIFESTRIKE_EFFECT));
        reg.accept(EntityTypes.SPIDER, new Ability("cursed_wound_spider","Cursed Wound", IS_MONSTER, TriggerType.HURT, CURSED_WOUND_EFFECT));
        reg.accept(EntityTypes.SPIDER, new Ability("rust_spider",   "Rust",           IS_MONSTER, TriggerType.HURT, RUST_EFFECT));
        reg.accept(EntityTypes.SPIDER, new Ability("disarm_spider",  "Disarm",        IS_MONSTER, TriggerType.HURT, DISARM_EFFECT));
        // TriggerType.TICK
        reg.accept(EntityTypes.SPIDER, new Ability("frenzy_spider",  "Frenzy",        IS_MONSTER, TriggerType.TICK, FRENZY_EFFECT));
        reg.accept(EntityTypes.SPIDER, new Ability("berserker_spider","Berserker",    IS_MONSTER, TriggerType.TICK, BERSERKER_EFFECT));
        reg.accept(EntityTypes.SPIDER, new Ability("regenerator_spider","Regenerator",IS_MONSTER, TriggerType.TICK, REGENERATOR_EFFECT));
        reg.accept(EntityTypes.SPIDER, new Ability("pack_leader_spider","Pack Leader", IS_MONSTER,TriggerType.TICK, PACK_LEADER_EFFECT));
        // TriggerType.DEATH
        reg.accept(EntityTypes.SPIDER, new Ability("split_spider",  "Split",         IS_MONSTER, TriggerType.DEATH, SPLIT_EFFECT));
        // TriggerType.SPAWN
        reg.accept(EntityTypes.SPIDER, new Ability("corrupted_presence_spider","Corrupted Presence", IS_MONSTER, TriggerType.SPAWN, CORRUPTED_PRESENCE_EFFECT));
        reg.accept(EntityTypes.SPIDER, new Ability("regenerator_spider_spawn","Regenerator",        IS_MONSTER, TriggerType.SPAWN, REGENERATOR_EFFECT));
        reg.accept(EntityTypes.SPIDER, new Ability("thorns_spider",           "Thorns",             IS_MONSTER, TriggerType.SPAWN, THORNS_EFFECT));

        // ========================
        //  CREEPER
        // ========================
        // TriggerType.HURT
        reg.accept(EntityTypes.CREEPER, new Ability("lifestrike_creeper","Lifestrike",IS_MONSTER, TriggerType.HURT, LIFESTRIKE_EFFECT));
        reg.accept(EntityTypes.CREEPER, new Ability("rust_creeper",  "Rust",          IS_MONSTER, TriggerType.HURT, RUST_EFFECT));
        // TriggerType.DEATH
        reg.accept(EntityTypes.CREEPER, new Ability("corrosive_splash","Corrosive Splash", IS_CREEPER, TriggerType.DEATH, CORROSIVE_SPLASH_EFFECT));
        reg.accept(EntityTypes.CREEPER, new Ability("split_creeper", "Split",          IS_MONSTER, TriggerType.DEATH, SPLIT_EFFECT));
        // TriggerType.TICK
        reg.accept(EntityTypes.CREEPER, new Ability("frenzy_creeper","Frenzy",         IS_MONSTER, TriggerType.TICK, FRENZY_EFFECT));
        reg.accept(EntityTypes.CREEPER, new Ability("berserker_creeper","Berserker",   IS_MONSTER, TriggerType.TICK, BERSERKER_EFFECT));
        reg.accept(EntityTypes.CREEPER, new Ability("shadowstep_creeper","Shadowstep", IS_MONSTER, TriggerType.TICK, SHADOWSTEP_EFFECT));
        // TriggerType.SPAWN
        reg.accept(EntityTypes.CREEPER, new Ability("corrupted_presence_creeper","Corrupted Presence", IS_MONSTER, TriggerType.SPAWN, CORRUPTED_PRESENCE_EFFECT));
        reg.accept(EntityTypes.CREEPER, new Ability("regenerator_creeper_spawn","Regenerator",        IS_MONSTER, TriggerType.SPAWN, REGENERATOR_EFFECT));
        reg.accept(EntityTypes.CREEPER, new Ability("thorns_creeper",           "Thorns",             IS_MONSTER, TriggerType.SPAWN, THORNS_EFFECT));

        // ========================
        //  PILLAGER
        // ========================
        // TriggerType.PROJECTILE_HIT
        reg.accept(EntityTypes.PILLAGER, new Ability("shield_breaker_pillager","Shield Breaker", IS_PILLAGER, TriggerType.PROJECTILE_HIT, SHIELD_BREAKER_EFFECT));
        // TriggerType.HURT
        reg.accept(EntityTypes.PILLAGER, new Ability("disarm_pillager","Disarm",      IS_MONSTER, TriggerType.HURT, DISARM_EFFECT));
        reg.accept(EntityTypes.PILLAGER, new Ability("lifestrike_pillager","Lifestrike",IS_MONSTER,TriggerType.HURT, LIFESTRIKE_EFFECT));
        reg.accept(EntityTypes.PILLAGER, new Ability("rust_pillager","Rust",           IS_MONSTER, TriggerType.HURT, RUST_EFFECT));
        // TriggerType.TICK
        reg.accept(EntityTypes.PILLAGER, new Ability("bannerman",    "Bannerman",     IS_PILLAGER, TriggerType.TICK, BANNERMAN_EFFECT));
        reg.accept(EntityTypes.PILLAGER, new Ability("frenzy_pillager","Frenzy",       IS_MONSTER,  TriggerType.TICK, FRENZY_EFFECT));
        reg.accept(EntityTypes.PILLAGER, new Ability("berserker_pillager","Berserker", IS_MONSTER,  TriggerType.TICK, BERSERKER_EFFECT));
        reg.accept(EntityTypes.PILLAGER, new Ability("regenerator_pillager","Regenerator",IS_MONSTER,TriggerType.TICK, REGENERATOR_EFFECT));
        reg.accept(EntityTypes.PILLAGER, new Ability("pack_leader_pillager","Pack Leader",IS_MONSTER,TriggerType.TICK, PACK_LEADER_EFFECT));
        // TriggerType.DEATH
        reg.accept(EntityTypes.PILLAGER, new Ability("split_pillager","Split",         IS_MONSTER, TriggerType.DEATH, SPLIT_EFFECT));
        // TriggerType.SPAWN
        reg.accept(EntityTypes.PILLAGER, new Ability("bannerman_spawn",             "Bannerman",           IS_PILLAGER, TriggerType.SPAWN, BANNERMAN_EFFECT));
        reg.accept(EntityTypes.PILLAGER, new Ability("corrupted_presence_pillager", "Corrupted Presence",  IS_MONSTER,  TriggerType.SPAWN, CORRUPTED_PRESENCE_EFFECT));
        reg.accept(EntityTypes.PILLAGER, new Ability("regenerator_pillager_spawn",  "Regenerator",         IS_MONSTER,  TriggerType.SPAWN, REGENERATOR_EFFECT));
        reg.accept(EntityTypes.PILLAGER, new Ability("thorns_pillager",             "Thorns",              IS_MONSTER,  TriggerType.SPAWN, THORNS_EFFECT));

        // ========================
        //  ENDERMAN
        // ========================
        // TriggerType.HURT
        reg.accept(EntityTypes.ENDERMAN, new Ability("mind_shatter",  "Mind Shatter",  IS_ENDERMAN, TriggerType.HURT, MIND_SHATTER_EFFECT));
        reg.accept(EntityTypes.ENDERMAN, new Ability("lifestrike_ender","Lifestrike",  IS_MONSTER,  TriggerType.HURT, LIFESTRIKE_EFFECT));
        reg.accept(EntityTypes.ENDERMAN, new Ability("cursed_wound_ender","Cursed Wound",IS_MONSTER, TriggerType.HURT, CURSED_WOUND_EFFECT));
        reg.accept(EntityTypes.ENDERMAN, new Ability("rust_ender",    "Rust",           IS_MONSTER,  TriggerType.HURT, RUST_EFFECT));
        reg.accept(EntityTypes.ENDERMAN, new Ability("disarm_ender",  "Disarm",         IS_MONSTER,  TriggerType.HURT, DISARM_EFFECT));
        // TriggerType.TICK
        reg.accept(EntityTypes.ENDERMAN, new Ability("shadowstep",   "Shadowstep",     IS_ENDERMAN, TriggerType.TICK, SHADOWSTEP_EFFECT));
        reg.accept(EntityTypes.ENDERMAN, new Ability("frenzy_ender",  "Frenzy",         IS_MONSTER,  TriggerType.TICK, FRENZY_EFFECT));
        reg.accept(EntityTypes.ENDERMAN, new Ability("berserker_ender","Berserker",     IS_MONSTER,  TriggerType.TICK, BERSERKER_EFFECT));
        reg.accept(EntityTypes.ENDERMAN, new Ability("regenerator_ender","Regenerator", IS_MONSTER,  TriggerType.TICK, REGENERATOR_EFFECT));
        reg.accept(EntityTypes.ENDERMAN, new Ability("pack_leader_ender","Pack Leader", IS_MONSTER,  TriggerType.TICK, PACK_LEADER_EFFECT));
        // TriggerType.DEATH
        reg.accept(EntityTypes.ENDERMAN, new Ability("split_ender",  "Split",          IS_MONSTER,  TriggerType.DEATH, SPLIT_EFFECT));
        // TriggerType.SPAWN
        reg.accept(EntityTypes.ENDERMAN, new Ability("corrupted_presence_ender","Corrupted Presence", IS_MONSTER, TriggerType.SPAWN, CORRUPTED_PRESENCE_EFFECT));
        reg.accept(EntityTypes.ENDERMAN, new Ability("regenerator_ender_spawn","Regenerator",        IS_MONSTER, TriggerType.SPAWN, REGENERATOR_EFFECT));
        reg.accept(EntityTypes.ENDERMAN, new Ability("thorns_ender",           "Thorns",             IS_MONSTER, TriggerType.SPAWN, THORNS_EFFECT));
    }
}
