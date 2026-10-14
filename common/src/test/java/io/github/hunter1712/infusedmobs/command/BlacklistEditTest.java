package io.github.hunter1712.infusedmobs.command;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class BlacklistEditTest {

    @Test
    void addInsertsMissing() {
        BlacklistEdit.Result result = BlacklistEdit.edit(List.of("a"), "b", true);
        assertEquals(BlacklistEdit.Outcome.ADDED, result.outcome());
        assertEquals(List.of("a", "b"), result.updated());
    }

    @Test
    void addIsIdempotent() {
        BlacklistEdit.Result result = BlacklistEdit.edit(List.of("a"), "a", true);
        assertEquals(BlacklistEdit.Outcome.ALREADY_PRESENT, result.outcome());
        assertEquals(List.of("a"), result.updated());
    }

    @Test
    void removeDeletesPresent() {
        BlacklistEdit.Result result = BlacklistEdit.edit(List.of("a", "b"), "a", false);
        assertEquals(BlacklistEdit.Outcome.REMOVED, result.outcome());
        assertEquals(List.of("b"), result.updated());
    }

    @Test
    void removeIsIdempotent() {
        BlacklistEdit.Result result = BlacklistEdit.edit(List.of(), "a", false);
        assertEquals(BlacklistEdit.Outcome.NOT_PRESENT, result.outcome());
        assertEquals(List.of(), result.updated());
    }
}
