package io.github.hunter1712.infusedmobs.ability;

/**
 * A data record representing a mob ability.
 * <p>
 * Each ability has a unique identifier, a display name, a trigger type
 * for when it activates, and the effect logic that executes when triggered.
 *
 * @param id      unique identifier (e.g., "bane")
 * @param name    display name (e.g., "Bane")
 * @param trigger when this ability activates
 * @param effect  the effect to execute — see {@link AbilityEffect}
 */
public record Ability(
        String id,
        String name,
        TriggerType trigger,
        AbilityEffect effect
) {
}
