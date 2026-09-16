package io.github.hunter1712.infusedmobs.platform;

/**
 * Holds the active {@link PlatformHooks} implementation.
 * <p>
 * The provider is set once during mod init to the Versioned Source Set's
 * own adapter. Tests install fakes via {@link #setProvider} and isolate
 * via the shared test extension.
 */
public final class Platform {
    private static PlatformHooks provider;

    private Platform() {}

    /** Installs the active implementation. Init and tests only. */
    public static void setProvider(PlatformHooks hooks) {
        provider = hooks;
    }

    /** Clears the active implementation. Test-only. */
    public static void resetForTests() {
        provider = null;
    }

    /** Returns the active implementation, failing fast when unset. */
    public static PlatformHooks hooks() {
        if (provider == null) {
            throw new IllegalStateException(
                    "Platform provider not set — mod init must install it first");
        }
        return provider;
    }
}
