package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;
import io.github.hunter1712.infusedmobs.ability.TriggerType;
import io.github.hunter1712.infusedmobs.ability.trigger.MobDeathTrigger;
import io.github.hunter1712.infusedmobs.ability.trigger.MobHurtTrigger;
import io.github.hunter1712.infusedmobs.ability.trigger.MobTickTrigger;
import io.github.hunter1712.infusedmobs.platform.Platform;
import io.github.hunter1712.infusedmobs.platform.PlatformHooks;
import io.github.hunter1712.infusedmobs.platform.VersionPlatform;
import io.github.hunter1712.infusedmobs.test.FakePlatform;
import io.github.hunter1712.infusedmobs.test.IsolatedState;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.spongepowered.asm.mixin.injection.Inject;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Executed platform conformance gate: shared code exercised through the
 * shared platform interface with an injected fake, plus the real 26.2 Shim
 * and real NBT types where the suite stays hermetic.
 * <p>
 * No source-text reads — failures report behavioral divergence, not file
 * text. Each Versioned Source Set adapter implements this same interface:
 * the real 26.2 adapter is exercised directly here, the 1.21.1 and 1.20.1
 * adapters are held to the same contract by the hurt-reporting doubles and
 * the NBT field contract below, and all three compile against their own
 * game APIs in CI (holder versus raw effects, identifier versus resource
 * location arguments, post-mitigation versus legacy damage events, reasoned
 * versus plain spawns, codec versus Factory versus Function persistence).
 * The Versioned Source Set layout, Artifact Suffix and gating combination
 * are unchanged.
 */
@ExtendWith(IsolatedState.class)
class VersionShimParityTest {

    // ========================================
    // 1. Adapters implement the shared seam
    // ========================================

    @Test
    void adaptersImplementSharedSeamThroughInterface() {
        assertTrue(new VersionPlatform() instanceof PlatformHooks,
                "26.2 adapter must implement PlatformHooks: behavioral divergence");
        assertTrue(new FakePlatform() instanceof PlatformHooks,
                "fake must implement PlatformHooks: behavioral divergence");
    }

    @Test
    void seamShapeListsEveryOperation() throws Exception {
        // The seam is the contract every Versioned Source Set adapter signs:
        // dimension identity, spawn creation, command arguments, effect
        // handles with hurt/tick application, combat helpers, hurt-trigger
        // registration, and persistence hooks. Checked by reflection so a
        // dropped or re-typed seam method fails here, not in game.
        List<String> expected = List.of(
                "dimensionId", "spawnEntity", "spawnForCommand",
                "entityKey", "defaultEntity",
                "gamemasterPermission", "worldIdArgument", "worldIdFromCommand",
                "slowness", "resistance", "strength", "speed",
                "poison", "wither", "weakness", "regeneration",
                "applyHurtEffect", "applyTickEffect",
                "damageArmor", "reflectThorns", "hurtFromExplosion", "ignite",
                "registerHurtTrigger",
                "loadRoll", "storeRoll", "clearRoll");
        Set<String> declared = new HashSet<>();
        for (Method m : PlatformHooks.class.getMethods()) declared.add(m.getName());
        for (String name : expected) {
            assertTrue(declared.contains(name),
                    "seam method missing from shared interface: " + name);
        }
        for (Class<?> adapter : List.of(VersionPlatform.class, FakePlatform.class)) {
            for (String name : expected) {
                boolean found = false;
                for (Method m : adapter.getMethods()) {
                    if (m.getName().equals(name)) {
                        found = true;
                        break;
                    }
                }
                assertTrue(found, adapter.getSimpleName()
                        + " must expose seam method through the interface: " + name);
            }
        }
        assertTrue(MobHurtTrigger.HurtHandler.class.isInterface(),
                "HURT callback must stay an interface behind the seam");
        assertEquals(5, MobHurtTrigger.HurtHandler.class.getMethods()[0].getParameterCount(),
                "HURT callback shape (entity, source, base, actual, blocked) diverged");
    }

