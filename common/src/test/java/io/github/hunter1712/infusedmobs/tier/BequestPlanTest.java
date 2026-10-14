package io.github.hunter1712.infusedmobs.tier;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the Bequest pairing rule without Minecraft bootstrap: source is the
 * dead mob's own set minus Bequest itself, paired one-to-one in rolled
 * order onto the nearest qualifying heir (lacks it, under pool cap,
 * unused this death), sliding past collisions.
 */
class BequestPlanTest {

    private static List<MobTierManager.Grant> plan(List<String> deadIds, List<Set<String>> heirs, int poolCap) {
        return MobTierManager.planBequest(deadIds, heirs, poolCap);
    }

    @Test
    void singleTransferGoesToNearestHeir() {
        var grants = plan(List.of("bequest", "bane"), List.of(Set.of("ward"), Set.of()), 15);

        assertEquals(List.of(new MobTierManager.Grant(0, "bane")), grants);
    }

    @Test
    void soleBequestFizzles() {
        assertTrue(plan(List.of("bequest"), List.of(Set.of()), 15).isEmpty());
    }

    @Test
    void emptyDeadSetFizzles() {
        assertTrue(plan(List.of(), List.of(Set.of()), 15).isEmpty());
    }

    @Test
    void bequestItselfNeverMigrates() {
        var grants = plan(List.of("bequest", "bane", "thorns"),
                List.of(Set.of(), Set.of(), Set.of()), 15);

        assertTrue(grants.stream().noneMatch(g -> g.abilityId().equals("bequest")));
        assertEquals(2, grants.size());
    }

    @Test
    void duplicateSlidesToNextAlly() {
        var grants = plan(List.of("bequest", "bane"), List.of(Set.of("bane"), Set.of()), 15);

        assertEquals(List.of(new MobTierManager.Grant(1, "bane")), grants);
    }

    @Test
    void duplicateWithNoEligibleHeirFizzles() {
        var grants = plan(List.of("bequest", "bane"), List.of(Set.of("bane"), Set.of("bane")), 15);

        assertTrue(grants.isEmpty());
    }

    @Test
    void heirAtPoolCapIsSkipped() {
        var grants = plan(List.of("bequest", "bane"),
                List.of(Set.of("ward", "chill"), Set.of("ward")), 2);

        assertEquals(List.of(new MobTierManager.Grant(1, "bane")), grants);
    }

    @Test
    void onePerHeirMaxExcessFizzles() {
        var grants = plan(List.of("bequest", "bane", "thorns"), List.of(Set.of()), 15);

        assertEquals(1, grants.size());
        assertEquals(new MobTierManager.Grant(0, "bane"), grants.get(0));
    }

    @Test
    void excessHeirsStayUntouched() {
        var grants = plan(List.of("bequest", "bane"), List.of(Set.of(), Set.of()), 15);

        assertEquals(List.of(new MobTierManager.Grant(0, "bane")), grants);
    }

    @Test
    void overflowedHostMovesAllTransferable() {
        var grants = plan(List.of("bequest", "bane", "thorns", "ward", "chill"),
                List.of(Set.of(), Set.of(), Set.of(), Set.of()), 15);

        assertEquals(4, grants.size());
        assertEquals(List.of("bane", "thorns", "ward", "chill"),
                grants.stream().map(MobTierManager.Grant::abilityId).toList());
    }

    @Test
    void usedHeirIsSkippedForLaterAbilities() {
        // Nearest takes bane; thorns must slide past it even though it lacks thorns.
        var grants = plan(List.of("bequest", "bane", "thorns"),
                List.of(Set.of(), Set.of()), 15);

        assertEquals(List.of(
                new MobTierManager.Grant(0, "bane"),
                new MobTierManager.Grant(1, "thorns")), grants);
    }

    @Test
    void grantsNeverExceedTransferable() {
        // Conservation: a death removes N instances and adds at most N minus one.
        var grants = plan(List.of("bequest", "bane", "thorns", "ward"),
                List.of(Set.of(), Set.of(), Set.of(), Set.of(), Set.of()), 15);

        assertEquals(3, grants.size());
    }
}
