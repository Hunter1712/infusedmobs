package io.github.hunter1712.mobabilities.ability;

/**
 * Enum representing when an ability triggers.
 */
public enum TriggerType {
    SPAWN,       // Applied when mob spawns
    TICK,        // Applied every tick while active
    HURT,        // Triggered when mob hurts a player
    DEATH,       // Triggered on mob death
    PROJECTILE_HIT  // Triggered when mob's projectile hits a player
}
