package io.github.hunter1712.infusedmobs.config;

import io.github.hunter1712.infusedmobs.tier.MobTier;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import net.fabricmc.loader.api.FabricLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
    private static final Logger LOGGER = LoggerFactory.getLogger("infusedmobs");
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

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
     *
     * @param abilityPoolSize live Ability pool size, the upper bound for
     *                        per-Tier ability counts. Passed in by the caller
     *                        (which owns pool-then-config init order) so this
     *                        module never pulls the Ability pool itself.
     */
    public static void load(int abilityPoolSize) {
        Path configPath = FabricLoader.getInstance().getConfigDir().resolve("infusedmobs.json");

        if (Files.exists(configPath)) {
            try {
                String json = Files.readString(configPath);
                Instance parsed;
                try {
                    parsed = GSON.fromJson(json, Instance.class);
                } catch (JsonSyntaxException malformed) {
                    LOGGER.warn("Config file {} is malformed JSON — rewriting with defaults.", configPath);
                    parsed = null;
                }
                if (parsed != null) {
                    Instance fixed = parsed.clamped(abilityPoolSize).backfillFromDefaults();
                    instance = fixed;
                    if (!fixed.equals(parsed)) {
                        save();
                    }
                    return;
                }
                LOGGER.warn("Config file {} is invalid — rewriting with defaults.", configPath);
            } catch (IOException e) {
                LOGGER.warn("Could not read config file {} — rewriting with defaults.", configPath, e);
            }
        }

        instance = Instance.defaults();
        writeDefaults(configPath);
    }

    /** Returns the current config instance. Never null after {@link #load(int)}. */
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
            String json = GSON.toJson(instance);
            Files.writeString(configPath, json);
        } catch (IOException e) {
            // Non-critical — in-memory config is still correct, but log so
            // silent disk failures are diagnosable.
            LOGGER.warn("Could not write config file {} — in-memory values remain active.", configPath, e);
        }
    }

    private static void writeDefaults(Path path) {
        try {
            Files.createDirectories(path.getParent());
            String json = GSON.toJson(Instance.defaults());
            Files.writeString(path, json);
        } catch (IOException e) {
            // Defaults are already set in memory — file is non-critical, but log.
            LOGGER.warn("Could not write default config file {}.", path, e);
        }
    }

    // ========================================
    // Config data records
    // ========================================

    /**
     * Per-tier tunables. Defaults derive from the matching {@link MobTier}
     * enum member — the enum is the single source of truth for the numbers.
     */
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
            List<String> worldBlacklist,
            int configVersion
    ) {
        /** Bump when config fields change so {@link #backfillFromDefaults()} knows what to fill. */
        private static final int CURRENT_CONFIG_VERSION = 3;  // v1 = 2.6.0, v2 = 2.7.0 (worldBlacklist), v3 = announcements removed

        /** Strict validity predicate pinned by tests. Load clamps instead of rejecting (see {@link #clamped}). */
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
         * Used by {@link #isValid()} to reject malformed World Blacklist entries.
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

        /** HURT/TICK effect amplifier range — vanilla caps at V (4), one headroom. */
        private static final int MAX_AMPLIFIER = 5;

        /** Combust explosion power range — 0.5 is a pop, 10 dwarfs TNT (4.0). */
        private static final float MIN_EXPLOSION_POWER = 0.5f;
        private static final float MAX_EXPLOSION_POWER = 10.0f;

        /**
         * Returns a copy with every out-of-range value clamped to its nearest
         * bound, logging one warning per fix naming the field with old and new
         * values. A single bad field no longer discards the whole file.
         * <p>
         * Bounds: Tier spawn chance in (0, 1] (non-positive falls back to the
         * Tier default — the open bound has no nearest valid value),
         * per-Tier ability count in [1, {@code abilityPoolSize}], HURT/TICK
         * amplifiers in [0, 5], Combust power in [0.5, 10]. Positive
         * durations, multipliers at or above 1.0 and colon-shaped World
         * Blacklist ids keep their existing rules; malformed ids are dropped.
         *
         * @param abilityPoolSize live Ability pool size (upper count bound)
         */
        Instance clamped(int abilityPoolSize) {
            List<String> warnings = new ArrayList<>();
            Instance fixed = clamped(abilityPoolSize, warnings);
            for (String warning : warnings) {
                LOGGER.warn(warning);
            }
            return fixed;
        }

        /**
         * Clamp worker collecting warnings instead of logging, so tests pin
         * the warnings without a log harness. Idempotent: clamping a clamped
         * instance changes nothing and warns nothing.
         */
        Instance clamped(int abilityPoolSize, List<String> warnings) {
            int upperCount = Math.max(1, abilityPoolSize);
            return new Instance(
                    clampedTier("cinder", cinder, MobTier.CINDER, upperCount, warnings),
                    clampedTier("shade", shade, MobTier.SHADE, upperCount, warnings),
                    clampedTier("doom", doom, MobTier.DOOM, upperCount, warnings),
                    clampMin("hurtEffectDuration", hurtEffectDuration, 1, warnings),
                    clampRange("hurtEffectAmplifier", hurtEffectAmplifier, 0, MAX_AMPLIFIER, warnings),
                    clampMin("tickEffectDuration", tickEffectDuration, 1, warnings),
                    clampRange("tickEffectAmplifier", tickEffectAmplifier, 0, MAX_AMPLIFIER, warnings),
                    clampMin("infernoFireSeconds", infernoFireSeconds, 1, warnings),
                    clampMin("acidArmorDamage", acidArmorDamage, 1, warnings),
                    clampExplosion(combustExplosionPower, warnings),
                    showNametags,
                    clampedBlacklist(worldBlacklist, warnings),
                    configVersion);
        }

        private static TierConfig clampedTier(String name, TierConfig tier, MobTier fallback,
                                              int upperCount, List<String> warnings) {
            if (tier == null) {
                warnings.add("Config field '" + name + "' is missing — using defaults " + fallback.defaultConfig() + ".");
                return fallback.defaultConfig();
            }
            double chance = tier.spawnChance();
            if (Double.isNaN(chance) || chance <= 0) {
                warnings.add("Config field '" + name + ".spawnChance' out of range (" + chance
                        + ") — using default " + fallback.spawnChance() + ".");
                chance = fallback.spawnChance();
            } else if (chance > 1) {
                warnings.add("Config field '" + name + ".spawnChance' out of range (" + chance
                        + ") — clamped to 1.0.");
                chance = 1.0;
            }
            int count = Math.min(Math.max(tier.abilityCount(), 1), upperCount);
            if (count != tier.abilityCount()) {
                warnings.add("Config field '" + name + ".abilityCount' out of range (" + tier.abilityCount()
                        + ") — clamped to " + count + ".");
            }
            double health = tier.healthMultiplier();
            if (Double.isNaN(health) || health < 1.0) {
                warnings.add("Config field '" + name + ".healthMultiplier' out of range (" + health
                        + ") — clamped to 1.0.");
                health = 1.0;
            }
            double xp = tier.xpMultiplier();
            if (Double.isNaN(xp) || xp < 1.0) {
                warnings.add("Config field '" + name + ".xpMultiplier' out of range (" + xp
                        + ") — clamped to 1.0.");
                xp = 1.0;
            }
            return new TierConfig(chance, count, health, xp);
        }

        private static int clampMin(String field, int value, int min, List<String> warnings) {
            if (value < min) {
                warnings.add("Config field '" + field + "' out of range (" + value
                        + ") — clamped to " + min + ".");
                return min;
            }
            return value;
        }

        private static int clampRange(String field, int value, int min, int max, List<String> warnings) {
            if (value < min || value > max) {
                int fixed = Math.min(Math.max(value, min), max);
                warnings.add("Config field '" + field + "' out of range (" + value
                        + ") — clamped to " + fixed + ".");
                return fixed;
            }
            return value;
        }

        private static float clampExplosion(float power, List<String> warnings) {
            if (Float.isNaN(power) || power < MIN_EXPLOSION_POWER || power > MAX_EXPLOSION_POWER) {
                float fixed = Float.isNaN(power) ? MIN_EXPLOSION_POWER
                        : Math.min(Math.max(power, MIN_EXPLOSION_POWER), MAX_EXPLOSION_POWER);
                warnings.add("Config field 'combustExplosionPower' out of range (" + power
                        + ") — clamped to " + fixed + ".");
                return fixed;
            }
            return power;
        }

        private static List<String> clampedBlacklist(List<String> blacklist, List<String> warnings) {
            List<String> normalised = normaliseBlacklist(blacklist);
            List<String> kept = normalised.stream().filter(Instance::isValidWorldId).toList();
            if (kept.size() != normalised.size()) {
                warnings.add("Config field 'worldBlacklist' dropped malformed ids " + normalised
                        + " — kept " + kept + ".");
            }
            return kept;
        }

        /** Returns a copy with a new showNametags value. */
        public Instance withShowNametags(boolean show) {
            return new Instance(
                    cinder, shade, doom,
                    hurtEffectDuration, hurtEffectAmplifier,
                    tickEffectDuration, tickEffectAmplifier,
                    infernoFireSeconds, acidArmorDamage,
                    combustExplosionPower, show,
                    worldBlacklist, configVersion
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
                    normaliseBlacklist(blacklist), configVersion
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

        /** Sensible default values — per-tier numbers come from {@link MobTier}. */
        public static Instance defaults() {
            return new Instance(
                    MobTier.CINDER.defaultConfig(),
                    MobTier.SHADE.defaultConfig(),
                    MobTier.DOOM.defaultConfig(),
                    60, 0,   // hurt: 3s, level I
                    60, 0,   // tick: 3s, level I
                    5,       // infernoFireSeconds
                    4,       // acidArmorDamage
                    4.0f,    // combustExplosionPower
                    true,    // showNametags
                    List.of(),// worldBlacklist — empty by default (mod active everywhere)
                    CURRENT_CONFIG_VERSION
            );
        }

        /**
         * Backfills fields missing from an older config file (pre-2.7.0)
         * with their default values, preserving all existing tier/effect
         * settings. Called on the clamped instance during {@link ModConfig#load(int)}.
         * <p>
         * Without this, upgrading from 2.6.0 would leave
         * {@code worldBlacklist} null. Older files may also carry a
         * {@code showAnnouncements} field — Gson ignores unknown fields,
         * so the removed field is simply dropped.
         */
        Instance backfillFromDefaults() {
            if (configVersion >= CURRENT_CONFIG_VERSION) {
                // Already current — just ensure worldBlacklist isn't null.
                return worldBlacklist == null
                        ? withWorldBlacklist(List.of())
                        : this;
            }
            // Older config — fill in the new fields with defaults.
            List<String> backfilledBlacklist = (worldBlacklist == null) ? List.of() : normaliseBlacklist(worldBlacklist);
            return new Instance(
                    cinder, shade, doom,
                    hurtEffectDuration, hurtEffectAmplifier,
                    tickEffectDuration, tickEffectAmplifier,
                    infernoFireSeconds, acidArmorDamage,
                    combustExplosionPower, showNametags,
                    backfilledBlacklist,
                    CURRENT_CONFIG_VERSION
            );
        }

        /**
         * Normalises a blacklist: trims entries, drops blanks/nulls, removes
         * duplicates preserving first-seen order. Returns an immutable list.
         */
        private static List<String> normaliseBlacklist(List<String> blacklist) {
            if (blacklist == null) return List.of();
            Set<String> seen = new LinkedHashSet<>();
            for (String entry : blacklist) {
                if (entry == null) continue;
                String trimmed = entry.trim();
                if (!trimmed.isEmpty()) seen.add(trimmed);
            }
            return List.copyOf(seen);
        }
    }
}
