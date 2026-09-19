package io.github.hunter1712.infusedmobs.config;

import java.util.List;

/**
 * One-liner builder for config records in tests.
 * Starts from valid filler values so each test names only what it varies.
 */
public final class InstanceBuilder {
    private static final ModConfig.TierConfig VALID_TIER =
            new ModConfig.TierConfig(0.1, 1, 1.0, 1.0);

    private ModConfig.TierConfig cinder = VALID_TIER;
    private ModConfig.TierConfig shade = VALID_TIER;
    private ModConfig.TierConfig doom = VALID_TIER;
    private int hurtEffectDuration = 60;
    private int hurtEffectAmplifier = 0;
    private int tickEffectDuration = 60;
    private int tickEffectAmplifier = 0;
    private int infernoFireSeconds = 5;
    private int acidArmorDamage = 4;
    private float combustExplosionPower = 4.0f;
    private boolean showNametags = true;
    private List<String> worldBlacklist = List.of();
    private List<String> mobBlacklist = List.of();
    private int configVersion = 4;

    private InstanceBuilder() {}

    public static InstanceBuilder valid() {
        return new InstanceBuilder();
    }

    /** Filler Tier values for tests that only vary other fields. */
    public static ModConfig.TierConfig validTier() {
        return VALID_TIER;
    }

    public InstanceBuilder cinder(ModConfig.TierConfig tier) {
        cinder = tier;
        return this;
    }

    public InstanceBuilder shade(ModConfig.TierConfig tier) {
        shade = tier;
        return this;
    }

    public InstanceBuilder doom(ModConfig.TierConfig tier) {
        doom = tier;
        return this;
    }

    public InstanceBuilder hurtAmplifier(int amplifier) {
        hurtEffectAmplifier = amplifier;
        return this;
    }

    public InstanceBuilder tickAmplifier(int amplifier) {
        tickEffectAmplifier = amplifier;
        return this;
    }

    public InstanceBuilder explosion(float power) {
        combustExplosionPower = power;
        return this;
    }

    public InstanceBuilder blacklist(List<String> blacklist) {
        worldBlacklist = blacklist;
        return this;
    }

    public InstanceBuilder mobBlacklist(List<String> blacklist) {
        mobBlacklist = blacklist;
        return this;
    }

    public InstanceBuilder version(int version) {
        configVersion = version;
        return this;
    }

    public ModConfig.Instance build() {
        return new ModConfig.Instance(
                cinder, shade, doom,
                hurtEffectDuration, hurtEffectAmplifier,
                tickEffectDuration, tickEffectAmplifier,
                infernoFireSeconds, acidArmorDamage,
                combustExplosionPower, showNametags,
                worldBlacklist, mobBlacklist, configVersion);
    }
}
