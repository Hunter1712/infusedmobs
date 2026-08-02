package io.github.hunter1712.infusedmobs.ability;

import io.github.hunter1712.infusedmobs.ability.effect.SplitEffect;
import io.github.hunter1712.infusedmobs.config.ModConfig;

import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Central registry containing the global mob ability pool.
 * <p>
 * Abilities are stored in a flat list for random sampling and indexed
 * by id for O(1) lookups. Registration fails fast on duplicate ids —
 * a silently duplicated ability would skew the random draw weights.
 */
public final class AbilityRegistry {

    private static final List<Ability> ALL_ABILITIES = new ArrayList<>();
    private static final Map<String, Ability> BY_ID = new HashMap<>();
    private static final EquipmentSlot[] ARMOR_SLOTS = {
            EquipmentSlot.HEAD, EquipmentSlot.CHEST, EquipmentSlot.LEGS, EquipmentSlot.FEET
    };

    private AbilityRegistry() {}

    // ========================================
    // Registration
    // ========================================

    /**
     * Populates the global ability pool. Must be called during mod init.
     * All abilities use vanilla effects — no custom status effects needed.
     * <p>
     * Effect lambdas read {@link ModConfig#get()} at fire time, so values
     * changed via {@code /infusedmobs reload} apply without a restart.
     */
    public static void registerAll() {
        // ---- HURT abilities (fire when the mob hits a player — melee or projectile) ----

        registerHurtEffect("bane",     "Bane",     MobEffects.POISON);
        registerHurtEffect("chill",    "Chill",    MobEffects.SLOWNESS);
        registerHurtEffect("decay",    "Decay",    MobEffects.WITHER);
        registerHurtEffect("hex",      "Hex",      MobEffects.WEAKNESS);

        all("hellfire", "Hellfire", TriggerType.HURT, (mob, target, damage) ->
                target.igniteForSeconds(ModConfig.get().infernoFireSeconds()));

        all("siphon", "Siphon", TriggerType.HURT, (mob, target, damage) -> {
            if (damage > 0) mob.heal(damage);
        });

        all("vitriol", "Vitriol", TriggerType.HURT, AbilityRegistry::damageArmor);

        // ---- TICK abilities (passive, refresh every 1 second while alive) ----

        registerTickEffect("ward",    "Ward",    MobEffects.RESISTANCE);
        registerTickEffect("frenzy",   "Frenzy",  MobEffects.STRENGTH);
        registerTickEffect("wraith",   "Wraith",  MobEffects.SPEED);
        registerTickEffect("blight",   "Blight",  MobEffects.REGENERATION);

        // Thorns: reactive TICK ability — no status effect, reflection handled in MobHurtTrigger
        all("thorns", "Thorns", TriggerType.TICK, (mob, target, damage) -> {});

        // ---- DEATH abilities ----

        all("rupture", "Rupture", TriggerType.DEATH, (mob, target, damage) -> SplitEffect.apply(mob));

        all("combust", "Combust", TriggerType.DEATH, (mob, target, damage) -> {
            if (mob.level() instanceof ServerLevel level) {
                double radius = ModConfig.get().combustExplosionPower() * 2.0;
                var entities = level.getEntities(mob, mob.getBoundingBox().inflate(radius));
                var dmgSource = level.damageSources().explosion(null, null);
                for (var entity : entities) {
                    if (entity instanceof LivingEntity living && entity != mob) {
                        double dist = entity.distanceTo(mob);
                        if (dist <= radius) {
                            float inflicted = (float) (4.0 * (1.0 - dist / radius));
                            living.hurtServer(level, dmgSource, Math.max(inflicted, 1.0f));
                        }
                    }
                }
                // Explosion sound without particles or block damage
                level.playSound(null, mob.getX(), mob.getY(), mob.getZ(),
                        SoundEvents.GENERIC_EXPLODE, SoundSource.BLOCKS, 1.0f, 1.0f);
            }
        });
    }

