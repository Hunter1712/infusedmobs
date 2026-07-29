package io.github.hunter1712.infusedmobs.ability;

/**
 * Enum representing when an ability triggers.
 */
public enum TriggerType {
    HURT,   // Triggered when mob hurts a player (melee or projectile)
    TICK,   // Applied every second while the mob is alive
    DEATH   // Triggered on mob death
}
