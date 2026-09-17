package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TriggerType;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;

/**
 * Immutable per-mob infused state, mirroring {@link Rolled}
 * with abilities resolved to live objects and pre-indexed by trigger.
 * Split copies are a distinct variant — no null-tier polymorphism.
 * Uses CONTEXT.md vocabulary: Infused Mob, Tier, Ability, TriggerType.
 */
sealed interface InfusedMob permits InfusedMob.Tiered, InfusedMob.SplitCopy {

    List<Ability> abilities();

    EnumMap<TriggerType, List<Ability>> byTrigger();

    /** Abilities matching the given trigger (O(1) lookup). */
    default List<Ability> forTrigger(TriggerType trigger) {
        return byTrigger().getOrDefault(trigger, List.of());
    }

    record Tiered(MobTier tier, List<Ability> abilities,
                  EnumMap<TriggerType, List<Ability>> byTrigger) implements InfusedMob {}

    record SplitCopy(List<Ability> abilities,
                     EnumMap<TriggerType, List<Ability>> byTrigger) implements InfusedMob {}

    static InfusedMob tiered(MobTier tier, List<Ability> abilities) {
        return new Tiered(tier, abilities, index(abilities));
    }

    static InfusedMob split(List<Ability> abilities) {
        return new SplitCopy(abilities, index(abilities));
    }

    private static EnumMap<TriggerType, List<Ability>> index(List<Ability> abilities) {
        EnumMap<TriggerType, List<Ability>> index = new EnumMap<>(TriggerType.class);
        for (Ability ability : abilities) {
            index.computeIfAbsent(ability.trigger(), t -> new ArrayList<>()).add(ability);
        }
        index.replaceAll((t, list) -> List.copyOf(list));
        return index;
    }
}
