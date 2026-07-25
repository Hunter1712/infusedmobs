package io.github.hunter1712.mobabilities.config;

import io.github.hunter1712.mobabilities.ability.Ability;
import io.github.hunter1712.mobabilities.ability.AbilityRegistry;
import io.github.hunter1712.mobabilities.ability.TriggerType;
import io.github.hunter1712.mobabilities.ability.effect.ShadowstepEffect;
import net.minecraft.world.entity.EntityTypes;

import java.util.function.Predicate;

/**
 * Central configuration for all mob ability tuning parameters —
 * weights, durations, ranges, and probability thresholds.
 *
 * <p>All values are public static final constants so they can be
 * inlined by the JIT and changed in a single location. A future
 * iteration may load these from {@code config/mobabilities.json}.
 */
public final class ModConfig {

    private ModConfig() {
    }

    // ================================================================
    // Shadowstep
    // ================================================================

    /** Probability (0.0 – 1.0) that Shadowstep dodges an incoming attack. */
    public static final double SHADOWSTEP_DODGE_CHANCE = 0.15;

    /** Cooldown in ticks between Shadowstep attempts. */
    public static final int SHADOWSTEP_COOLDOWN_TICKS = 100; // 5 s

    /** Maximum teleport distance in blocks. */
    public static final double SHADOWSTEP_TELEPORT_RANGE = 8.0;

    // ================================================================
    // Effect durations (seconds — converted to ticks ×20)
    // ================================================================

    // ------- OFFENSIVE (HURT) -------
    public static final int PLAGUE_BEARER_POISON_SEC  = 10;
    public static final int PLAGUE_BEARER_HUNGER_SEC  = 15;
    public static final int VENOMOUS_BITE_POISON_SEC  = 10;
    public static final int CURSE_WOUND_DURATION_SEC  = 10;
    public static final int GRAVE_DUST_WITHER_SEC     = 10;

    // ------- PASSIVE (TICK) -------
    /** Interval in ticks at which passive tick triggers fire. */
    public static final int TICK_INTERVAL = 40; // 2 s

    /** Duration in seconds of each applied buff effect. */
    public static final int BUFF_DURATION_SEC = 2; // matches tick interval

    // ================================================================
    // Effect ranges (blocks)
    // ================================================================

    public static final double BANNERMAN_AURA_RANGE         = 15.0;
    public static final double CORRUPTED_PRESENCE_RANGE     = 10.0;
    public static final double CORROSIVE_SPLASH_RADIUS       = 3.0;
    public static final double PACK_LEADER_RANGE             = 10.0;

    // ================================================================
    // Per-mob-type ability selection weights
    // ================================================================

    // ---- ZOMBIE weights ----
    public static final int ZOMBIE_PLAGUE_BEARER    = 10;
    public static final int ZOMBIE_LIFESTRIKE       = 5;
    public static final int ZOMBIE_CURSED_WOUND     = 8;
    public static final int ZOMBIE_RUST             = 4;
    public static final int ZOMBIE_DISARM           = 3;
    public static final int ZOMBIE_BULWARK          = 10;
    public static final int ZOMBIE_FRENZY           = 7;
    public static final int ZOMBIE_BERSERKER        = 5;
    public static final int ZOMBIE_REGENERATOR      = 6;
    public static final int ZOMBIE_PACK_LEADER      = 4;
    public static final int ZOMBIE_SPLIT            = 6;
    public static final int ZOMBIE_HORDE_CALLER     = 5;
    public static final int ZOMBIE_THORNS           = 3;
    public static final int ZOMBIE_CORRUPTED_PRESENCE = 8;

    // ---- SKELETON weights ----
    public static final int SKELETON_GRAVE_DUST     = 10;
    public static final int SKELETON_SHIELD_BREAKER = 7;
    public static final int SKELETON_VOLLEY         = 8;
    public static final int SKELETON_BONE_ARMOR     = 10;
    public static final int SKELETON_FRENZY         = 6;
    public static final int SKELETON_BERSERKER      = 4;
    public static final int SKELETON_REGENERATOR    = 5;
    public static final int SKELETON_PACK_LEADER    = 3;
    public static final int SKELETON_SPLIT          = 5;
    public static final int SKELETON_THORNS         = 4;
    public static final int SKELETON_CORRUPTED_PRESENCE = 7;