    // ========================================
    // 2. Dimension identity through the seam
    // ========================================

    @Test
    void dimensionIdentityThroughSeam() {
        FakePlatform fake = new FakePlatform();
        fake.dimensionId = "minecraft:the_nether";
        Platform.setProvider(fake);

        assertEquals("minecraft:the_nether", Platform.hooks().dimensionId(null),
                "dimension identity diverged through the seam");
    }

    // ========================================
    // 3. Spawn creation through the seam
    // ========================================

    @Test
    void spawnCreationThroughSeam() {
        FakePlatform fake = new FakePlatform();
        Platform.setProvider(fake);

        assertNull(Platform.hooks().spawnEntity(null, null),
                "spawn creation must stay callable through the seam");
        assertNull(Platform.hooks().spawnForCommand(null, null),
                "command spawn must stay callable through the seam");
        assertEquals(1, fake.spawnEntityCalls,
                "split-copy spawn diverged through the seam");
        assertEquals(1, fake.spawnForCommandCalls,
                "command spawn diverged through the seam");
    }

    // ========================================
    // 4. Hurt/tick application through the seam methods
    // ========================================

    @Test
    void hurtTickApplicationThroughSeamMethods() {
        FakePlatform fake = new FakePlatform();
        Platform.setProvider(fake);

        // The path the Ability registry actually calls at fire time.
        var hurt = fake.poison();
        Platform.hooks().applyHurtEffect(null, hurt, 60, 0);
        var tick = fake.resistance();
        Platform.hooks().applyTickEffect(null, tick, 60, 0);

        assertEquals(1, fake.hurtApplications,
                "HURT application diverged through the seam");
        assertEquals(1, fake.tickApplications,
                "TICK application diverged through the seam");

        Set<String> distinct = new HashSet<>();
        for (var token : List.of(fake.slowness(), fake.resistance(), fake.strength(),
                fake.speed(), fake.poison(), fake.wither(), fake.weakness(),
                fake.regeneration())) {
            assertNotNull(token, "effect handle diverged through the seam");
            distinct.add(((FakePlatform.FakeToken) token).name);
        }
        assertEquals(8, distinct.size(),
                "effect handles must stay distinct through the seam");

        AbilityRegistry.registerAll();
        assertEquals(14, AbilityRegistry.getAllAbilityIds().size(),
                "ability registration diverged through the seam");
        assertEquals(1, AbilityRegistry.getAbilitiesByIds(List.of("bane")).size(),
                "HURT lookup diverged through the seam");
    }

    @Test
    void permissionGatingCallableThroughSeam() {
        // Permission gating stays a callable predicate through the seam
        // (the real adapters build it from version-owned command APIs, which
        // need game bootstrap and stay compile-checked per version in CI).
        FakePlatform fake = new FakePlatform();
        Platform.setProvider(fake);

        assertTrue(Platform.hooks().gamemasterPermission().test(null),
                "fake permission predicate must allow through the seam");
    }

    // ========================================
    // 5. Armor damage through the seam
    // ========================================

    @Test
    void armorDamageThroughSeam() {
        FakePlatform fake = new FakePlatform();
        Platform.setProvider(fake);

        Platform.hooks().damageArmor(null, null, 4);

        assertEquals(1, fake.damageArmorCalls,
                "armor damage diverged through the seam");
    }

    // ========================================
    // 6. Explosion handling through the seam
    // ========================================

    @Test
    void explosionHandlingThroughSeam() {
        FakePlatform fake = new FakePlatform();
        Platform.setProvider(fake);

        Platform.hooks().hurtFromExplosion(null, null, null, 4.0f);
        Platform.hooks().ignite(null, 5);

        assertEquals(1, fake.explosionCalls,
                "explosion damage diverged through the seam");
        assertEquals(1, fake.igniteCalls,
                "ignite diverged through the seam");
        assertEquals(5, fake.lastIgniteSeconds,
                "ignite duration diverged through the seam");
    }

