package io.github.hunter1712.infusedmobs.config;

import io.github.hunter1712.infusedmobs.tier.MobTier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * JSON-driven config loaded from {@code config/infusedmobs.json}.
 * <p>
 * Auto-creates with defaults on first run. All values are readable
 * at any time via {@link #get()}. The config is loaded once during
 * mod initialisation and is immutable thereafter.
 */
public final class ModConfig {

    private static Instance instance;

    private ModConfig() {}

    // ========================================
    // Lifecycle
    // ========================================

    /**
     * Loads config from disk or creates defaults.
     * Must be called during mod init before any other config reads.
     */
    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("infusedmobs.json");

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                instance = new Gson().fromJson(json, Instance.class);
                return;
            } catch (IOException e) {
                // Fall through to defaults
            }
        }

        instance = Instance.defaults();
        writeDefaults(configPath);
    }

    /** Returns the current config instance. Never null after {@link #load()}. */
    public static Instance get() {
        return instance;
    }

    private static void writeDefaults(Path path) {
        try {
            Files.createDirectories(path.getParent());
            String json = new GsonBuilder().setPrettyPrinting().create().toJson(Instance.defaults());
            Files.writeString(path, json);
        } catch (IOException e) {
            // Defaults are already set in memory — file is non-critical
        }
    }

    // ========================================
    // Config data records
    // ========================================

    /** Per-tier overrides matching {@link MobTier} enum members. */
    public record TierConfig(
            double spawnChance,
            int hurtAbilities,
            int tickAbilities,
            int deathAbilities,
            double healthMultiplier,
            double xpMultiplier
    ) {}

    /** Root config object serialised to / from JSON. */
    public record Instance(
            TierConfig ember,
            TierConfig surge,
            TierConfig tempest,
            int hurtEffectDuration,
            int hurtEffectAmplifier,
            int tickEffectDuration,
            int tickEffectAmplifier,
            int infernoFireSeconds,
            int acidArmorDamage,
            float combustExplosionPower
    ) {
        /** Returns the tier config matching the given enum member. */
        public TierConfig forTier(MobTier tier) {
            return switch (tier) {
                case EMBER -> ember;
                case SURGE -> surge;
                case TEMPEST -> tempest;
            };
        }

        /** Sensible default values that match the original hardcoded behaviour. */
        public static Instance defaults() {
            return new Instance(
                    new TierConfig(0.5, 1, 0, 0, 3.0, 3.0),
                    new TierConfig(0.3, 1, 1, 0, 5.0, 5.0),
                    new TierConfig(0.15, 1, 1, 1, 8.0, 8.0),
                    100, 1,
                    60, 1,
                    5,
                    4,
                    4.0f
            );
        }
    }
}
