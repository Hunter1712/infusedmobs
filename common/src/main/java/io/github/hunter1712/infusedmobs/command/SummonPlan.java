package io.github.hunter1712.infusedmobs.command;

import io.github.hunter1712.infusedmobs.tier.InfusionGate;
import io.github.hunter1712.infusedmobs.tier.MobTier;

import java.util.Arrays;
import java.util.Locale;
import java.util.Optional;

/**
 * Pure summon decisions behind the summon handler.
 *
 * <p>Tier parsing and Gating refusal wording live here so tests pin
 * them without a command source — the handler only spawns and reports.
 */
public final class SummonPlan {

    private SummonPlan() {}

    /** Parses a tier name, or empty when unknown. */
    public static Optional<MobTier> parseTier(String tierName) {
        return Optional.ofNullable(MobTier.parse(tierName));
    }

    /** Refusal text for the given gate status, or empty when active. */
    public static Optional<String> refusalFor(InfusionGate.Status status, String dimensionId) {
        return InfusionGate.refusalMessage(status, dimensionId);
    }

    /**
     * Tier options derived from the enum so summon errors and help text
     * never rot when a Tier is added. Owned here with tier parsing so all
     * summon Tier naming lives behind one seam.
     */
    public static String tierOptions() {
        return String.join(", ",
                Arrays.stream(MobTier.values())
                        .map(t -> t.name().toLowerCase(Locale.ROOT))
                        .toList());
    }
}
