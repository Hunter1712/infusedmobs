package io.github.hunter1712.infusedmobs.test;

import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.platform.Platform;
import io.github.hunter1712.infusedmobs.tier.InfusedTracker;

import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * Resets shared registries around each test so the suite stays hermetic
 * without manual reset calls.
 */
public final class IsolatedState implements BeforeEachCallback, AfterEachCallback {
    @Override
    public void beforeEach(ExtensionContext context) {
        reset();
    }

    @Override
    public void afterEach(ExtensionContext context) {
        reset();
    }

    private static void reset() {
        AbilityRegistry.resetForTests();
        InfusedTracker.clear();
        Platform.resetForTests();
    }
}
