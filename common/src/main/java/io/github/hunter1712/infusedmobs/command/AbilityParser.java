package io.github.hunter1712.infusedmobs.command;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * Parses the space-separated Ability argument for summon.
 * Preserves input order and reports unknown ids deduplicated in input order.
 */
final class AbilityParser {
    private AbilityParser() {}

    record Parsed(List<Ability> abilities, List<String> unknown) {}

    /** Splits on whitespace, dropping blanks — shared with {@link AbilitySuggestions}. */
    static List<String> words(String value) {
        if (value == null) return List.of();
        String trimmed = value.trim();
        if (trimmed.isEmpty()) return List.of();
        return Arrays.stream(trimmed.split("\\s+"))
                .filter(part -> !part.isEmpty())
                .toList();
    }

    static Parsed parse(String raw) {
        List<String> ids = words(raw).stream()
                .map(part -> part.toLowerCase(Locale.ROOT))
                .toList();
        if (ids.isEmpty()) return new Parsed(List.of(), List.of());

        List<Ability> abilities = AbilityRegistry.getAbilitiesByIds(ids);
        Set<String> found = new HashSet<>();
        for (Ability ability : abilities) found.add(ability.id());
        List<String> unknown = ids.stream()
                .filter(id -> !found.contains(id))
                .distinct()
                .toList();
        return new Parsed(abilities, unknown);
    }
}
