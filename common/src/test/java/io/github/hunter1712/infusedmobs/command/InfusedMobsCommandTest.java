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
    // parseTier
    // ========================================

    @Test
    void parseTierIsCaseInsensitive() {
        assertEquals(MobTier.CINDER, InfusedMobsCommand.parseTier("cinder"));
        assertEquals(MobTier.SHADE, InfusedMobsCommand.parseTier("SHADE"));
        assertEquals(MobTier.DOOM, InfusedMobsCommand.parseTier("DoOm"));
    }

    @Test
    void parseTierReturnsNullForUnknown() {
        assertNull(InfusedMobsCommand.parseTier("legendary"));
        assertNull(InfusedMobsCommand.parseTier(""));
    }

    // ========================================
    // findClosest (typo hints)
    // ========================================

    @Test
    void findClosestPrefersPrefixMatch() {
        assertEquals("bane", InfusedMobsCommand.findClosest("ban", List.of("bane", "ward", "thorns")));
    }

    @Test
    void findClosestFindsCloseTypoWithinDistanceTwo() {
        assertEquals("bane", InfusedMobsCommand.findClosest("bnae", List.of("bane", "ward")));
    }

    @Test
    void findClosestReturnsNullWhenTooFar() {
        assertNull(InfusedMobsCommand.findClosest("xyzzy", List.of("bane", "ward")));
    }

    @Test
    void findClosestReturnsNullForEmptyCandidates() {
        assertNull(InfusedMobsCommand.findClosest("bane", List.of()));
    }

    // ========================================
    // levenshtein
    // ========================================

    @Test
    void levenshteinBasics() {
        assertEquals(0, InfusedMobsCommand.levenshtein("same", "same"));
        assertEquals(3, InfusedMobsCommand.levenshtein("", "abc"));
        assertEquals(3, InfusedMobsCommand.levenshtein("kitten", "sitting"));
        assertEquals(2, InfusedMobsCommand.levenshtein("bane", "bnae"));
    }

    @Test
    void levenshteinHandlesDifferentLengths() {
        assertEquals(2, InfusedMobsCommand.levenshtein("ba", "bane"));
    }
}
