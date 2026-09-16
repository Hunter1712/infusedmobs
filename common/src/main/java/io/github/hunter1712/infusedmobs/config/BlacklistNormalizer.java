package io.github.hunter1712.infusedmobs.config;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Normalises the World Blacklist: trims entries, drops blanks and nulls,
 * removes duplicates preserving first-seen order. Returns an immutable list.
 */
final class BlacklistNormalizer {
    private BlacklistNormalizer() {}

    static List<String> normalise(List<String> blacklist) {
        if (blacklist == null) return List.of();
        Set<String> seen = new LinkedHashSet<>();
        for (String entry : blacklist) {
            if (entry == null) continue;
            String trimmed = entry.trim();
            if (!trimmed.isEmpty()) seen.add(trimmed);
        }
        return List.copyOf(seen);
    }
}
