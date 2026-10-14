package io.github.hunter1712.infusedmobs.command;

import java.util.ArrayList;
import java.util.List;

/**
 * Pure blacklist add/remove behind the command seam.
 *
 * <p>Both the World Blacklist and Mob Blacklist subcommands share one
 * edit rule, so unit tests pin the add/remove/idempotent matrix without
 * a command source — handlers only persist and report.
 */
public final class BlacklistEdit {

    /** What an edit did. */
    public enum Outcome {
        ADDED,
        REMOVED,
        ALREADY_PRESENT,
        NOT_PRESENT
    }

    /** Updated list plus what happened. */
    public record Result(List<String> updated, Outcome outcome) {}

    private BlacklistEdit() {}

    /**
     * Adds or removes {@code id} from {@code current}.
     *
     * @param add true to add, false to remove
     * @return updated copy (never mutates {@code current}) and the outcome
     */
    public static Result edit(List<String> current, String id, boolean add) {
        List<String> snapshot = current == null ? List.of() : List.copyOf(current);
        boolean present = snapshot.contains(id);
        if (add && present) return new Result(snapshot, Outcome.ALREADY_PRESENT);
        if (!add && !present) return new Result(snapshot, Outcome.NOT_PRESENT);
        List<String> updated = new ArrayList<>(snapshot);
        if (add) {
            updated.add(id);
            return new Result(List.copyOf(updated), Outcome.ADDED);
        }
        updated.removeIf(id::equals);
        return new Result(List.copyOf(updated), Outcome.REMOVED);
    }
}
