package io.github.hunter1712.infusedmobs.command;

import io.github.hunter1712.infusedmobs.tier.MobTier;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pure-logic tests for {@link InfusedMobsCommand} parsing helpers.
 * <p>
 * The ability registry is empty in unit tests (no Minecraft bootstrap), so
 * every id parses as "unknown" — these tests cover the parsing structure;
 * pool behaviour is covered by {@code AbilityRegistryTest}.
 */
class InfusedMobsCommandTest {

    // ========================================
    // AbilityParser (single parse shape for the summon command)
    // ========================================

    @Test
    void parseAbilitiesNullOrBlankReturnsEmpty() {
        assertTrue(AbilityParser.parse(null).unknown().isEmpty());
        assertTrue(AbilityParser.parse("").unknown().isEmpty());
        assertTrue(AbilityParser.parse("   ").unknown().isEmpty());
    }

    @Test
    void parseAbilitiesCollectsUnknownIdsInOrder() {
        AbilityParser.Parsed parsed = AbilityParser.parse("bane thorns");
        assertTrue(parsed.abilities().isEmpty());
        assertEquals(List.of("bane", "thorns"), parsed.unknown());
    }

    @Test
    void parseAbilitiesDeduplicatesAndIgnoresExtraWhitespace() {
        AbilityParser.Parsed parsed = AbilityParser.parse("  bane   bane  ");
        assertEquals(List.of("bane"), parsed.unknown());
    }

    // ========================================
    // MobTier.parse (single parse shape for the summon command)
    // ========================================

    @Test
    void parseTierIsCaseInsensitive() {
        assertEquals(MobTier.CINDER, MobTier.parse("cinder"));
        assertEquals(MobTier.SHADE, MobTier.parse("SHADE"));
        assertEquals(MobTier.DOOM, MobTier.parse("DoOm"));
    }

    @Test
    void parseTierReturnsNullForUnknown() {
        assertNull(MobTier.parse("legendary"));
        assertNull(MobTier.parse(""));
        assertNull(MobTier.parse(null));
    }

    // ========================================
    // FuzzyMatcher.closest (typo hints)
    // ========================================

    @Test
    void findClosestPrefersPrefixMatch() {
        assertEquals("bane", FuzzyMatcher.closest("ban", List.of("bane", "ward", "thorns")));
    }

    @Test
    void findClosestFindsCloseTypoWithinDistanceTwo() {
        assertEquals("bane", FuzzyMatcher.closest("bnae", List.of("bane", "ward")));
    }

    @Test
    void findClosestReturnsNullWhenTooFar() {
        assertNull(FuzzyMatcher.closest("xyzzy", List.of("bane", "ward")));
    }

    @Test
    void findClosestReturnsNullForEmptyCandidates() {
        assertNull(FuzzyMatcher.closest("bane", List.of()));
    }

    // ========================================
    // FuzzyMatcher.distance
    // ========================================

    @Test
    void levenshteinBasics() {
        assertEquals(0, FuzzyMatcher.distance("same", "same"));
        assertEquals(3, FuzzyMatcher.distance("", "abc"));
        assertEquals(3, FuzzyMatcher.distance("kitten", "sitting"));
        assertEquals(2, FuzzyMatcher.distance("bane", "bnae"));
    }

    @Test
    void levenshteinHandlesDifferentLengths() {
        assertEquals(2, FuzzyMatcher.distance("ba", "bane"));
    }
}