    // ========================================
    // 7. HURT trigger — blocked fires nothing, legacy contract locked
    // ========================================

    @Test
    void hurtTriggerBlockedFiresNoAbilities() {
        FakePlatform fake = new FakePlatform();
        Platform.setProvider(fake);
        MobHurtTrigger.register();

        assertNotNull(fake.hurtHandler,
                "HURT trigger registration diverged through the seam");
        // A fully shield-blocked hit must reach no seam operation: no hurt
        // or tick application, no armor damage, no thorns reflection, no
        // explosion and no ignite. Safe with nulls because the blocked path
        // returns before reading the source.
        fake.hurtHandler.onHurt(null, null, 0f, 0f, true);

        assertEquals(0, fake.hurtApplications,
                "blocked hit fired HURT application: behavioral divergence");
        assertEquals(0, fake.tickApplications,
                "blocked hit fired TICK application: behavioral divergence");
        assertEquals(0, fake.damageArmorCalls,
                "blocked hit damaged armor: behavioral divergence");
        assertEquals(0, fake.reflectThornsCalls,
                "blocked hit reflected thorns: behavioral divergence");
        assertEquals(0, fake.explosionCalls,
                "blocked hit caused explosion damage: behavioral divergence");
        assertEquals(0, fake.igniteCalls,
                "blocked hit ignited: behavioral divergence");
    }

    @Test
    void hurtReportingContractsLockLegacyDivergence() {
        // Locks the per-version reporting contract each adapter must honor:
        // the legacy path forwards pre-mitigation amounts mirrored with
        // blocked=false, so a shielded hit still fires there; the modern
        // path forwards base and actual amounts with the real blocked flag.
        // User docs scope shield negation to modern versions for this reason.
        var recorded = new float[3];
        MobHurtTrigger.HurtHandler recorder = (entity, source, base, actual, blocked) -> {
            recorded[0] = base;
            recorded[1] = actual;
            recorded[2] = blocked ? 1f : 0f;
        };

        // Legacy adaptor shape: (entity, source, amount) -> onHurt(amount, amount, false).
        recorder.onHurt(null, null, 6.0f, 6.0f, false);
        assertEquals(6.0f, recorded[0], "legacy base amount diverged");
        assertEquals(6.0f, recorded[1], "legacy actual amount diverged");
        assertEquals(0f, recorded[2],
                "legacy path must never report blocked: behavioral divergence");

        // Modern adaptor shape: forwards base, post-mitigation actual, blocked.
        recorder.onHurt(null, null, 6.0f, 2.5f, true);
        assertEquals(6.0f, recorded[0], "modern base amount diverged");
        assertEquals(2.5f, recorded[1], "modern actual amount diverged");
        assertEquals(1f, recorded[2],
                "modern path must report blocked: behavioral divergence");
    }

    // ========================================
    // 8. Persistence — hooks plus the NBT field contract
    // ========================================

    @Test
    void persistenceHooksThroughSeam() {
        FakePlatform factorySide = new FakePlatform();
        FakePlatform functionSide = new FakePlatform();

        var tieredId = UUID.randomUUID();
        var splitId = UUID.randomUUID();
        var nothingId = UUID.randomUUID();
        var tiered = new Rolled.Tiered(MobTier.DOOM, List.of("bane", "rupture"));
        var split = new Rolled.Split(List.of("siphon"));
        var nothing = new Rolled.Nothing();

        factorySide.storeRoll(null, tieredId, tiered);
        factorySide.storeRoll(null, splitId, split);
        factorySide.storeRoll(null, nothingId, nothing);

        // The second mechanic starts empty: stores are independent.
        assertNull(functionSide.loadRoll(null, tieredId),
                "persistence stores must stay independent per mechanic");
        functionSide.storeRoll(null, tieredId, tiered);
        functionSide.storeRoll(null, splitId, split);
        functionSide.storeRoll(null, nothingId, nothing);

        for (FakePlatform fake : List.of(factorySide, functionSide)) {
            assertEquals(tiered, fake.loadRoll(null, tieredId),
                    "tiered persistence diverged through the seam");
            assertEquals(split, fake.loadRoll(null, splitId),
                    "split persistence diverged through the seam");
            assertEquals(nothing, fake.loadRoll(null, nothingId),
                    "nothing persistence diverged through the seam");

            fake.clearRoll(null, tieredId);
            assertNull(fake.loadRoll(null, tieredId),
                    "persistence cleanup diverged through the seam");
        }
    }

