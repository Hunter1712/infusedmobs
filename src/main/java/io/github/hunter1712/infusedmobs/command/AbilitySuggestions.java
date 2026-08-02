package io.github.hunter1712.infusedmobs.command;

import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;

import net.minecraft.commands.CommandSourceStack;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Tab-completion provider for the space-separated ability argument of
 * {@code /infusedmobs summon}. Extracted from the command class so the
 * word-boundary logic stays unit-testable.
 */
public final class AbilitySuggestions {

    private AbilitySuggestions() {}

    /** Suggests ability IDs for the argument at the cursor. */
    public static CompletableFuture<Suggestions> suggest(
            CommandContext<CommandSourceStack> ctx, SuggestionsBuilder builder) {
        // getRemaining() returns text AFTER the cursor position, NOT the full argument value.
        // When cursor is at end of input, getRemaining() is "" even when text was typed.
        // Use getInput() + getStart() to reconstruct the actual argument content.
        String input = builder.getInput();
        int start = builder.getStart();

        // Find the beginning of this argument's value by walking back from start
        // to find the previous word boundary (space).
        int argStart = 0;
        for (int i = start - 1; i >= 0; i--) {
            if (input.charAt(i) == ' ') {
                argStart = i + 1;
                break;
            }
        }

        // Full text from the argument value start to end of input.
        // Keep trailing space so we can detect when user finished a word.
        String fullValue = input.substring(argStart);
        boolean hasTrailingSpace = fullValue.endsWith(" ");
        fullValue = fullValue.trim();

        for (String suggestion : nextWords(fullValue, hasTrailingSpace, AbilityRegistry.getAllAbilityIds())) {
            builder.suggest(suggestion);
        }
        return builder.buildFuture();
    }

    /**
     * Pure word-list logic: given the text typed so far, returns the full
     * suggestion strings to offer (already-picked abilities are skipped).
     * The input is trimmed internally, so callers may pass the raw value.
     */
    static List<String> nextWords(String fullValue, boolean hasTrailingSpace, List<String> allIds) {
        List<String> result = new ArrayList<>();
        fullValue = fullValue.trim();

        // Nothing typed yet — show all abilities
        if (fullValue.isEmpty()) {
            result.addAll(allIds);
            return result;
        }

        // User finished a word and pressed space — suggest the next unused ability
        if (hasTrailingSpace) {
            String prefix = fullValue + " ";
            for (String id : allIds) {
                if (!fullValue.contains(id)) {
                    result.add(prefix + id);
                }
            }
            return result;
        }

        // Determine the current word being typed (last segment)
        int lastSpace = fullValue.lastIndexOf(' ');
        String prefix = lastSpace >= 0 ? fullValue.substring(0, lastSpace + 1) : "";
        String currentWord = lastSpace >= 0 ? fullValue.substring(lastSpace + 1) : fullValue;

        // Only suggest when the current segment matches the start of an ability ID
        for (String id : allIds) {
            if (fullValue.contains(id)) continue; // already picked
            if (id.startsWith(currentWord)) {
                result.add(prefix + id);
            }
        }
        return result;
    }
}
