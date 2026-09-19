package io.github.hunter1712.infusedmobs.ability;

import io.github.hunter1712.infusedmobs.ability.effect.CombustEffect;
import io.github.hunter1712.infusedmobs.ability.effect.SplitEffect;
import io.github.hunter1712.infusedmobs.config.ModConfig;
import io.github.hunter1712.infusedmobs.platform.Platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Global Ability pool: registration, id lookup in input order,
 * random draw with exclusions, and id listing.
 * <p>
 * Live code uses the shared static facade for one-liner call sites; tests
 * instantiate a fresh registry per case with no manual reset.
 */
public final class AbilityRegistry {

    private final List<Ability> all = new ArrayList<>();
    private final Map<String, Ability> byId = new HashMap<>();

    private static final AbilityRegistry SHARED = new AbilityRegistry();

    // ========================================
    // Registration
    // ========================================

    /** The Thorns Ability id, shared by registration and both HURT trigger branches. */
    public static final String THORNS_ID = "thorns";

    /** Fraction of incoming damage reflected by the Thorns Ability. */
    private static final float THORNS_REFLECT_FRACTION = 0.15f;

    /**
     * Populates the global ability pool. Must be called during mod init.
     * All abilities use vanilla effects — no custom status effects needed.
     * <p>
     * Effect lambdas read {@link ModConfig#get()} at fire time, so values
     * changed via {@code /infusedmobs reload} apply without a restart.
     * HURT Abilities fire on damage: offensive HURT Abilities fire when an
     * Infused Mob damages a player (melee or projectile); Thorns fires
     * reactively when an Infused Mob is damaged by a player, reflecting a
     * fraction back. TICK refreshes every second; DEATH fires on death.
     */
    public static void registerAll() {
        // ---- HURT abilities (fire when an Infused Mob hits a player — melee or projectile) ----

        registerHurtEffect("bane",     "Bane",     Platform.hooks().effectToken("poison"));
        registerHurtEffect("chill",    "Chill",    Platform.hooks().effectToken("slowness"));
        registerHurtEffect("decay",    "Decay",    Platform.hooks().effectToken("wither"));
        registerHurtEffect("hex",      "Hex",      Platform.hooks().effectToken("weakness"));

        register("hellfire", "Hellfire", TriggerType.HURT, (mob, target, damage) ->
                Platform.hooks().ignite(target, ModConfig.get().infernoFireSeconds()));

        register("siphon", "Siphon", TriggerType.HURT, (mob, target, damage) -> {
            if (damage > 0) mob.heal(damage);
        });

        register("vitriol", "Vitriol", TriggerType.HURT, AbilityRegistry::damageArmor);

        register(THORNS_ID, "Thorns", TriggerType.HURT, AbilityRegistry::reflectThorns);

        // ---- TICK abilities (passive, refresh every 1 second while alive) ----

        registerTickEffect("ward",    "Ward",    Platform.hooks().effectToken("resistance"));
        registerTickEffect("frenzy",   "Frenzy",  Platform.hooks().effectToken("strength"));
        registerTickEffect("wraith",   "Wraith",  Platform.hooks().effectToken("speed"));
        registerTickEffect("blight",   "Blight",  Platform.hooks().effectToken("regeneration"));

        // ---- DEATH abilities ----

        register("rupture", "Rupture", TriggerType.DEATH, (mob, target, damage) -> SplitEffect.apply(mob));

        register("combust", "Combust", TriggerType.DEATH, (mob, target, damage) -> CombustEffect.apply(mob));
    }

    /**
     * Registers a HURT ability that applies a status effect to the target.
     * Duration/amplifier are read from config at fire time.
     * The token is opaque (see {@link io.github.hunter1712.infusedmobs.platform.PlatformHooks}):
     * always obtained from the platform seam, never constructed directly.
     */
    private static void registerHurtEffect(String id, String name, EffectToken effect) {
        register(id, name, TriggerType.HURT, (mob, target, damage) ->
                effect.applyHurt(target,
                        ModConfig.get().hurtEffectDuration(),
                        ModConfig.get().hurtEffectAmplifier()));
    }

    /**
     * Registers a TICK ability that applies a status effect to the mob itself.
     * Duration/amplifier are read from config at fire time.
     */
    private static void registerTickEffect(String id, String name, EffectToken effect) {
        register(id, name, TriggerType.TICK, (mob, target, damage) ->
                effect.applyTick(mob,
                        ModConfig.get().tickEffectDuration(),
                        ModConfig.get().tickEffectAmplifier()));
    }

