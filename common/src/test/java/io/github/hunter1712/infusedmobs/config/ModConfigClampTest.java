package io.github.hunter1712.infusedmobs.config;

import io.github.hunter1712.infusedmobs.tier.MobTier;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins per-field config clamping: out-of-range values clamp to the nearest
 * bound with one warning naming the field and old/new values, valid values
 * pass through untouched, and clamping is idempotent so disk converges.
 */
class ModConfigClampTest {

    private static final int POOL = 14;

    private static List<String> clampWarnings(ModConfig.Instance instance) {
        List<String> warnings = new ArrayList<>();
        instance.clamped(POOL, warnings);
        return warnings;
    }

    private static ModConfig.Instance clamped(ModConfig.Instance instance) {
        return instance.clamped(POOL, new ArrayList<>());
    }

    @Test
    void spawnChanceAboveOneClampsDown() {
        var bad = new ModConfig.TierConfig(1.5, 1, 1.5, 1.5);
        var instance = InstanceBuilder.valid().cinder(bad).build();

        assertEquals(1.0, clamped(instance).cinder().spawnChance());
        List<String> warnings = clampWarnings(instance);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("cinder.spawnChance"));
        assertTrue(warnings.get(0).contains("1.5"));
    }

    @Test
    void spawnChanceAtOrBelowZeroFallsBackToDefault() {
        // The (0, 1] bound is open at zero — no nearest valid value exists,
        // so non-positive chances restore the Tier default.
        var zero = new ModConfig.TierConfig(0.0, 1, 1.5, 1.5);
        var negative = new ModConfig.TierConfig(-0.5, 1, 1.5, 1.5);

        assertEquals(MobTier.CINDER.spawnChance(),
                clamped(InstanceBuilder.valid().cinder(zero).build()).cinder().spawnChance());
        assertEquals(MobTier.CINDER.spawnChance(),
                clamped(InstanceBuilder.valid().cinder(negative).build()).cinder().spawnChance());
        assertEquals(1, clampWarnings(InstanceBuilder.valid().cinder(zero).build()).size());
    }

    @Test
    void abilityCountClampsToOneAndPoolSize() {
        var none = new ModConfig.TierConfig(0.4, 0, 1.5, 1.5);
        var huge = new ModConfig.TierConfig(0.4, 99, 1.5, 1.5);

        assertEquals(1, clamped(InstanceBuilder.valid().cinder(none).build()).cinder().abilityCount());
        assertEquals(POOL, clamped(InstanceBuilder.valid().cinder(huge).build()).cinder().abilityCount());
    }

    @Test
    void amplifiersClampToZeroAndFive() {
        var instance = InstanceBuilder.valid().hurtAmplifier(-1).tickAmplifier(9).build();

        ModConfig.Instance fixed = clamped(instance);
        assertEquals(0, fixed.hurtEffectAmplifier());
        assertEquals(5, fixed.tickEffectAmplifier());
        assertEquals(2, clampWarnings(instance).size());
    }

    @Test
    void explosionPowerClampsToHalfAndTen() {
        assertEquals(0.5f, clamped(InstanceBuilder.valid().explosion(0.1f).build()).combustExplosionPower());
        assertEquals(10.0f, clamped(InstanceBuilder.valid().explosion(99.0f).build()).combustExplosionPower());
    }

    @Test
    void multipliersBelowOneClampUp() {
        var bad = new ModConfig.TierConfig(0.4, 1, 0.5, 0.5);
        ModConfig.Instance fixed = clamped(InstanceBuilder.valid().cinder(bad).build());

        assertEquals(1.0, fixed.cinder().healthMultiplier());
        assertEquals(1.0, fixed.cinder().xpMultiplier());
    }

    @Test
    void missingTierRestoresDefaults() {
        var instance = InstanceBuilder.valid().cinder(null).build();

        assertEquals(MobTier.CINDER.defaultConfig(), clamped(instance).cinder());
        assertEquals(1, clampWarnings(instance).size());
    }

    @Test
    void malformedBlacklistIdsAreDropped() {
        var instance = InstanceBuilder.valid()
                .blacklist(List.of("minecraft:overworld", "overworld"))
                .build();

        ModConfig.Instance fixed = clamped(instance);
        assertEquals(List.of("minecraft:overworld"), fixed.worldBlacklist());
        List<String> warnings = clampWarnings(instance);
        assertEquals(1, warnings.size());
        assertTrue(warnings.get(0).contains("worldBlacklist"));
    }

    @Test
    void validValuesPassThroughUntouched() {
        ModConfig.Instance defaults = ModConfig.Instance.defaults();

        assertEquals(defaults, clamped(defaults));
        assertTrue(clampWarnings(defaults).isEmpty());
    }

    @Test
    void clampingIsIdempotent() {
        var bad = new ModConfig.TierConfig(2.0, 99, 0.5, 0.5);
        var instance = InstanceBuilder.valid()
                .cinder(bad).hurtAmplifier(-2).explosion(50.0f)
                .blacklist(List.of("bogus")).build();

        ModConfig.Instance once = clamped(instance);
        List<String> secondWarnings = new ArrayList<>();

        assertEquals(once, once.clamped(POOL, secondWarnings));
        assertTrue(secondWarnings.isEmpty(), "re-clamping must warn nothing so disk converges");
    }
}
