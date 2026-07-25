package io.github.hunter1712.mobabilities.ability;

/**
 * A record pairing an {@link Ability} with its selection weight for
 * weighted-random ability selection in {@link AbilityRegistry}.
 *
 * @param ability the ability to assign
 * @param weight  the relative selection weight (higher = more likely)
 */
public record WeightedAbility(Ability ability, int weight) {
}