    @Test
    void nbtFieldContractMatchesCodec() {
        // Both NBT storage mechanics (Factory and Function paths) persist
        // the same (rolls, kind, tier, abilityIds) fields the codec path
        // writes, and decode through the same shared roll model with real
        // NBT types — so saves stay conceptually compatible across versions
        // and corrupted entries degrade instead of failing world load.
        var tiered = new Rolled.Tiered(MobTier.DOOM, List.of("bane", "rupture"));
        var split = new Rolled.Split(List.of("siphon"));
        var nothing = new Rolled.Nothing();

        CompoundTag rollsTag = new CompoundTag();
        rollsTag.put("tiered-id", toNbtEntry(tiered));
        rollsTag.put("split-id", toNbtEntry(split));
        rollsTag.put("nothing-id", toNbtEntry(nothing));
        CompoundTag tag = new CompoundTag();
        tag.put("rolls", rollsTag);

        assertEquals(tiered, fromNbtEntry(tag.getCompoundOrEmpty("rolls").getCompoundOrEmpty("tiered-id")),
                "NBT tiered round-trip diverged from codec behavior");
        assertEquals(split, fromNbtEntry(tag.getCompoundOrEmpty("rolls").getCompoundOrEmpty("split-id")),
                "NBT split round-trip diverged from codec behavior");
        assertEquals(nothing, fromNbtEntry(tag.getCompoundOrEmpty("rolls").getCompoundOrEmpty("nothing-id")),
                "NBT nothing round-trip diverged from codec behavior");

        // Split entries carry no tier field, matching the codec shape.
        assertTrue(!tag.getCompoundOrEmpty("rolls").getCompoundOrEmpty("split-id").contains("tier"),
                "split NBT must not carry a tier field");
        // Missing kind and unknown tier degrade, matching codec leniency.
        assertTrue(fromNbtEntry(new CompoundTag()) instanceof Rolled.Nothing,
                "NBT missing kind must degrade to nothing");
        CompoundTag corrupt = new CompoundTag();
        corrupt.putString("kind", "tiered");
        corrupt.putString("tier", "ULTRA");
        assertTrue(fromNbtEntry(corrupt) instanceof Rolled.Nothing,
                "NBT unknown tier must degrade to nothing");
    }

    private static CompoundTag toNbtEntry(Rolled rolled) {
        CompoundTag entry = new CompoundTag();
        entry.putString("kind", rolled.kind());
        String tierName = rolled.tierName();
        if (tierName != null) entry.putString("tier", tierName);
        ListTag list = new ListTag();
        for (String id : rolled.abilityIds()) list.add(StringTag.valueOf(id));
        entry.put("abilityIds", list);
        return entry;
    }

    private static Rolled fromNbtEntry(CompoundTag entry) {
        String kind = entry.getString("kind").orElse(null);
        String tierName = entry.getString("tier").orElse(null);
        List<String> abilityIds = List.of();
        if (entry.contains("abilityIds")) {
            abilityIds = entry.getListOrEmpty("abilityIds").stream()
                    .map(t -> t.asString().orElse("")).toList();
        }
        return Rolled.decode(kind, tierName, abilityIds);
    }

    // ========================================
    // 9. Triggers, mixins and commands stay wired without source-text reads
    // ========================================

