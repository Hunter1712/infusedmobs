package io.github.hunter1712.infusedmobs.command;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.TestAbilities;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.test.IsolatedState;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Contract for Ability argument parsing: input order preserved,
 * unknowns reported deduplicated in input order.
 */
@ExtendWith(IsolatedState.class)
class AbilityParserTest {

    @Test
    void preservesInputOrderAndReportsUnknowns() {
        TestAbilities.register("bane", TriggerType.HURT);
        TestAbilities.register("ward", TriggerType.TICK);
        TestAbilities.register("chill", TriggerType.HURT);

        AbilityParser.Parsed parsed = AbilityParser.parse("ward bane nope ward nope");

        assertEquals(List.of("ward", "bane", "ward"),
                parsed.abilities().stream().map(Ability::id).toList());
        assertEquals(List.of("nope"), parsed.unknown());
    }
}
