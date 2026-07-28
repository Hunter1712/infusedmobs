package io.github.hunter1712.infusedmobs.ability;

/**
 * Enum representing when an ability triggers.
 */
public enum TriggerType {
    HURT,   // Triggered when mob hurts a player in melee
    TICK,   // Applied every tick while active
    DEATH   // Triggered on mob death
}
