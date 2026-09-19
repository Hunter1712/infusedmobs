package io.github.hunter1712.infusedmobs.ability;

/**
 * Enum representing when an ability triggers.
 * HURT covers both directions: offensive Abilities fire when an Infused Mob
 * damages a player (melee or mob-owned projectile); Thorns fires reactively
 * when an Infused Mob is damaged by a player, reflecting a fraction back.
 */
public enum TriggerType {
    HURT,   // Triggered on damage involving an Infused Mob and a player (both directions)
    TICK,   // Applied every second while the mob is alive
    DEATH   // Triggered on mob death
}
