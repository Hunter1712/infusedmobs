package io.github.hunter1712.infusedmobs.ability;

import io.github.hunter1712.infusedmobs.ability.effect.SplitEffect;
import io.github.hunter1712.infusedmobs.config.ModConfig;
import io.github.hunter1712.infusedmobs.platform.Platform;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;

import java.util.List;

/**
 * Global facade over the shared mob ability pool.
 * Delegates to a shared {@link AbilityPool} instance so call sites stay
 * one-liners while tests can instantiate isolated pools directly.
 */
public final class AbilityRegistry {

    private static final AbilityPool SHARED = new AbilityPool();

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
     * HURT Abilities fire when an Infused Mob damages a player (melee or
     * projectile); TICK refreshes every second; DEATH fires on death.
     */
    public static void registerAll() {
        // ---- HURT abilities (fire when an Infused Mob hits a player — melee or projectile) ----

        registerHurtEffect("bane",     "Bane",     Platform.hooks().poison());
        registerHurtEffect("chill",    "Chill",    Platform.hooks().slowness());
        registerHurtEffect("decay",    "Decay",    Platform.hooks().wither());
        registerHurtEffect("hex",      "Hex",      Platform.hooks().weakness());

        register("hellfire", "Hellfire", TriggerType.HURT, (mob, target, damage) ->
                Platform.hooks().ignite(target, ModConfig.get().infernoFireSeconds()));

        register("siphon", "Siphon", TriggerType.HURT, (mob, target, damage) -> {
            if (damage > 0) mob.heal(damage);
        });

        register("vitriol", "Vitriol", TriggerType.HURT, AbilityRegistry::damageArmor);

        // ---- TICK abilities (passive, refresh every 1 second while alive) ----

        registerTickEffect("ward",    "Ward",    Platform.hooks().resistance());
        registerTickEffect("frenzy",   "Frenzy",  Platform.hooks().strength());
        registerTickEffect("wraith",   "Wraith",  Platform.hooks().speed());
        registerTickEffect("blight",   "Blight",  Platform.hooks().regeneration());

        // Thorns: reactive TICK ability — no status effect, reflection handled in MobHurtTrigger
        register("thorns", "Thorns", TriggerType.TICK, (mob, target, damage) -> {});

        // ---- DEATH abilities ----

        register("rupture", "Rupture", TriggerType.DEATH, (mob, target, damage) -> SplitEffect.apply(mob));

        register("combust", "Combust", TriggerType.DEATH, (mob, target, damage) -> {
            if (mob.level() instanceof ServerLevel level) {
                double radius = ModConfig.get().combustExplosionPower() * 2.0;
                var entities = level.getEntities(mob, mob.getBoundingBox().inflate(radius));
                var dmgSource = level.damageSources().explosion(null, null);
                for (var entity : entities) {
                    if (entity instanceof LivingEntity living && entity != mob) {
                        double dist = entity.distanceTo(mob);
                        if (dist <= radius) {
                            float inflicted = (float) (4.0 * (1.0 - dist / radius));
                            Platform.hooks().hurtFromExplosion(living, level, dmgSource, Math.max(inflicted, 1.0f));
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
     * The token is opaque (see {@link io.github.hunter1712.infusedmobs.platform.PlatformHooks}):
     * always obtained from the platform seam, never constructed directly.
     */
    private static void registerHurtEffect(String id, String name, EffectToken effect) {
        register(id, name, TriggerType.HURT, (mob, target, damage) ->
                Platform.hooks().applyHurtEffect(target, effect,
                        ModConfig.get().hurtEffectDuration(),
                        ModConfig.get().hurtEffectAmplifier()));
    }

    /**
     * Registers a TICK ability that applies a status effect to the mob itself.
     * Duration/amplifier are read from config at fire time.
     */
    private static void registerTickEffect(String id, String name, EffectToken effect) {
        register(id, name, TriggerType.TICK, (mob, target, damage) ->
                Platform.hooks().applyTickEffect(mob, effect,
                        ModConfig.get().tickEffectDuration(),
                        ModConfig.get().tickEffectAmplifier()));
    }

    /**
     * Damages all 4 armor slots by the configurable durability amount.
     * Delegates to the platform seam so per-version item handling is isolated.
     */
    private static void damageArmor(Mob mob, LivingEntity target, float damage) {
        if (!(target instanceof ServerPlayer player)) return;
        ServerLevel level = (ServerLevel) player.level();
        int dmg = ModConfig.get().acidArmorDamage();
        Platform.hooks().damageArmor(player, level, dmg);
    }

    /**
     * Builds and registers a single ability. Fails fast on duplicate ids.
     * Package-private: tests use it to populate the pool without
     * initialising Minecraft.
     */
    static void register(String id, String name, TriggerType trigger,
                         AbilityEffect effect) {
        SHARED.register(id, name, trigger, effect);
    }

    /**
     * Test hook — clears the registered pool. Not for production use.
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
     * Returns all registered ability IDs (e.g., "bane", "thorns", "rupture").
     * Useful for command tab-completions.
     */
    public static List<String> getAllAbilityIds() {
        return SHARED.allIds();
    }
}