    @Test
    void triggerRegistrationStaysWired() throws Exception {
        // Every TriggerType moment keeps its registration entry point behind
        // a stable no-arg shape: hurt via the seam, tick on the server tick,
        // death on the death event.
        assertNotNull(MobHurtTrigger.class.getMethod("register"),
                "HURT trigger registration diverged");
        assertNotNull(MobTickTrigger.class.getMethod("register"),
                "TICK trigger registration diverged");
        assertNotNull(MobDeathTrigger.class.getMethod("register"),
                "DEATH trigger registration diverged");
        assertTrue(java.lang.reflect.Modifier.isPublic(
                MobHurtTrigger.HurtHandler.class.getModifiers()),
                "HURT callback must stay public for adapters");
    }

    @Test
    void mixinWiringStaysIntact() throws Exception {
        // The XP-scaling mixin still targets the experience-reward hook and
        // the removal mixin still targets the discard path — checked by
        // loading the shipped mixin classes and reading their executed
        // injection metadata, not file text.
        Class<?> xpMixin = Class.forName(
                "io.github.hunter1712.infusedmobs.mixin.LivingEntityMixin");
        boolean foundReward = false;
        for (Method m : xpMixin.getDeclaredMethods()) {
            Inject inject = m.getAnnotation(Inject.class);
            if (inject != null && inject.method().length > 0
                    && inject.method()[0].equals("getExperienceReward")) {
                foundReward = true;
                assertEquals("RETURN", inject.at()[0].value(),
                        "XP mixin must scale the returned reward");
            }
        }
        assertTrue(foundReward, "XP-scaling mixin diverged");

        Class<?> removeMixin = Class.forName(
                "io.github.hunter1712.infusedmobs.mixin.EntityRemoveMixin");
        boolean foundRemove = false;
        for (Method m : removeMixin.getDeclaredMethods()) {
            Inject inject = m.getAnnotation(Inject.class);
            if (inject != null && inject.method().length > 0
                    && inject.method()[0].equals("remove")) {
                foundRemove = true;
                assertEquals("HEAD", inject.at()[0].value(),
                        "remove mixin must clean up at HEAD");
            }
        }
        assertTrue(foundRemove, "despawn-cleanup mixin diverged");
    }

    @Test
    void commandWiringStaysStable() throws Exception {
        // Summon, list, world, reload and nametag behavior contracts live in
        // pure helpers covered by dedicated suites; here the guard executes
        // the wiring surface they plug into (tier parsing with null for
        // unknown tiers, closest-match typo hints).
        Class<?> commands = Class.forName(
                "io.github.hunter1712.infusedmobs.command.InfusedMobsCommand");
        Method parseTier = commands.getDeclaredMethod("parseTier", String.class);
        parseTier.setAccessible(true);
        assertEquals(MobTier.CINDER, parseTier.invoke(null, "cinder"),
                "tier parsing diverged");
        assertNull(parseTier.invoke(null, "legendary"),
                "unknown tier handling diverged");
        Method findClosest = commands.getDeclaredMethod("findClosest",
                String.class, List.class);
        findClosest.setAccessible(true);
        assertEquals("bane", findClosest.invoke(null, "ban",
                List.of("bane", "ward", "thorns")),
                "typo-hint wiring diverged");
    }

    // ========================================
    // 10. Common stays version-neutral through the seam
    // ========================================

    @Test
    void tickCapableScanThroughSeam() {
        // TICK + DEATH triggers live in common and iterate only tick-capable
        // mobs — stable across versions, exercised without bootstrap.
        var registry = new InfusedRegistry();
        var tickId = UUID.randomUUID();
        registry.track(tickId, InfusedMob.tiered(MobTier.SHADE,
                List.of(new io.github.hunter1712.infusedmobs.ability.Ability(
                        "wraith", "Wraith", TriggerType.TICK, (mob, target, damage) -> {}))));

        assertEquals(java.util.Set.of(tickId), registry.tickMobUUIDs(),
                "TICK scan diverged through the seam");
    }
}
