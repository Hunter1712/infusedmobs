package io.github.hunter1712.mobabilities.ability;

/**
 * Enum representing when an ability triggers.
 */
public enum TriggerType {
    HURT,   // Triggered when mob hurts a player
    TICK,   // Applied every tick while active
    DEATH   // Triggered on mob death
}
