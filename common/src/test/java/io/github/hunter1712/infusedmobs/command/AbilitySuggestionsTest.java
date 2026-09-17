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

    @Test
    void trailingSpaceMatchesByTokenNotSubstring() {
        List<String> ids = List.of("bane", "an", "ward");

        List<String> result = AbilitySuggestions.nextWords("bane ", true, ids);

        assertEquals(List.of("bane an", "bane ward"), result);
    }

    @Test
    void partialWordSuggestsExactTokenEvenWhenSubstringOfPicked() {
        List<String> ids = List.of("bane", "an");

        List<String> result = AbilitySuggestions.nextWords("bane an", false, ids);

        assertEquals(List.of("bane an"), result);
    }

    @Test
    void exactDuplicateWordIsNeverResuggested() {
        // "bane" already picked; typing "bane" again must not re-suggest it —
        // skipped by token equality even when the picked id equals the word.
        assertTrue(AbilitySuggestions.nextWords("bane bane", false, IDS).isEmpty());
    }

    @Test
    void singleExactWordStillSuggestsItself() {
        // First word being typed is not yet picked — exact match suggests itself.
        assertEquals(List.of("bane"), AbilitySuggestions.nextWords("bane", false, IDS));
    }

    @Test
    void pickedPrefixIsNotResuggested() {
        // "bane" picked; typing "b" must not offer "bane" again.
        assertTrue(AbilitySuggestions.nextWords("bane b", false, IDS).isEmpty());
    }
}
