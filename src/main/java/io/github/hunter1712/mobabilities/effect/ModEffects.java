package io.github.hunter1712.mobabilities.effect;

import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.core.Holder;
import net.minecraft.resources.Identifier;

/**
 * Registry for custom {@link MobEffect} instances.
 *
 * <p>Effects are registered as static {@link Holder} references
 * during class initialisation. Call {@link #onInitialize()} from
 * the mod entrypoint to ensure the class loads and effects are
 * available throughout the game's lifecycle.
 */
public final class ModEffects {

    private ModEffects() {
        // static-only utility class
    }

    public static final Holder<MobEffect> CURSED_WOUND = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            Identifier.fromNamespaceAndPath("mobabilities", "cursed_wound"),
            new CursedWoundEffect()
    );

    public static final Holder<MobEffect> ACID = Registry.registerForHolder(
            BuiltInRegistries.MOB_EFFECT,
            Identifier.fromNamespaceAndPath("mobabilities", "acid"),
            new AcidEffect()
    );

    /**
     * Ensures the registry class is loaded and effects are registered.
     * Call from {@code ModInitializer.onInitialize()}.
     */
    public static void onInitialize() {
        // Static fields are already initialised; this method exists to
        // guarantee the class is loaded at the correct point in the
        // mod lifecycle.
    }
}
