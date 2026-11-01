package io.github.hunter1712.infusedmobs.ability.effect;

import io.github.hunter1712.infusedmobs.config.InstanceBuilder;
import io.github.hunter1712.infusedmobs.config.ModConfig;

import net.minecraft.world.entity.MobCategory;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Pins the pure conversion plan behind {@link BequestEffect#apply}: which
 * nearby vanilla mobs become Cinder and with which ability. Plain data — no
 * Minecraft bootstrap, matching {@code MobHurtTriggerTest} and
 * {@code MobTickTriggerTest}.
 */
class BequestEffectTest {

    private static BequestEffect.MobCandidate candidate(String entityKey, String category,
                                                        boolean tiered, boolean splitCopy,
                                                        double distance) {
        return new BequestEffect.MobCandidate(entityKey, category, tiered, splitCopy, distance);
    }

    private static ModConfig.Instance config() {
        return InstanceBuilder.valid().build();
    }

    @Test
    void inactiveGateConvertsNothing() {
        ModConfig.Instance config = config();

        List<BequestEffect.Conversion> conversions =
                BequestEffect.planConversions(List.of("bane"), List.of(candidate("minecraft:zombie",
                        MobCategory.MONSTER.name(), false, false, 1.0)), config, false);

        assertTrue(conversions.isEmpty());
    }

    @Test
    void noTransferableAbilityConvertsNothing() {
        ModConfig.Instance config = config();

        List<BequestEffect.Conversion> conversions =
                BequestEffect.planConversions(List.of("bequest"), List.of(candidate("minecraft:zombie",
                        MobCategory.MONSTER.name(), false, false, 1.0)), config, true);

        assertTrue(conversions.isEmpty());
    }

    @Test
    void soleBequestFollowedByAbilityIgnoresBequest() {
        ModConfig.Instance config = config();

        List<BequestEffect.Conversion> conversions =
                BequestEffect.planConversions(List.of("bequest", "bane"), List.of(candidate("minecraft:zombie",
                        MobCategory.MONSTER.name(), false, false, 1.0)), config, true);

        assertEquals(List.of(new BequestEffect.Conversion(0, "bane")), conversions);
    }

    @Test
    void firstNonBequestAbilityIsUsed() {
        ModConfig.Instance config = config();

        List<BequestEffect.Conversion> conversions =
                BequestEffect.planConversions(List.of("chill", "bane"), List.of(candidate("minecraft:zombie",
                        MobCategory.MONSTER.name(), false, false, 1.0)), config, true);

        assertEquals(List.of(new BequestEffect.Conversion(0, "chill")), conversions);
    }

    @Test
    void eachConvertGetsExactlyOneAbility() {
        ModConfig.Instance config = config();

        List<BequestEffect.Conversion> conversions =
                BequestEffect.planConversions(List.of("bane", "thorns"), List.of(
                        candidate("minecraft:zombie", MobCategory.MONSTER.name(), false, false, 1.0),
                        candidate("minecraft:skeleton", MobCategory.MONSTER.name(), false, false, 2.0)),
                        config, true);

        assertEquals(List.of(
                new BequestEffect.Conversion(0, "bane"),
                new BequestEffect.Conversion(1, "thorns")), conversions);
    }

    @Test
    void abilitiesRotateUntilExhaustedThenNone() {
        ModConfig.Instance config = config();

        // dead has bane, chill, ward; three eligible allies get one each, a fourth gets none
        List<BequestEffect.Conversion> conversions =
                BequestEffect.planConversions(List.of("bane", "chill", "ward"), List.of(
                        candidate("minecraft:zombie", MobCategory.MONSTER.name(), false, false, 1.0),
                        candidate("minecraft:skeleton", MobCategory.MONSTER.name(), false, false, 2.0),
                        candidate("minecraft:spider", MobCategory.MONSTER.name(), false, false, 3.0),
                        candidate("minecraft:creeper", MobCategory.MONSTER.name(), false, false, 4.0)),
                        config, true);

        assertEquals(List.of(
                new BequestEffect.Conversion(0, "bane"),
                new BequestEffect.Conversion(1, "chill"),
                new BequestEffect.Conversion(2, "ward")), conversions);
    }

    @Test
    void tieredAllyIsSkipped() {
        ModConfig.Instance config = config();

        List<BequestEffect.Conversion> conversions =
                BequestEffect.planConversions(List.of("bane"), List.of(candidate("minecraft:zombie",
                        MobCategory.MONSTER.name(), true, false, 1.0)), config, true);

        assertTrue(conversions.isEmpty());
    }

    @Test
    void splitCopyIsSkipped() {
        ModConfig.Instance config = config();

        List<BequestEffect.Conversion> conversions =
                BequestEffect.planConversions(List.of("bane"), List.of(candidate("minecraft:zombie",
                        MobCategory.MONSTER.name(), false, true, 1.0)), config, true);

        assertTrue(conversions.isEmpty());
    }

    @Test
    void nonMonsterIsSkipped() {
        ModConfig.Instance config = config();

        List<BequestEffect.Conversion> conversions =
                BequestEffect.planConversions(List.of("bane"), List.of(candidate("minecraft:zombie",
                        MobCategory.CREATURE.name(), false, false, 1.0)), config, true);

        assertTrue(conversions.isEmpty());
    }

    @Test
    void blacklistedEntityKeyIsSkipped() {
        ModConfig.Instance config = InstanceBuilder.valid()
                .mobBlacklist(List.of("minecraft:spider"))
                .build();

        List<BequestEffect.Conversion> conversions =
                BequestEffect.planConversions(List.of("bane"), List.of(candidate("minecraft:spider",
                        MobCategory.MONSTER.name(), false, false, 1.0)), config, true);

        assertTrue(conversions.isEmpty());
    }

    @Test
    void nullEntityKeyIsNotBlacklisted() {
        ModConfig.Instance config = InstanceBuilder.valid()
                .mobBlacklist(List.of("minecraft:spider"))
                .build();

        List<BequestEffect.Conversion> conversions =
                BequestEffect.planConversions(List.of("bane"), List.of(candidate(null,
                        MobCategory.MONSTER.name(), false, false, 1.0)), config, true);

        assertEquals(List.of(new BequestEffect.Conversion(0, "bane")), conversions);
    }

    @Test
    void nearestFirstOrderingPreserved() {
        ModConfig.Instance config = config();

        List<BequestEffect.Conversion> conversions =
                BequestEffect.planConversions(List.of("bane"), List.of(
                        candidate("minecraft:zombie", MobCategory.MONSTER.name(), false, false, 5.0),
                        candidate("minecraft:skeleton", MobCategory.MONSTER.name(), false, false, 1.0)),
                        config, true);

        assertEquals(List.of(
                new BequestEffect.Conversion(0, "bane")), conversions);
    }

    @Test
    void duplicateDeadIdsCollapseToFirstTransferable() {
        ModConfig.Instance config = config();

        List<BequestEffect.Conversion> conversions =
                BequestEffect.planConversions(List.of("bequest", "bequest", "bane"), List.of(candidate("minecraft:zombie",
                        MobCategory.MONSTER.name(), false, false, 1.0)), config, true);

        assertEquals(List.of(new BequestEffect.Conversion(0, "bane")), conversions);
    }
}
