package io.github.hunter1712.infusedmobs.tier;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Guards the platform seam: one adapter per Versioned Source Set implements
 * the shared interface with the correct version tokens inside.
 *
 * <p>Pure-logic file-content tests (no Minecraft bootstrap): they guard
 * against divergence between the per-version adapters and against
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

    private static final String ADAPTER = "io/github/hunter1712/infusedmobs/platform/VersionPlatform.java";

    // ========================================
    // 1. Adapters implement the shared seam
    // ========================================

    @Test
    void adaptersImplementSharedSeam() throws Exception {
        for (String version : new String[]{"26.2", "1.21.1", "1.20.1"}) {
            String adapter = readShim(version, ADAPTER);
            assertTrue(adapter.contains("implements PlatformHooks"),
                    version + " adapter must implement PlatformHooks");
            for (String seam : new String[]{"dimensionId(", "spawnEntity(", "spawnForCommand(",
                    "entityKey(", "defaultEntity(", "gamemasterPermission(",
                    "worldIdArgument(", "worldIdFromCommand(",
                    "applyHurtEffect(", "applyTickEffect(", "damageArmor(",
                    "reflectThorns(", "hurtFromExplosion(", "ignite(",
                    "registerHurtTrigger(", "loadRoll(", "storeRoll(", "clearRoll("}) {
                assertTrue(adapter.contains(seam), version + " adapter must expose " + seam);
            }
        }
    }

    // ========================================
    // 2. Tier assignment gate — dimension accessor per version
    // ========================================

    @Test
    void dimensionAccessorPerVersion() throws Exception {
        String modern = readShim("26.2", ADAPTER);
        assertTrue(modern.contains(".identifier()"), "26.2 adapter must use identifier()");
        assertTrue(modern.contains("dimensionId(ServerLevel"), "26.2 adapter must expose dimensionId(ServerLevel)");

        for (String version : new String[]{"1.21.1", "1.20.1"}) {
            String adapter = readShim(version, ADAPTER);
            assertTrue(adapter.contains(".location()"), version + " adapter must use location()");
            assertTrue(adapter.contains("dimensionId(ServerLevel"), version + " adapter must expose dimensionId(ServerLevel)");
            assertTrue(adapter.contains("level.dimension()"), version + " must read level.dimension()");
        }
    }

    // ========================================
    // 3. Ability effects — holders vs raw, damage, items
    // ========================================

    @Test
    void abilityEffectHandlesPerVersion() throws Exception {
        String modern = readShim("26.2", ADAPTER);
        assertTrue(modern.contains("MobEffects.SLOWNESS"), "26.2 must use SLOWNESS");
        assertTrue(modern.contains("MobEffects.RESISTANCE"), "26.2 must use RESISTANCE");
        assertTrue(modern.contains("MobEffects.STRENGTH"), "26.2 must use STRENGTH");
        assertTrue(modern.contains("MobEffects.SPEED"), "26.2 must use SPEED");

        for (String version : new String[]{"1.21.1", "1.20.1"}) {
            String adapter = readShim(version, ADAPTER);
            assertTrue(adapter.contains("MobEffects.MOVEMENT_SLOWDOWN"), version + " must use MOVEMENT_SLOWDOWN");
            assertTrue(adapter.contains("MobEffects.DAMAGE_RESISTANCE"), version + " must use DAMAGE_RESISTANCE");
            assertTrue(adapter.contains("MobEffects.DAMAGE_BOOST"), version + " must use DAMAGE_BOOST");
            assertTrue(adapter.contains("MobEffects.MOVEMENT_SPEED"), version + " must use MOVEMENT_SPEED");
        }

        // Shared effects keep the same names on all versions
        for (String version : new String[]{"26.2", "1.21.1", "1.20.1"}) {
            String adapter = readShim(version, ADAPTER);
            for (String effect : new String[]{"MobEffects.POISON", "MobEffects.WITHER",
                    "MobEffects.WEAKNESS", "MobEffects.REGENERATION"}) {
                assertTrue(adapter.contains(effect), version + " must map " + effect);
            }
            assertTrue(adapter.contains("EffectToken.of("), version + " must wrap handles as tokens");
            assertTrue(adapter.contains("effect.handle()"), version + " must unwrap tokens at the boundary");
        }
    }

    // ========================================
    // 4. HURT trigger registration — AFTER_DAMAGE vs ALLOW_DAMAGE
    // ========================================

    @Test
    void hurtTriggerRegistrationPerVersion() throws Exception {
        for (String version : new String[]{"26.2", "1.21.1"}) {
            String adapter = readShim(version, ADAPTER);
            assertTrue(adapter.contains("AFTER_DAMAGE"), version + " must register AFTER_DAMAGE");
            assertTrue(adapter.contains("HurtHandler"), version + " must adapt HurtHandler");
        }

        // 1.20.1 FAPI has no AFTER_DAMAGE: ALLOW_DAMAGE adaptor that never cancels.
        // Divergences (pre-mitigation amount, blocked=false) are accepted: exact
        // parity is impossible without the post-mitigation event.
        String legacy = readShim("1.20.1", ADAPTER);
        assertTrue(legacy.contains("ALLOW_DAMAGE"), "1.20.1 must register ALLOW_DAMAGE");
        assertTrue(legacy.contains("return true"), "1.20.1 adaptor must never cancel damage");
        assertTrue(legacy.contains("HurtHandler"), "1.20.1 must adapt HurtHandler");
        assertTrue(!legacy.contains("owned by #9"),
                "1.20.1 divergences are accepted in #9, not an open TODO");
    }

    // ========================================
    // 5. Spawn helper — REINFORCEMENT vs plain create
    // ========================================

    @Test
    void spawnHelperPerVersion() throws Exception {
        for (String version : new String[]{"26.2", "1.21.1", "1.20.1"}) {
            String adapter = readShim(version, ADAPTER);
            assertTrue(adapter.contains("spawnEntity(EntityType"), version + " must expose spawnEntity(EntityType, ServerLevel)");
            assertTrue(adapter.contains("spawnForCommand(EntityType"), version + " must expose spawnForCommand(EntityType, ServerLevel)");
        }
        assertTrue(readShim("26.2", ADAPTER).contains("EntitySpawnReason.REINFORCEMENT"),
                "26.2 must spawn split copies as REINFORCEMENT");
        for (String version : new String[]{"1.21.1", "1.20.1"}) {
            assertTrue(readShim(version, ADAPTER).contains("type.create(level)"),
                    version + " must use plain create(Level)");
        }
    }

    // ========================================
    // 6. Mixins fire on all versions — XP scaling + despawn cleanup
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
    // 7. Command args + TICK/DEATH triggers — same wiring per version
    // ========================================

    @Test
    void commandArgsAndTickDeathTriggers() throws Exception {
        // Adapters expose the same command seam on all versions, Identifier vs ResourceLocation inside
        for (String version : new String[]{"26.2", "1.21.1", "1.20.1"}) {
            String adapter = readShim(version, ADAPTER);
            for (String seam : new String[]{"worldIdArgument(", "worldIdFromCommand(",
                    "defaultEntity()", "entityKey(", "spawnForCommand(", "gamemasterPermission("}) {
                assertTrue(adapter.contains(seam), version + " adapter must expose " + seam);
            }
        }
        assertTrue(readShim("26.2", ADAPTER).contains("IdentifierArgument"), "26.2 adapter must use IdentifierArgument");
        for (String version : new String[]{"1.21.1", "1.20.1"}) {
            assertTrue(readShim(version, ADAPTER).contains("ResourceLocationArgument"),
                    version + " adapter must use ResourceLocationArgument");
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
    // 8. Common stays version-neutral — no API leaks past the seam
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
        // Call/import patterns that must only appear inside per-version adapters
        List<String> forbidden = List.of(
                "IdentifierArgument", "ResourceLocationArgument", "EntitySpawnReason",
                "import net.minecraft.world.effect.MobEffects",
                "new MobEffectInstance(", ".hurtServer(", ".igniteForSeconds(",
                ".setSecondsOnFire(", ".hurtAndBreak(", ".identifier()", ".location()");
        for (String file : commonFiles) {
            String text = readCommon(file);
            for (String pattern : forbidden) {
                assertTrue(!text.contains(pattern),
                        "common/" + file + " must not contain '" + pattern + "' (use the seam)");
            }
        }
    }
}
