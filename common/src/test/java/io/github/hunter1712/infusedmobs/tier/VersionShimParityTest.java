package io.github.hunter1712.infusedmobs.tier;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the remaining version shims (#9): dimension accessor, ability effect
 * handling, spawn helper, HURT trigger registration and the two mixins must
 * produce the same Infused Mob behaviour on 26.2, 1.21.1 and 1.20.1.
 *
 * <p>Pure-logic file-content tests (no Minecraft bootstrap): they guard
 * against divergence between the per-version shims and against
 * version-specific APIs leaking into {@code common}.
 */
class VersionShimParityTest {

    private static String readShim(String version, String relative) throws Exception {
        String suffix = relative;
        String[] candidates = {
                version + "/src/main/java/" + suffix,
                "../" + version + "/src/main/java/" + suffix,
                "../../" + version + "/src/main/java/" + suffix
        };
        for (String cand : candidates) {
            Path p = Path.of(cand);
            if (Files.exists(p)) return Files.readString(p);
            Path abs = Path.of(System.getProperty("user.dir")).resolve(cand);
            if (Files.exists(abs)) return Files.readString(abs);
        }
        Path root = Path.of(System.getProperty("user.dir"));
        for (int i = 0; i < 5; i++) {
            Path p = root.resolve(version + "/src/main/java/" + suffix);
            if (Files.exists(p)) return Files.readString(p);
            root = root.getParent();
            if (root == null) break;
        }
        throw new java.io.FileNotFoundException("Cannot find " + version + "/" + suffix);
    }

    private static String readResource(String version, String name) throws Exception {
        String[] candidates = {
                version + "/src/main/resources/" + name,
                "../" + version + "/src/main/resources/" + name,
                "../../" + version + "/src/main/resources/" + name
        };
        for (String cand : candidates) {
            Path p = Path.of(cand);
            if (Files.exists(p)) return Files.readString(p);
            Path abs = Path.of(System.getProperty("user.dir")).resolve(cand);
            if (Files.exists(abs)) return Files.readString(abs);
        }
        Path root = Path.of(System.getProperty("user.dir"));
        for (int i = 0; i < 5; i++) {
            Path p = root.resolve(version + "/src/main/resources/" + name);
            if (Files.exists(p)) return Files.readString(p);
            root = root.getParent();
            if (root == null) break;
        }
        throw new java.io.FileNotFoundException("Cannot find " + version + "/" + name);
    }

    private static String readCommon(String relative) throws Exception {
        String[] candidates = {
                "common/src/main/java/" + relative,
                "../common/src/main/java/" + relative,
                "../../common/src/main/java/" + relative
        };
        for (String cand : candidates) {
            Path p = Path.of(cand);
            if (Files.exists(p)) return Files.readString(p);
            Path abs = Path.of(System.getProperty("user.dir")).resolve(cand);
            if (Files.exists(abs)) return Files.readString(abs);
        }
        Path root = Path.of(System.getProperty("user.dir"));
        for (int i = 0; i < 5; i++) {
            Path p = root.resolve("common/src/main/java/" + relative);
            if (Files.exists(p)) return Files.readString(p);
            root = root.getParent();
            if (root == null) break;
        }
        throw new java.io.FileNotFoundException("Cannot find common/" + relative);
    }

    private static final String DIM = "io/github/hunter1712/infusedmobs/tier/DimensionHelper.java";
    private static final String ABIL = "io/github/hunter1712/infusedmobs/util/AbilityHelper.java";
    private static final String SPAWN = "io/github/hunter1712/infusedmobs/util/SpawnHelper.java";
    private static final String HURT = "io/github/hunter1712/infusedmobs/ability/trigger/HurtTriggerHelper.java";
    private static final String CMD = "io/github/hunter1712/infusedmobs/command/CommandArgHelper.java";

    // ========================================
    // 1. Tier assignment gate — dimension accessor per version
    // ========================================

    @Test
    void dimensionAccessorPerVersion() throws Exception {
        String modern = readShim("26.2", DIM);
        assertTrue(modern.contains(".identifier()"), "26.2 DimensionHelper must use identifier()");
        assertTrue(modern.contains("getId(ServerLevel"), "26.2 DimensionHelper must expose getId(ServerLevel)");

        for (String version : new String[]{"1.21.1", "1.20.1"}) {
            String shim = readShim(version, DIM);
            assertTrue(shim.contains(".location()"), version + " DimensionHelper must use location()");
            assertTrue(shim.contains("getId(ServerLevel"), version + " DimensionHelper must expose getId(ServerLevel)");
            assertTrue(shim.contains("level.dimension()"), version + " must read level.dimension()");
        }
    }

    // ========================================
    // 2. Ability effects — holders vs raw, damage, items
    // ========================================

    @Test
    void abilityEffectHandlesPerVersion() throws Exception {
        String modern = readShim("26.2", ABIL);
        assertTrue(modern.contains("MobEffects.SLOWNESS"), "26.2 must use SLOWNESS");
        assertTrue(modern.contains("MobEffects.RESISTANCE"), "26.2 must use RESISTANCE");
        assertTrue(modern.contains("MobEffects.STRENGTH"), "26.2 must use STRENGTH");
        assertTrue(modern.contains("MobEffects.SPEED"), "26.2 must use SPEED");

        for (String version : new String[]{"1.21.1", "1.20.1"}) {
            String shim = readShim(version, ABIL);
            assertTrue(shim.contains("MobEffects.MOVEMENT_SLOWDOWN"), version + " must use MOVEMENT_SLOWDOWN");
            assertTrue(shim.contains("MobEffects.DAMAGE_RESISTANCE"), version + " must use DAMAGE_RESISTANCE");
            assertTrue(shim.contains("MobEffects.DAMAGE_BOOST"), version + " must use DAMAGE_BOOST");
            assertTrue(shim.contains("MobEffects.MOVEMENT_SPEED"), version + " must use MOVEMENT_SPEED");
        }

        // Shared effects keep the same names on all versions
        for (String version : new String[]{"26.2", "1.21.1", "1.20.1"}) {
            String shim = readShim(version, ABIL);
            for (String effect : new String[]{"MobEffects.POISON", "MobEffects.WITHER",
                    "MobEffects.WEAKNESS", "MobEffects.REGENERATION"}) {
                assertTrue(shim.contains(effect), version + " must map " + effect);
            }
            assertTrue(shim.contains("applyHurtEffect"), version + " must expose applyHurtEffect");
            assertTrue(shim.contains("applyTickEffect"), version + " must expose applyTickEffect");
            assertTrue(shim.contains("damageArmor"), version + " must expose damageArmor");
            assertTrue(shim.contains("reflectThorns"), version + " must expose reflectThorns");
            assertTrue(shim.contains("hurtFromExplosion"), version + " must expose hurtFromExplosion");
            assertTrue(shim.contains("ignite("), version + " must expose ignite");
        }
    }

    // ========================================
    // 3. HURT trigger registration — AFTER_DAMAGE vs ALLOW_DAMAGE
    // ========================================

    @Test
    void hurtTriggerRegistrationPerVersion() throws Exception {
        for (String version : new String[]{"26.2", "1.21.1"}) {
            String shim = readShim(version, HURT);
            assertTrue(shim.contains("AFTER_DAMAGE"), version + " must register AFTER_DAMAGE");
            assertTrue(shim.contains("HurtHandler"), version + " must adapt HurtHandler");
        }

        // 1.20.1 FAPI has no AFTER_DAMAGE: ALLOW_DAMAGE adaptor that never cancels.
        // Divergences (pre-mitigation amount, blocked=false) are accepted: exact
        // parity is impossible without the post-mitigation event.
        String legacy = readShim("1.20.1", HURT);
        assertTrue(legacy.contains("ALLOW_DAMAGE"), "1.20.1 must register ALLOW_DAMAGE");
        assertTrue(legacy.contains("return true"), "1.20.1 adaptor must never cancel damage");
        assertTrue(legacy.contains("HurtHandler"), "1.20.1 must adapt HurtHandler");
        assertTrue(!legacy.contains("owned by #9"),
                "1.20.1 divergences are accepted in #9, not an open TODO");
    }

    // ========================================
    // 4. Spawn helper — REINFORCEMENT vs plain create
    // ========================================

    @Test
    void spawnHelperPerVersion() throws Exception {
        for (String version : new String[]{"26.2", "1.21.1", "1.20.1"}) {
            String shim = readShim(version, SPAWN);
            assertTrue(shim.contains("create(EntityType"), version + " must expose create(EntityType, ServerLevel)");
        }
        assertTrue(readShim("26.2", SPAWN).contains("EntitySpawnReason.REINFORCEMENT"),
                "26.2 must spawn split copies as REINFORCEMENT");
        for (String version : new String[]{"1.21.1", "1.20.1"}) {
            assertTrue(readShim(version, SPAWN).contains("type.create(level)"),
                    version + " must use plain create(Level)");
        }
    }

    // ========================================
    // 5. Mixins fire on all versions — XP scaling + despawn cleanup
    // ========================================

    @Test
    void mixinsPresentOnAllVersions() throws Exception {
        List<String> versions = List.of("26.2", "1.21.1", "1.20.1");
        List<String> levels = List.of("JAVA_25", "JAVA_21", "JAVA_17");
        for (int i = 0; i < versions.size(); i++) {
            String json = readResource(versions.get(i), "infusedmobs.mixins.json");
            assertTrue(json.contains("LivingEntityMixin"), versions.get(i) + " must list LivingEntityMixin (XP scaling)");
            assertTrue(json.contains("EntityRemoveMixin"), versions.get(i) + " must list EntityRemoveMixin (despawn cleanup)");
            assertTrue(json.contains(levels.get(i)), versions.get(i) + " must declare " + levels.get(i));
        }

        String xpMixin = readCommon("io/github/hunter1712/infusedmobs/mixin/LivingEntityMixin.java");
        assertTrue(xpMixin.contains("getExperienceReward"), "XP mixin must target getExperienceReward");
        assertTrue(xpMixin.contains("xpMultiplier()"), "XP mixin must scale via config xpMultiplier");

        String removeMixin = readCommon("io/github/hunter1712/infusedmobs/mixin/EntityRemoveMixin.java");
        assertTrue(removeMixin.contains("DISCARDED"), "Remove mixin must clean up on DISCARDED");
    }

    // ========================================
    // 6. Command args + TICK/DEATH triggers — same wiring per version
    // ========================================

    @Test
    void commandArgsAndTickDeathTriggers() throws Exception {
        // CommandArgHelper: same seam on all versions, Identifier vs ResourceLocation inside
        for (String version : new String[]{"26.2", "1.21.1", "1.20.1"}) {
            String cmd = readShim(version, CMD);
            for (String seam : new String[]{"worldId(", "getWorldId", "defaultEntity()",
                    "entityKey(", "createForCommand(", "gamemaster()"}) {
                assertTrue(cmd.contains(seam), version + " CommandArgHelper must expose " + seam);
            }
        }
        assertTrue(readShim("26.2", CMD).contains("IdentifierArgument"), "26.2 commands must use IdentifierArgument");
        for (String version : new String[]{"1.21.1", "1.20.1"}) {
            assertTrue(readShim(version, CMD).contains("ResourceLocationArgument"),
                    version + " commands must use ResourceLocationArgument");
        }

        // TICK + DEATH triggers live in common (no version split): stable Fabric APIs
        String tick = readCommon("io/github/hunter1712/infusedmobs/ability/trigger/MobTickTrigger.java");
        assertTrue(tick.contains("END_SERVER_TICK"), "TICK must fire on END_SERVER_TICK");
        assertTrue(tick.contains("getTickMobUUIDs"), "TICK must only process tick-capable mobs");
        String death = readCommon("io/github/hunter1712/infusedmobs/ability/trigger/MobDeathTrigger.java");
        assertTrue(death.contains("AFTER_DEATH"), "DEATH must fire on AFTER_DEATH");
        assertTrue(death.contains("TriggerType.DEATH"), "DEATH must fire DEATH abilities");
        assertTrue(death.contains("removeMob"), "DEATH must clean up tracking");
    }

    // ========================================
    // 7. Common stays version-neutral — no API leaks past the shims
    // ========================================

    @Test
    void commonHasNoVersionSpecificApiLeaks() throws Exception {
        List<String> commonFiles = List.of(
                "io/github/hunter1712/infusedmobs/tier/MobTierManager.java",
                "io/github/hunter1712/infusedmobs/tier/InfusionGate.java",
                "io/github/hunter1712/infusedmobs/tier/InfusedTracker.java",
                "io/github/hunter1712/infusedmobs/ability/AbilityRegistry.java",
                "io/github/hunter1712/infusedmobs/ability/effect/SplitEffect.java",
                "io/github/hunter1712/infusedmobs/ability/trigger/MobHurtTrigger.java",
                "io/github/hunter1712/infusedmobs/ability/trigger/MobTickTrigger.java",
                "io/github/hunter1712/infusedmobs/ability/trigger/MobDeathTrigger.java",
                "io/github/hunter1712/infusedmobs/command/InfusedMobsCommand.java");
        // Call/import patterns that must only appear inside per-version shims
        List<String> forbidden = List.of(
                "IdentifierArgument", "ResourceLocationArgument", "EntitySpawnReason",
                "import net.minecraft.world.effect.MobEffects",
                "new MobEffectInstance(", ".hurtServer(", ".igniteForSeconds(",
                ".setSecondsOnFire(", ".hurtAndBreak(", ".identifier()", ".location()");
        for (String file : commonFiles) {
            String text = readCommon(file);
            for (String pattern : forbidden) {
                assertTrue(!text.contains(pattern),
                        "common/" + file + " must not contain '" + pattern + "' (use a shim)");
            }
        }
    }
}
