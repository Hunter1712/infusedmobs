package io.github.hunter1712.infusedmobs.platform;

import io.github.hunter1712.infusedmobs.test.IsolatedState;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Holder semantics: fail fast when unset, return the installed adapter.
 * Uses the test-classpath adapter so no fake with dozens of stubs is needed.
 */
@ExtendWith(IsolatedState.class)
class PlatformTest {

    @Test
    void failsFastWhenUnset() {
        assertThrows(IllegalStateException.class, Platform::hooks);
    }

    @Test
    void returnsInstalledAdapter() {
        VersionPlatform adapter = new VersionPlatform();
        Platform.setProvider(adapter);

        assertSame(adapter, Platform.hooks());
    }

    @Test
    void rejectsNullProvider() {
        assertThrows(NullPointerException.class, () -> Platform.setProvider(null));
    }
}
