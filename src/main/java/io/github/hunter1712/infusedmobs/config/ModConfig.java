package io.github.hunter1712.infusedmobs.config;

import io.github.hunter1712.infusedmobs.tier.MobTier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * JSON-driven config loaded from {@code config/infusedmobs.json}.
 * <p>
 * Auto-creates with defaults on first run. All values are readable
 * at any time via {@link #get()}. The config is loaded during mod
 * initialisation and can be reloaded at runtime via {@link #load()}
 * or mutated via {@link #swapInstance(Instance)}.
 */
public final class ModConfig {

    private static Instance instance;
    private static final Gson GSON_PRETTY = new GsonBuilder().setPrettyPrinting().create();
    private static final Gson GSON = new Gson();

    private ModConfig() {}

    // ========================================
    // Lifecycle
    // ========================================

    /**
     * Loads config from disk or creates defaults.
     * Must be called during mod init before any other config reads.
     * <p>
     * Missing fields in an existing config file (e.g. when upgrading from a
     * previous version) are backfilled from {@link Instance#defaults()} so
     * users keep their existing tier settings across upgrades.
     */
    public static void load() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("infusedmobs.json");

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                Instance parsed = GSON.fromJson(json, Instance.class);
                if (parsed != null && parsed.isValid()) {
                    instance = parsed.backfillFromDefaults();
                    return;
                }
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

    /**
     * Atomically swaps the in-memory instance and persists to disk.
     * Used by runtime commands to modify live config values.
     */
    public static void swapInstance(Instance newInstance) {
        instance = newInstance;
        save();
    }

    /** Persists the current config to disk. */
    public static void save() {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("infusedmobs.json");
        try {
            Files.createDirectories(configPath.getParent());
            String json = GSON_PRETTY.toJson(instance);
            Files.writeString(configPath, json);
        } catch (IOException e) {
            // Non-critical — in-memory config is still correct
        }
    }

    private static void writeDefaults(Path path) {
        try {
            Files.createDirectories(path.getParent());
            String json = GSON_PRETTY.toJson(Instance.defaults());
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
            int abilityCount,
            double healthMultiplier,
            double xpMultiplier
    ) {}

    /** Root config object serialised to / from JSON. */
    public record Instance(
            TierConfig cinder,
            TierConfig shade,
            TierConfig doom,
            int hurtEffectDuration,
            int hurtEffectAmplifier,
            int tickEffectDuration,
            int tickEffectAmplifier,
            int infernoFireSeconds,
            int acidArmorDamage,
            float combustExplosionPower,
            boolean showNametags,
            boolean showAnnouncements,
            List<String> worldBlacklist,
            int configVersion
    ) {
        /** Bump when new config fields are added so {@link #backfillFromDefaults()} knows to fill them. */
        private static final int CURRENT_CONFIG_VERSION = 2;  // v1 = 2.6.0, v2 = 2.7.0 (added showAnnouncements, worldBlacklist)

        /** Returns true if all fields deserialised with valid values. */
        boolean isValid() {
            return cinder != null && shade != null && doom != null
                    && isTierValid(cinder) && isTierValid(shade) && isTierValid(doom)
                    && hurtEffectDuration > 0 && tickEffectDuration > 0
                    && infernoFireSeconds > 0 && acidArmorDamage > 0
                    && combustExplosionPower > 0
                    && (worldBlacklist == null
                            || worldBlacklist.stream().allMatch(Instance::isValidWorldId));
        }

        private static boolean isTierValid(TierConfig tc) {
            return tc.spawnChance() > 0
                    && tc.abilityCount() > 0
                    && tc.healthMultiplier() >= 1.0
                    && tc.xpMultiplier() >= 1.0;
        }

        /**
         * Returns true if {@code worldId} is a non-blank, non-null world identifier.
         * Used by {@link #isValid()} to reject malformed blacklist entries.
         */
        private static boolean isValidWorldId(String worldId) {
            if (worldId == null) return false;
            String trimmed = worldId.trim();
            if (trimmed.isEmpty()) return false;
            // A world id looks like "minecraft:overworld" — must contain a colon
            // and have non-empty namespace + path halves.
            int colon = trimmed.indexOf(':');
            if (colon <= 0 || colon >= trimmed.length() - 1) return false;
            return true;
        }

        /** Returns a copy with a new showNametags value. */
        public Instance withShowNametags(boolean show) {
            return new Instance(
                    cinder, shade, doom,
                    hurtEffectDuration, hurtEffectAmplifier,
                    tickEffectDuration, tickEffectAmplifier,
                    infernoFireSeconds, acidArmorDamage,
                    combustExplosionPower, show,
                    showAnnouncements, worldBlacklist, configVersion
            );
        }

        /** Returns a copy with a new showAnnouncements value. */
        public Instance withShowAnnouncements(boolean show) {
            return new Instance(
                    cinder, shade, doom,
                    hurtEffectDuration, hurtEffectAmplifier,
                    tickEffectDuration, tickEffectAmplifier,
                    infernoFireSeconds, acidArmorDamage,
                    combustExplosionPower, showNametags,
                    show, worldBlacklist, configVersion
            );
        }

        /**
         * Returns a copy with a new world blacklist.
         * The list is defensively copied and normalised (trimmed, blanks
         * removed, duplicates collapsed preserving first-seen order).
         */
        public Instance withWorldBlacklist(List<String> blacklist) {
            return new Instance(
                    cinder, shade, doom,
                    hurtEffectDuration, hurtEffectAmplifier,
                    tickEffectDuration, tickEffectAmplifier,
                    infernoFireSeconds, acidArmorDamage,
                    combustExplosionPower, showNametags,
                    showAnnouncements, normaliseBlacklist(blacklist), configVersion
            );
        }

        /**
         * Returns true if the given world identifier is on the blacklist.
         * {@code worldId} should be the string form of a dimension's
         * resource location (e.g. {@code "minecraft:overworld"}).
         * Matching is case-sensitive and trims the input.
         */
        public boolean isWorldBlacklisted(String worldId) {
            if (worldId == null || worldBlacklist == null) return false;
            String trimmed = worldId.trim();
            for (String entry : worldBlacklist) {
                if (entry != null && entry.equals(trimmed)) return true;
            }
            return false;
        }

        /** Returns the tier config matching the given enum member. */
        public TierConfig forTier(MobTier tier) {
            return switch (tier) {
                case CINDER -> cinder;
                case SHADE -> shade;
                case DOOM -> doom;
            };
        }

        /** Sensible default values. */
        public static Instance defaults() {
            return new Instance(
                    new TierConfig(0.4, 1, 1.5, 1.5),
                    new TierConfig(0.2, 2, 2.0, 2.0),
                    new TierConfig(0.1, 3, 4.0, 4.0),
                    60, 0,   // hurt: 3s, level I
                    60, 0,   // tick: 3s, level I
                    5,       // infernoFireSeconds
                    4,       // acidArmorDamage
                    4.0f,    // combustExplosionPower
                    true,    // showNametags
                    true,    // showAnnouncements
                    List.of(),// worldBlacklist — empty by default (mod active everywhere)
                    CURRENT_CONFIG_VERSION
            );
        }

        /**
         * Backfills fields missing from an older config file (pre-2.7.0)
         * with their default values, preserving all existing tier/effect
         * settings. Called after a successful {@link #isValid()} check.
         * <p>
         * Without this, upgrading from 2.6.0 would silently disable
         * announcements (Gson defaults missing booleans to {@code false})
         * and leave {@code worldBlacklist} null.
         */
        Instance backfillFromDefaults() {
            if (configVersion >= CURRENT_CONFIG_VERSION) {
                // Already current — just ensure worldBlacklist isn't null.
                return worldBlacklist == null
                        ? withWorldBlacklist(List.of())
                        : this;
            }
            // Older config — fill in the new fields with defaults.
            boolean backfilledAnnouncements = (configVersion < 2) ? true : showAnnouncements;
            List<String> backfilledBlacklist = (worldBlacklist == null) ? List.of() : normaliseBlacklist(worldBlacklist);
            return new Instance(
                    cinder, shade, doom,
                    hurtEffectDuration, hurtEffectAmplifier,
                    tickEffectDuration, tickEffectAmplifier,
                    infernoFireSeconds, acidArmorDamage,
                    combustExplosionPower, showNametags,
                    backfilledAnnouncements, backfilledBlacklist,
                    CURRENT_CONFIG_VERSION
            );
        }

        /**
         * Normalises a blacklist: trims entries, drops blanks/nulls, removes
         * duplicates preserving first-seen order. Returns an immutable list.
         */
        private static List<String> normaliseBlacklist(List<String> blacklist) {
            if (blacklist == null) return List.of();
            List<String> result = new ArrayList<>();
            for (String entry : blacklist) {
                if (entry == null) continue;
                String trimmed = entry.trim();
                if (trimmed.isEmpty() || result.contains(trimmed)) continue;
                result.add(trimmed);
            }
            return List.copyOf(result);
        }
    }
}