    /**
     * Registers a HURT ability that applies a status effect to the target.
     * Duration/amplifier are read from config at fire time.
     */
    private static void registerHurtEffect(String id, String name, Holder<MobEffect> effect) {
        all(id, name, TriggerType.HURT, (mob, target, damage) ->
                target.addEffect(new MobEffectInstance(
                        effect,
                        ModConfig.get().hurtEffectDuration(),
                        ModConfig.get().hurtEffectAmplifier())));
    }

    /**
     * Registers a TICK ability that applies a status effect to the mob itself.
     * Duration/amplifier are read from config at fire time.
     */
    private static void registerTickEffect(String id, String name, Holder<MobEffect> effect) {
        all(id, name, TriggerType.TICK, (mob, target, damage) ->
                mob.addEffect(new MobEffectInstance(
                        effect,
                        ModConfig.get().tickEffectDuration(),
                        ModConfig.get().tickEffectAmplifier(),
                        false, false, false)));
    }

    /**
     * Damages all 4 armor slots by the configurable durability amount.
     * {@code mob} and {@code damage} params unused — required by the
     * {@link AbilityEffect} signature.
     */
    private static void damageArmor(Mob mob, LivingEntity target, float damage) {
        if (!(target instanceof ServerPlayer player)) return;
        ServerLevel level = (ServerLevel) player.level();
        int dmg = ModConfig.get().acidArmorDamage();
        for (EquipmentSlot slot : ARMOR_SLOTS) {
            player.getItemBySlot(slot).hurtAndBreak(dmg, level, player, item -> {});
        }
    }

    /**
     * Builds and registers a single ability. Fails fast on duplicate ids.
     * Package-private: tests use it to populate the pool without
     * initialising Minecraft.
     */
    static void all(String id, String name, TriggerType trigger,
                    AbilityEffect effect) {
        if (BY_ID.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate ability id: '" + id + "'");
        }
        Ability ability = new Ability(id, name, trigger, effect);
        ALL_ABILITIES.add(ability);
        BY_ID.put(id, ability);
    }

    /**
     * Test hook — clears the registered pool. Not for production use.
     */
    static void resetForTests() {
        ALL_ABILITIES.clear();
        BY_ID.clear();
    }

    // ========================================
    // Query
    // ========================================

    /**
     * Returns {@code count} random abilities drawn from the unified
     * ability pool (all trigger types mixed together).
     * <p>
     * IDs in {@code excludedIds} are removed from the pool before drawing —
     * e.g. Rupture split copies draw with {@code "rupture"} excluded so a
     * copy can never split further; any other ability (including Combust)
     * is still available. There is no retry loop; the draw is filtered at
     * the pool level.
     *
     * @return a shuffled, unmodifiable list (may be shorter than {@code count})
     */
    public static List<Ability> getRandomAbilities(int count, String... excludedIds) {
        if (count <= 0 || ALL_ABILITIES.isEmpty()) return List.of();

        List<Ability> pool = new ArrayList<>(ALL_ABILITIES);
        if (excludedIds.length > 0) {
            Set<String> excluded = new HashSet<>(List.of(excludedIds));
            pool.removeIf(a -> excluded.contains(a.id()));
        }
        if (pool.isEmpty()) return List.of();

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        Collections.shuffle(pool, rng);
        List<Ability> result = pool.subList(0, Math.min(count, pool.size()));

        return Collections.unmodifiableList(result);
    }

    /**
     * Looks up abilities by their unique ID from the global pool.
     *
     * @param ids the ability IDs to look up
     * @return list of matching abilities in pool order (skips unknown IDs)
     */
    public static List<Ability> getAbilitiesByIds(List<String> ids) {
        Set<String> idSet = new HashSet<>(ids);
        List<Ability> result = new ArrayList<>();
        for (Ability ability : ALL_ABILITIES) {
            if (idSet.contains(ability.id())) {
                result.add(ability);
            }
        }
        return result;
    }

    /** Returns the ability with the given id, or null if unknown. */
    public static Ability getById(String id) {
        return BY_ID.get(id);
    }

    /**
     * Returns all registered ability IDs (e.g., "bane", "thorns", "rupture").
     * Useful for command tab-completions.
     */
    public static List<String> getAllAbilityIds() {
        return ALL_ABILITIES.stream()
                .map(Ability::id)
                .toList();
    }
}
