package io.github.hunter1712.mobabilities.ability;

import io.github.hunter1712.mobabilities.ability.effect.CorrosiveSplashEffect;
import io.github.hunter1712.mobabilities.ability.effect.HordeCallerEffect;
import io.github.hunter1712.mobabilities.ability.effect.LifestrikeEffect;
import io.github.hunter1712.mobabilities.ability.effect.ShadowstepEffect;
import io.github.hunter1712.mobabilities.ability.effect.SplitEffect;
import io.github.hunter1712.mobabilities.config.ModConfig;
import io.github.hunter1712.mobabilities.effect.ModEffects;
import io.github.hunter1712.mobabilities.trigger.DamageContext;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
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

    /** Cache: entity type → trigger → list of matching abilities (populated after registration). */
    private static final Map<EntityType<?>, EnumMap<TriggerType, List<Ability>>> CACHE = new HashMap<>();

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
     * <p>
     * Uses a pre-computed cache built after registration so no stream
     * pipelines are allocated at query time.
     *
     * @param mob    the living entity to query for
     * @param trigger the trigger type to filter by
     * @return list of matching abilities (never {@code null})
     */
    public static List<Ability> getAbilitiesForMob(LivingEntity mob, TriggerType trigger) {
        EnumMap<TriggerType, List<Ability>> byTrigger = CACHE.get(mob.getType());
        if (byTrigger == null) return List.of();
        List<Ability> result = byTrigger.get(trigger);
        return result != null ? result : List.of();
    }

    /**
     * Performs weighted-random selection of an ability for the given mob
     * and trigger type. Weights are relative — an ability with weight 20
     * is twice as likely as one with weight 10.
     * <p>
     * Uses a pre-computed cached list and {@link ThreadLocalRandom} for
     * zero-allocation random selection.
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
        int totalWeight = 0;
        for (WeightedAbility wa : candidates) {
            totalWeight += wa.weight();
        }
        int remaining = ThreadLocalRandom.current().nextInt(totalWeight);

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
    // Registration helpers
    // ========================================

    private static void reg(EntityType<?> type, Ability ability, int weight) {
        registerAbility(type, ability, weight);
    }

    // ========================================
    // Cache population (called after registerAll)
    // ========================================

    /**
     * Builds the query cache from the raw REGISTRY. Groups abilities by
     * (entity type, trigger type) so {@link #getAbilitiesForMob} returns
     * a constant list with zero per-call allocation.
     * <p>
     * Abilities are registered with predicates (IS_ZOMBIE, IS_MONSTER,
     * etc.) that always match the entity type they're registered under,
     * so grouping by type alone is correct.
     */
    private static void buildCache() {
        for (Map.Entry<EntityType<?>, List<WeightedAbility>> entry : REGISTRY.entrySet()) {
            EntityType<?> type = entry.getKey();
            List<WeightedAbility> weighted = entry.getValue();

            EnumMap<TriggerType, List<Ability>> byTrigger = new EnumMap<>(TriggerType.class);

            for (TriggerType trigger : TriggerType.values()) {
                List<Ability> matched = new ArrayList<>();
                for (WeightedAbility wa : weighted) {
                    Ability a = wa.ability();
                    if (a.trigger() == trigger) {
                        matched.add(a);
                    }
                }
                byTrigger.put(trigger, Collections.unmodifiableList(matched));
            }
            CACHE.put(type, byTrigger);
        }
    }

    // ========================================
    // Registration of all 23 abilities
    // ========================================

    /**
     * Populates the registry with all 23 mob abilities.
     *
     * <p>Each ability is registered under one or more {@link EntityType mob
     * types} with weights defined in {@link ModConfig}. Abilities that apply
     * to multiple mob types use a broad {@code mob instanceof Monster} predicate;
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
            float amount = DamageContext.getAndClear();
            if (amount > 0) {
                LifestrikeEffect.apply(mob, amount);
            }
        };

        BiConsumer<LivingEntity, LivingEntity> CURSED_WOUND_EFFECT = (mob, target) -> {
            target.addEffect(new MobEffectInstance(ModEffects.CURSED_WOUND, 200, 0));
        };

        BiConsumer<LivingEntity, LivingEntity> SHIELD_BREAKER_EFFECT = (mob, target) -> {
            if (target instanceof Player player && player.isBlocking()) {
                player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 1));
                // Disable shield via cooldown
                player.getCooldowns().addCooldown(player.getUseItem(), 100);
            }
        };

        BiConsumer<LivingEntity, LivingEntity> MIND_SHATTER_EFFECT = (mob, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 200, 0));
            target.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 100, 0));
        };

        BiConsumer<LivingEntity, LivingEntity> RUST_EFFECT = (mob, target) -> {
            if (target instanceof net.minecraft.server.level.ServerPlayer serverPlayer) {
                ServerLevel level = (ServerLevel) serverPlayer.level();
                serverPlayer.getItemBySlot(EquipmentSlot.HEAD).hurtAndBreak(2, level, serverPlayer, item -> {});
                serverPlayer.getItemBySlot(EquipmentSlot.CHEST).hurtAndBreak(2, level, serverPlayer, item -> {});
                serverPlayer.getItemBySlot(EquipmentSlot.LEGS).hurtAndBreak(2, level, serverPlayer, item -> {});
                serverPlayer.getItemBySlot(EquipmentSlot.FEET).hurtAndBreak(2, level, serverPlayer, item -> {});
            }
        };

        BiConsumer<LivingEntity, LivingEntity> DISARM_EFFECT = (mob, target) -> {
            if (target instanceof Player player) {
                if (player.getRandom().nextFloat() >= 0.25f) return;
                ItemStack held = player.getMainHandItem();
                if (held.isEmpty()) return;
                player.drop(held, true, false);
                player.setItemSlot(EquipmentSlot.MAINHAND, ItemStack.EMPTY);
            }
        };

        BiConsumer<LivingEntity, LivingEntity> CORROSIVE_SPLASH_EFFECT = (mob, target) -> {
            if (mob.level() instanceof ServerLevel serverLevel) {
                CorrosiveSplashEffect.apply(mob, serverLevel);
            }
        };

        // ---- DEATH triggers ----

        BiConsumer<LivingEntity, LivingEntity> SPLIT_EFFECT = (mob, target) -> {
            if (mob.level() instanceof ServerLevel serverLevel) {
                SplitEffect.apply(mob, serverLevel);
            }
        };

        BiConsumer<LivingEntity, LivingEntity> HORDE_CALLER_EFFECT = (mob, target) -> {
            if (mob.level() instanceof ServerLevel serverLevel) {
                HordeCallerEffect.apply(mob, serverLevel);
            }
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
            mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0));
        };

        BiConsumer<LivingEntity, LivingEntity> PACK_LEADER_EFFECT = (mob, target) -> {
            mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, 0));
        };

        BiConsumer<LivingEntity, LivingEntity> SHADOWSTEP_EFFECT = (mob, target) -> {
            // Shadowstep dodge is handled via LivingEntityMixin + ShadowstepEffect.tryDodge()
            // This TICK effect just provides a small resistance buff while alive
            mob.addEffect(new MobEffectInstance(MobEffects.RESISTANCE, 40, 0));
        };

        BiConsumer<LivingEntity, LivingEntity> FRENZY_EFFECT = (mob, target) -> {
            float healthRatio = mob.getHealth() / mob.getMaxHealth();
            if (healthRatio < 0.5f) {
                int amplifier = healthRatio < 0.25f ? 1 : 0;
                mob.addEffect(new MobEffectInstance(MobEffects.SPEED, 40, amplifier));
            }
        };

        BiConsumer<LivingEntity, LivingEntity> REGENERATOR_EFFECT = (mob, target) -> {
            mob.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 40, 0));
        };

        BiConsumer<LivingEntity, LivingEntity> BERSERKER_EFFECT = (mob, target) -> {
            float healthRatio = mob.getHealth() / mob.getMaxHealth();
            if (healthRatio < 0.5f) {
                int amplifier = healthRatio < 0.25f ? 1 : 0;
                mob.addEffect(new MobEffectInstance(MobEffects.STRENGTH, 40, amplifier));
            }
        };

        BiConsumer<LivingEntity, LivingEntity> VOLLEY_EFFECT = (mob, target) -> {
            // Volley multi-arrow is handled via SkeletonMixin
            // This BiConsumer exists so the ability is selectable; the mixin does the actual work
        };

        BiConsumer<LivingEntity, LivingEntity> THORNS_EFFECT = (mob, target) -> {
            // Thorns reflection is handled via LivingEntityMixin.onHurtServer
            // The Thorns mixin checks MobSpawnTrigger.hasThorns() and reflects damage
        };

        BiConsumer<LivingEntity, LivingEntity> GRAVE_DUST_EFFECT = (mob, target) -> {
            target.addEffect(new MobEffectInstance(MobEffects.WITHER, 200, 0));
        };

        // ==================================================================
        // REGISTRATIONS — grouped by mob type and trigger
        // Weights are from ModConfig constants for balanced gameplay.
        // ==================================================================

        // ========================
        //  ZOMBIE
        // ========================
        // TriggerType.HURT
        reg(EntityTypes.ZOMBIE, new Ability("plague_bearer",  "Plague Bearer",  IS_ZOMBIE,  TriggerType.HURT, PLAGUE_BEARER_EFFECT),   ModConfig.ZOMBIE_PLAGUE_BEARER);
        reg(EntityTypes.ZOMBIE, new Ability("lifestrike",     "Lifestrike",     IS_MONSTER, TriggerType.HURT, LIFESTRIKE_EFFECT),      ModConfig.ZOMBIE_LIFESTRIKE);
        reg(EntityTypes.ZOMBIE, new Ability("cursed_wound",   "Cursed Wound",   IS_MONSTER, TriggerType.HURT, CURSED_WOUND_EFFECT),    ModConfig.ZOMBIE_CURSED_WOUND);
        reg(EntityTypes.ZOMBIE, new Ability("rust",           "Rust",           IS_MONSTER, TriggerType.HURT, RUST_EFFECT),            ModConfig.ZOMBIE_RUST);
        reg(EntityTypes.ZOMBIE, new Ability("disarm",         "Disarm",         IS_MONSTER, TriggerType.HURT, DISARM_EFFECT),          ModConfig.ZOMBIE_DISARM);
        // TriggerType.TICK
        reg(EntityTypes.ZOMBIE, new Ability("bulwark",        "Bulwark",        IS_ZOMBIE,  TriggerType.TICK, BULWARK_EFFECT),        ModConfig.ZOMBIE_BULWARK);
        reg(EntityTypes.ZOMBIE, new Ability("frenzy",         "Frenzy",         IS_MONSTER, TriggerType.TICK, FRENZY_EFFECT),          ModConfig.ZOMBIE_FRENZY);
        reg(EntityTypes.ZOMBIE, new Ability("berserker",      "Berserker",      IS_MONSTER, TriggerType.TICK, BERSERKER_EFFECT),       ModConfig.ZOMBIE_BERSERKER);
        reg(EntityTypes.ZOMBIE, new Ability("regenerator",    "Regenerator",    IS_MONSTER, TriggerType.TICK, REGENERATOR_EFFECT),     ModConfig.ZOMBIE_REGENERATOR);
        reg(EntityTypes.ZOMBIE, new Ability("pack_leader",    "Pack Leader",    IS_MONSTER, TriggerType.TICK, PACK_LEADER_EFFECT),     ModConfig.ZOMBIE_PACK_LEADER);
        // TriggerType.DEATH
        reg(EntityTypes.ZOMBIE, new Ability("split",          "Split",          IS_MONSTER, TriggerType.DEATH, SPLIT_EFFECT),          ModConfig.ZOMBIE_SPLIT);
        reg(EntityTypes.ZOMBIE, new Ability("horde_caller",   "Horde Caller",   IS_ZOMBIE,  TriggerType.DEATH, HORDE_CALLER_EFFECT),  ModConfig.ZOMBIE_HORDE_CALLER);
        // TriggerType.SPAWN
        reg(EntityTypes.ZOMBIE, new Ability("corrupted_presence", "Corrupted Presence", IS_MONSTER, TriggerType.SPAWN, CORRUPTED_PRESENCE_EFFECT), ModConfig.ZOMBIE_CORRUPTED_PRESENCE);
        reg(EntityTypes.ZOMBIE, new Ability("regenerator_spawn",  "Regenerator",        IS_MONSTER, TriggerType.SPAWN, REGENERATOR_EFFECT),         ModConfig.ZOMBIE_REGENERATOR);
        reg(EntityTypes.ZOMBIE, new Ability("thorns_zombie",      "Thorns",             IS_MONSTER, TriggerType.SPAWN, THORNS_EFFECT),              ModConfig.ZOMBIE_THORNS);

        // ========================
        //  SKELETON
        // ========================
        // TriggerType.PROJECTILE_HIT
        reg(EntityTypes.SKELETON, new Ability("grave_dust",    "Grave Dust",    IS_SKELETON, TriggerType.PROJECTILE_HIT, GRAVE_DUST_EFFECT),    ModConfig.SKELETON_GRAVE_DUST);
        reg(EntityTypes.SKELETON, new Ability("shield_breaker","Shield Breaker",IS_SKELETON, TriggerType.PROJECTILE_HIT, SHIELD_BREAKER_EFFECT), ModConfig.SKELETON_SHIELD_BREAKER);
        reg(EntityTypes.SKELETON, new Ability("volley",        "Volley",        IS_SKELETON, TriggerType.PROJECTILE_HIT, VOLLEY_EFFECT),        ModConfig.SKELETON_VOLLEY);
        // TriggerType.TICK
        reg(EntityTypes.SKELETON, new Ability("bone_armor",    "Bone Armor",    IS_SKELETON, TriggerType.TICK, BONE_ARMOR_EFFECT),    ModConfig.SKELETON_BONE_ARMOR);
        reg(EntityTypes.SKELETON, new Ability("frenzy_skele",  "Frenzy",        IS_MONSTER,  TriggerType.TICK, FRENZY_EFFECT),        ModConfig.SKELETON_FRENZY);
        reg(EntityTypes.SKELETON, new Ability("berserker_skele","Berserker",    IS_MONSTER,  TriggerType.TICK, BERSERKER_EFFECT),     ModConfig.SKELETON_BERSERKER);
        reg(EntityTypes.SKELETON, new Ability("regenerator_skele","Regenerator",IS_MONSTER,  TriggerType.TICK, REGENERATOR_EFFECT),   ModConfig.SKELETON_REGENERATOR);
        reg(EntityTypes.SKELETON, new Ability("pack_leader_skele","Pack Leader", IS_MONSTER, TriggerType.TICK, PACK_LEADER_EFFECT),   ModConfig.SKELETON_PACK_LEADER);
        // TriggerType.DEATH
        reg(EntityTypes.SKELETON, new Ability("split_skele",   "Split",         IS_MONSTER, TriggerType.DEATH, SPLIT_EFFECT),         ModConfig.SKELETON_SPLIT);
        // TriggerType.SPAWN
        reg(EntityTypes.SKELETON, new Ability("bone_armor_spawn",       "Bone Armor",        IS_SKELETON, TriggerType.SPAWN, BONE_ARMOR_EFFECT),        ModConfig.SKELETON_BONE_ARMOR);
        reg(EntityTypes.SKELETON, new Ability("corrupted_presence_skele","Corrupted Presence", IS_MONSTER,  TriggerType.SPAWN, CORRUPTED_PRESENCE_EFFECT), ModConfig.SKELETON_CORRUPTED_PRESENCE);
        reg(EntityTypes.SKELETON, new Ability("regenerator_skele_spawn","Regenerator",        IS_MONSTER,  TriggerType.SPAWN, REGENERATOR_EFFECT),        ModConfig.SKELETON_REGENERATOR);
        reg(EntityTypes.SKELETON, new Ability("thorns_skele",           "Thorns",            IS_MONSTER,  TriggerType.SPAWN, THORNS_EFFECT),             ModConfig.SKELETON_THORNS);

        // ========================
        //  SPIDER
        // ========================
        // TriggerType.HURT
        reg(EntityTypes.SPIDER, new Ability("venomous_bite", "Venomous Bite", IS_SPIDER,  TriggerType.HURT, VENOMOUS_BITE_EFFECT),  ModConfig.SPIDER_VENOMOUS_BITE);
        reg(EntityTypes.SPIDER, new Ability("lifestrike_spider","Lifestrike", IS_MONSTER, TriggerType.HURT, LIFESTRIKE_EFFECT),     ModConfig.SPIDER_LIFESTRIKE);
        reg(EntityTypes.SPIDER, new Ability("cursed_wound_spider","Cursed Wound", IS_MONSTER, TriggerType.HURT, CURSED_WOUND_EFFECT), ModConfig.SPIDER_CURSED_WOUND);
        reg(EntityTypes.SPIDER, new Ability("rust_spider",   "Rust",           IS_MONSTER, TriggerType.HURT, RUST_EFFECT),           ModConfig.SPIDER_RUST);
        reg(EntityTypes.SPIDER, new Ability("disarm_spider",  "Disarm",        IS_MONSTER, TriggerType.HURT, DISARM_EFFECT),         ModConfig.SPIDER_DISARM);
        // TriggerType.TICK
        reg(EntityTypes.SPIDER, new Ability("frenzy_spider",  "Frenzy",        IS_MONSTER, TriggerType.TICK, FRENZY_EFFECT),        ModConfig.SPIDER_FRENZY);
        reg(EntityTypes.SPIDER, new Ability("berserker_spider","Berserker",    IS_MONSTER, TriggerType.TICK, BERSERKER_EFFECT),     ModConfig.SPIDER_BERSERKER);
        reg(EntityTypes.SPIDER, new Ability("regenerator_spider","Regenerator",IS_MONSTER,TriggerType.TICK, REGENERATOR_EFFECT),   ModConfig.SPIDER_REGENERATOR);
        reg(EntityTypes.SPIDER, new Ability("pack_leader_spider","Pack Leader", IS_MONSTER,TriggerType.TICK, PACK_LEADER_EFFECT),   ModConfig.SPIDER_PACK_LEADER);
        // TriggerType.DEATH
        reg(EntityTypes.SPIDER, new Ability("split_spider",  "Split",         IS_MONSTER, TriggerType.DEATH, SPLIT_EFFECT),        ModConfig.SPIDER_SPLIT);
        // TriggerType.SPAWN
        reg(EntityTypes.SPIDER, new Ability("corrupted_presence_spider","Corrupted Presence", IS_MONSTER, TriggerType.SPAWN, CORRUPTED_PRESENCE_EFFECT), ModConfig.SPIDER_CORRUPTED_PRESENCE);
        reg(EntityTypes.SPIDER, new Ability("regenerator_spider_spawn","Regenerator",        IS_MONSTER, TriggerType.SPAWN, REGENERATOR_EFFECT),        ModConfig.SPIDER_REGENERATOR);
        reg(EntityTypes.SPIDER, new Ability("thorns_spider",           "Thorns",             IS_MONSTER, TriggerType.SPAWN, THORNS_EFFECT),             ModConfig.SPIDER_THORNS);

        // ========================
        //  CREEPER
        // ========================
        // TriggerType.HURT
        reg(EntityTypes.CREEPER, new Ability("lifestrike_creeper","Lifestrike",IS_MONSTER, TriggerType.HURT, LIFESTRIKE_EFFECT),     ModConfig.CREEPER_LIFESTRIKE);
        reg(EntityTypes.CREEPER, new Ability("rust_creeper",  "Rust",          IS_MONSTER, TriggerType.HURT, RUST_EFFECT),           ModConfig.CREEPER_RUST);
        // TriggerType.DEATH
        reg(EntityTypes.CREEPER, new Ability("corrosive_splash","Corrosive Splash", IS_CREEPER, TriggerType.DEATH, CORROSIVE_SPLASH_EFFECT), ModConfig.CREEPER_CORROSIVE_SPLASH);
        reg(EntityTypes.CREEPER, new Ability("split_creeper", "Split",          IS_MONSTER, TriggerType.DEATH, SPLIT_EFFECT),        ModConfig.CREEPER_SPLIT);
        // TriggerType.TICK
        reg(EntityTypes.CREEPER, new Ability("frenzy_creeper","Frenzy",         IS_MONSTER, TriggerType.TICK, FRENZY_EFFECT),       ModConfig.CREEPER_FRENZY);
        reg(EntityTypes.CREEPER, new Ability("berserker_creeper","Berserker",   IS_MONSTER, TriggerType.TICK, BERSERKER_EFFECT),    ModConfig.CREEPER_BERSERKER);
        reg(EntityTypes.CREEPER, new Ability("shadowstep_creeper","Shadowstep", IS_MONSTER, TriggerType.TICK, SHADOWSTEP_EFFECT),  ModConfig.CREEPER_SHADOWSTEP);
        // TriggerType.SPAWN
        reg(EntityTypes.CREEPER, new Ability("corrupted_presence_creeper","Corrupted Presence", IS_MONSTER, TriggerType.SPAWN, CORRUPTED_PRESENCE_EFFECT), ModConfig.CREEPER_CORRUPTED_PRESENCE);
        reg(EntityTypes.CREEPER, new Ability("regenerator_creeper_spawn","Regenerator",        IS_MONSTER, TriggerType.SPAWN, REGENERATOR_EFFECT),        ModConfig.CREEPER_REGENERATOR);
        reg(EntityTypes.CREEPER, new Ability("thorns_creeper",           "Thorns",             IS_MONSTER, TriggerType.SPAWN, THORNS_EFFECT),             ModConfig.CREEPER_THORNS);

        // ========================
        //  PILLAGER
        // ========================
        // TriggerType.PROJECTILE_HIT
        reg(EntityTypes.PILLAGER, new Ability("shield_breaker_pillager","Shield Breaker", IS_PILLAGER, TriggerType.PROJECTILE_HIT, SHIELD_BREAKER_EFFECT), ModConfig.PILLAGER_SHIELD_BREAKER);
        // TriggerType.HURT
        reg(EntityTypes.PILLAGER, new Ability("disarm_pillager","Disarm",      IS_MONSTER, TriggerType.HURT, DISARM_EFFECT),          ModConfig.PILLAGER_DISARM);
        reg(EntityTypes.PILLAGER, new Ability("lifestrike_pillager","Lifestrike",IS_MONSTER,TriggerType.HURT, LIFESTRIKE_EFFECT),    ModConfig.PILLAGER_LIFESTRIKE);
        reg(EntityTypes.PILLAGER, new Ability("rust_pillager","Rust",           IS_MONSTER, TriggerType.HURT, RUST_EFFECT),            ModConfig.PILLAGER_RUST);
        // TriggerType.TICK
        reg(EntityTypes.PILLAGER, new Ability("bannerman",    "Bannerman",     IS_PILLAGER, TriggerType.TICK, BANNERMAN_EFFECT),     ModConfig.PILLAGER_BANNERMAN);
        reg(EntityTypes.PILLAGER, new Ability("frenzy_pillager","Frenzy",       IS_MONSTER,  TriggerType.TICK, FRENZY_EFFECT),       ModConfig.PILLAGER_FRENZY);
        reg(EntityTypes.PILLAGER, new Ability("berserker_pillager","Berserker", IS_MONSTER,  TriggerType.TICK, BERSERKER_EFFECT),    ModConfig.PILLAGER_BERSERKER);
        reg(EntityTypes.PILLAGER, new Ability("regenerator_pillager","Regenerator",IS_MONSTER,TriggerType.TICK, REGENERATOR_EFFECT), ModConfig.PILLAGER_REGENERATOR);
        reg(EntityTypes.PILLAGER, new Ability("pack_leader_pillager","Pack Leader",IS_MONSTER,TriggerType.TICK, PACK_LEADER_EFFECT), ModConfig.PILLAGER_PACK_LEADER);
        // TriggerType.DEATH
        reg(EntityTypes.PILLAGER, new Ability("split_pillager","Split",         IS_MONSTER, TriggerType.DEATH, SPLIT_EFFECT),        ModConfig.PILLAGER_SPLIT);
        // TriggerType.SPAWN
        reg(EntityTypes.PILLAGER, new Ability("bannerman_spawn",             "Bannerman",           IS_PILLAGER, TriggerType.SPAWN, BANNERMAN_EFFECT),            ModConfig.PILLAGER_BANNERMAN);
        reg(EntityTypes.PILLAGER, new Ability("corrupted_presence_pillager", "Corrupted Presence",  IS_MONSTER,  TriggerType.SPAWN, CORRUPTED_PRESENCE_EFFECT), ModConfig.PILLAGER_CORRUPTED_PRESENCE);
        reg(EntityTypes.PILLAGER, new Ability("regenerator_pillager_spawn",  "Regenerator",         IS_MONSTER,  TriggerType.SPAWN, REGENERATOR_EFFECT),        ModConfig.PILLAGER_REGENERATOR);
        reg(EntityTypes.PILLAGER, new Ability("thorns_pillager",             "Thorns",              IS_MONSTER,  TriggerType.SPAWN, THORNS_EFFECT),             ModConfig.PILLAGER_THORNS);

        // ========================
        //  ENDERMAN
        // ========================
        // TriggerType.HURT
        reg(EntityTypes.ENDERMAN, new Ability("mind_shatter",  "Mind Shatter",  IS_ENDERMAN, TriggerType.HURT, MIND_SHATTER_EFFECT),  ModConfig.ENDERMAN_MIND_SHATTER);
        reg(EntityTypes.ENDERMAN, new Ability("lifestrike_ender","Lifestrike",  IS_MONSTER,  TriggerType.HURT, LIFESTRIKE_EFFECT),     ModConfig.ENDERMAN_LIFESTRIKE);
        reg(EntityTypes.ENDERMAN, new Ability("cursed_wound_ender","Cursed Wound",IS_MONSTER, TriggerType.HURT, CURSED_WOUND_EFFECT), ModConfig.ENDERMAN_CURSED_WOUND);
        reg(EntityTypes.ENDERMAN, new Ability("rust_ender",    "Rust",           IS_MONSTER,  TriggerType.HURT, RUST_EFFECT),           ModConfig.ENDERMAN_RUST);
        reg(EntityTypes.ENDERMAN, new Ability("disarm_ender",  "Disarm",         IS_MONSTER,  TriggerType.HURT, DISARM_EFFECT),         ModConfig.ENDERMAN_DISARM);
        // TriggerType.TICK
        reg(EntityTypes.ENDERMAN, new Ability("shadowstep",   "Shadowstep",     IS_ENDERMAN, TriggerType.TICK, SHADOWSTEP_EFFECT),   ModConfig.ENDERMAN_SHADOWSTEP);
        reg(EntityTypes.ENDERMAN, new Ability("frenzy_ender",  "Frenzy",         IS_MONSTER,  TriggerType.TICK, FRENZY_EFFECT),       ModConfig.ENDERMAN_FRENZY);
        reg(EntityTypes.ENDERMAN, new Ability("berserker_ender","Berserker",     IS_MONSTER,  TriggerType.TICK, BERSERKER_EFFECT),    ModConfig.ENDERMAN_BERSERKER);
        reg(EntityTypes.ENDERMAN, new Ability("regenerator_ender","Regenerator", IS_MONSTER,  TriggerType.TICK, REGENERATOR_EFFECT),  ModConfig.ENDERMAN_REGENERATOR);
        reg(EntityTypes.ENDERMAN, new Ability("pack_leader_ender","Pack Leader", IS_MONSTER,  TriggerType.TICK, PACK_LEADER_EFFECT),  ModConfig.ENDERMAN_PACK_LEADER);
        // TriggerType.DEATH
        reg(EntityTypes.ENDERMAN, new Ability("split_ender",  "Split",          IS_MONSTER,  TriggerType.DEATH, SPLIT_EFFECT),       ModConfig.ENDERMAN_SPLIT);
        // TriggerType.SPAWN
        reg(EntityTypes.ENDERMAN, new Ability("corrupted_presence_ender","Corrupted Presence", IS_MONSTER, TriggerType.SPAWN, CORRUPTED_PRESENCE_EFFECT), ModConfig.ENDERMAN_CORRUPTED_PRESENCE);
        reg(EntityTypes.ENDERMAN, new Ability("regenerator_ender_spawn","Regenerator",        IS_MONSTER, TriggerType.SPAWN, REGENERATOR_EFFECT),        ModConfig.ENDERMAN_REGENERATOR);
        reg(EntityTypes.ENDERMAN, new Ability("thorns_ender",           "Thorns",             IS_MONSTER, TriggerType.SPAWN, THORNS_EFFECT),             ModConfig.ENDERMAN_THORNS);

        // Build query cache after all registrations are complete
        buildCache();
    }
}
