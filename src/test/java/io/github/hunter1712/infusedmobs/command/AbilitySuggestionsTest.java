package io.github.hunter1712.infusedmobs.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pure-logic tests for {@link AbilitySuggestions#nextWords} — the
 * word-boundary completion logic behind the summon ability argument.
 */
class AbilitySuggestionsTest {

    private static final List<String> IDS = List.of("bane", "chill", "ward", "rupture");

    @Test
    void emptyInputSuggestsAllIds() {
        assertEquals(IDS, AbilitySuggestions.nextWords("", false, IDS));
    }

    @Test
    void trailingSpaceSuggestsUnusedIdsWithPrefix() {
        List<String> result = AbilitySuggestions.nextWords("bane ", true, IDS);
        assertEquals(List.of("bane chill", "bane ward", "bane rupture"), result);
    }

    @Test
    void partialWordSuggestsOnlyMatchingIds() {
        List<String> result = AbilitySuggestions.nextWords("ba", false, IDS);
        assertEquals(List.of("bane"), result);
    }

    @Test
    void secondWordFiltersAgainstAlreadyPicked() {
        List<String> result = AbilitySuggestions.nextWords("bane ru", false, IDS);
        assertEquals(List.of("bane rupture"), result);
    }

    @Test
    void noMatchesYieldsEmpty() {
        assertTrue(AbilitySuggestions.nextWords("zzz", false, IDS).isEmpty());
    }
}
