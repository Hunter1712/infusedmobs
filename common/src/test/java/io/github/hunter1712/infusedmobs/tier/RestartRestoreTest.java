package io.github.hunter1712.infusedmobs.tier;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Restart-restore contract behind spec #27.
 * <p>
 * A world restart empties every in-memory map while the world save keeps the
 * persisted rolls. Restore must therefore be a pure function of the persisted
 * roll: the same roll in, the same Tier and abilities out, on every restart,
 * with no accumulation across consecutive restarts. Health itself needs no
 * accumulation guard on top because vanilla never persists modified attribute
 * bases (LivingEntity save data carries current Health and AbsorptionAmount
 * only), so each restore multiplies a fresh vanilla base exactly once.
 */
class RestartRestoreTest {

    /** Simulates one restart: snapshot the pre-restart store into a fresh post-restart store. */
    private static TierRollStore restart(TierRollStore before) {
        return new TierRollStore(before.snapshot());
    }

    @Test
    void restartPreservesExactRollForEveryVariant() {
        var before = new TierRollStore();
        var tieredId = UUID.randomUUID();
        var splitId = UUID.randomUUID();
        var nothingId = UUID.randomUUID();
        before.put(tieredId, new Rolled.Tiered(MobTier.DOOM, List.of("bane", "rupture")));
        before.put(splitId, new Rolled.Split(List.of("siphon")));
        before.put(nothingId, new Rolled.Nothing());

        var after = restart(before);

        assertEquals(new Rolled.Tiered(MobTier.DOOM, List.of("bane", "rupture")), after.get(tieredId));
        assertEquals(new Rolled.Split(List.of("siphon")), after.get(splitId));
        assertEquals(new Rolled.Nothing(), after.get(nothingId));
    }

    @Test
    void consecutiveRestartsNeverAccumulate() {
        var first = new TierRollStore();
        var id = UUID.randomUUID();
        first.put(id, new Rolled.Tiered(MobTier.SHADE, List.of("hex", "thorns")));

        var second = restart(first);
        var third = restart(second);

        assertEquals(first.get(id), second.get(id));
        assertEquals(second.get(id), third.get(id));
    }

    @Test
    void restartNeverPromotesSplitCopyToTiered() {
        var before = new TierRollStore();
        var id = UUID.randomUUID();
        before.put(id, new Rolled.Split(List.of("combust")));

        var rolled = restart(before).get(id);

        assertTrue(rolled instanceof Rolled.Split, "split copy must stay Split across restarts");
        assertTrue(!(rolled instanceof Rolled.Tiered), "split copy must never gain a tier on restart");
    }
}