    // ---- SPIDER weights ----
    public static final int SPIDER_VENOMOUS_BITE    = 12;
    public static final int SPIDER_LIFESTRIKE       = 4;
    public static final int SPIDER_CURSED_WOUND     = 5;
    public static final int SPIDER_RUST             = 3;
    public static final int SPIDER_DISARM           = 2;
    public static final int SPIDER_FRENZY           = 7;
    public static final int SPIDER_BERSERKER        = 5;
    public static final int SPIDER_REGENERATOR      = 4;
    public static final int SPIDER_PACK_LEADER      = 3;
    public static final int SPIDER_SPLIT            = 5;
    public static final int SPIDER_THORNS           = 3;
    public static final int SPIDER_CORRUPTED_PRESENCE = 6;

    // ---- CREEPER weights ----
    public static final int CREEPER_LIFESTRIKE        = 3;
    public static final int CREEPER_RUST              = 4;
    public static final int CREEPER_CORROSIVE_SPLASH  = 12;
    public static final int CREEPER_SPLIT             = 6;
    public static final int CREEPER_FRENZY            = 5;
    public static final int CREEPER_BERSERKER         = 6;
    public static final int CREEPER_SHADOWSTEP        = 4;
    public static final int CREEPER_REGENERATOR       = 5;
    public static final int CREEPER_THORNS            = 5;
    public static final int CREEPER_CORRUPTED_PRESENCE = 7;

    // ---- PILLAGER weights ----
    public static final int PILLAGER_SHIELD_BREAKER = 8;
    public static final int PILLAGER_DISARM         = 5;
    public static final int PILLAGER_LIFESTRIKE     = 4;
    public static final int PILLAGER_RUST           = 4;
    public static final int PILLAGER_BANNERMAN      = 10;
    public static final int PILLAGER_FRENZY         = 6;
    public static final int PILLAGER_BERSERKER      = 5;
    public static final int PILLAGER_REGENERATOR    = 4;
    public static final int PILLAGER_PACK_LEADER    = 7;
    public static final int PILLAGER_SPLIT          = 4;
    public static final int PILLAGER_THORNS         = 3;
    public static final int PILLAGER_CORRUPTED_PRESENCE = 6;

    // ---- ENDERMAN weights ----
    public static final int ENDERMAN_MIND_SHATTER   = 10;
    public static final int ENDERMAN_LIFESTRIKE     = 5;
    public static final int ENDERMAN_CURSED_WOUND   = 6;
    public static final int ENDERMAN_RUST           = 4;
    public static final int ENDERMAN_DISARM         = 5;
    public static final int ENDERMAN_SHADOWSTEP     = 12;
    public static final int ENDERMAN_FRENZY         = 5;
    public static final int ENDERMAN_BERSERKER      = 5;
    public static final int ENDERMAN_REGENERATOR    = 6;
    public static final int ENDERMAN_PACK_LEADER    = 4;
    public static final int ENDERMAN_SPLIT          = 7;
    public static final int ENDERMAN_THORNS         = 5;
    public static final int ENDERMAN_CORRUPTED_PRESENCE = 8;

    // ================================================================
    // Initialisation
    // ================================================================

    /**
     * Applies all configuration values to the runtime components that
     * depend on them.  Must be called during mod initialisation, after
     * {@link AbilityRegistry#registerAll()}.
     *
     * <p>Currently applies:
     * <ul>
     *   <li>Shadowstep probability to {@link ShadowstepEffect}</li>
     * </ul>
     */
    public static void onInitialize() {
        // Push Shadowstep dodge chance to the effect handler
        ShadowstepEffect.setDodgeChance(SHADOWSTEP_DODGE_CHANCE);
        ShadowstepEffect.setCooldownTicks(SHADOWSTEP_COOLDOWN_TICKS);
        ShadowstepEffect.setTeleportRange(SHADOWSTEP_TELEPORT_RANGE);

        // TODO: re-register ability weights if they are read from JSON
    }
}
