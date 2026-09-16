package io.github.hunter1712.infusedmobs.ability;

/**
 * Opaque handle for a version-specific status effect.
 * Created only by version shims, consumed only by version shims —
 * shared code passes tokens through without inspecting them.
 */
public final class EffectToken {
    private final Object handle;

    private EffectToken(Object handle) {
        this.handle = handle;
    }

    public static EffectToken of(Object handle) {
        return new EffectToken(handle);
    }

    public Object handle() {
        return handle;
    }
}