    /**
     * Damages all 4 armor slots by the configurable durability amount.
     * Delegates to the platform seam so per-version item handling is isolated.
     */
    private static void damageArmor(Mob mob, LivingEntity target, float damage) {
        if (!(target instanceof ServerPlayer player)) return;
        if (!(player.level() instanceof ServerLevel level)) return;
        int armorDamage = ModConfig.get().acidArmorDamage();
        Platform.hooks().damageArmor(player, level, armorDamage);
    }

    /**
     * Reflects a fraction of incoming damage back at the attacker.
     * Fired reactively when an Infused Mob is damaged by a player;
     * reflection damage uses {@code THORNS} type so it never re-triggers
     * Abilities. Called through the Ability effect so the TriggerType
     * listing stays honest — no cross-trigger side-channel.
     */
    private static void reflectThorns(Mob mob, LivingEntity target, float damage) {
        if (!(target instanceof Player player)) return;
        float reflected = damage * THORNS_REFLECT_FRACTION;
        if (reflected > 0.0f && mob.level() instanceof ServerLevel level) {
            Platform.hooks().reflectThorns(player, mob, reflected, level);
        }
    }

    /**
     * Builds and registers a single ability on the shared pool. Fails fast on
     * duplicate ids. Package-private: tests use it to populate the pool without
     * initialising Minecraft.
     */
    static void register(String id, String name, TriggerType trigger,
                         AbilityEffect effect) {
        SHARED.add(id, name, trigger, effect);
    }

    /**
     * Test hook — clears the shared pool. Not for production use.
     * Public so shared test extensions can isolate state without reflection.
     */
    public static void resetForTests() {
        SHARED.clear();
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
        return SHARED.random(count, excludedIds);
    }

    /**
     * Looks up abilities by their unique ID from the global pool.
     *
     * @param ids the ability IDs to look up
     * @return list of matching abilities in input order (skips unknown IDs)
     */
    public static List<Ability> getAbilitiesByIds(List<String> ids) {
        return SHARED.byIds(ids);
    }

    /** Returns the ability with the given id, or null if unknown. */
    public static Ability getById(String id) {
        return SHARED.byId(id);
    }

    /**
     * Returns the abilities in {@code abilities} matching the given trigger,
     * in order. Pure list filter behind trigger queries — no registry state.
     */
    public static List<Ability> forTrigger(List<Ability> abilities, TriggerType trigger) {
        List<Ability> result = new ArrayList<>(abilities.size());
        for (Ability ability : abilities) {
            if (ability.trigger() == trigger) result.add(ability);
        }
        return result;
    }

    /**
     * Returns all registered ability IDs (e.g., "bane", "thorns", "rupture").
     * Useful for command tab-completions.
     */
    public static List<String> getAllAbilityIds() {
        return SHARED.allIds();
    }

    // ========================================
    // Instance core — a fresh registry per test, no manual reset
    // ========================================

    public void add(String id, String name, TriggerType trigger, AbilityEffect effect) {
        if (byId.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate ability id: '" + id + "'");
        }
        Ability ability = new Ability(id, name, trigger, effect);
        all.add(ability);
        byId.put(id, ability);
    }

    public void clear() {
        all.clear();
        byId.clear();
    }

    public List<Ability> random(int count, String... excludedIds) {
        if (count <= 0 || all.isEmpty()) return List.of();
        List<Ability> pool = new ArrayList<>(all);
        if (excludedIds.length > 0) {
            Set<String> excluded = new HashSet<>(List.of(excludedIds));
            pool.removeIf(a -> excluded.contains(a.id()));
        }
        if (pool.isEmpty()) return List.of();
        Collections.shuffle(pool, ThreadLocalRandom.current());
        return Collections.unmodifiableList(pool.subList(0, Math.min(count, pool.size())));
    }

    public List<Ability> byIds(List<String> ids) {
        List<Ability> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            Ability ability = byId.get(id);
            if (ability != null) result.add(ability);
        }
        return result;
    }

    public Ability byId(String id) {
        return byId.get(id);
    }

    public List<String> allIds() {
        return all.stream().map(Ability::id).toList();
    }
}
