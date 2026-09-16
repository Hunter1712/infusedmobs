package io.github.hunter1712.infusedmobs.tier;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Verifies that the NBT shims (1.20.1, 1.21.1) use identical field names to the modern
 * codec path (26.2) ({@code rolls}, {@code kind}, {@code tier}, {@code abilityIds}) so saves
 * are conceptually compatible across versions, even though the serialization adapter differs
 * (codec vs CompoundTag).
 *
 * This is a pure-logic file-content test (no Minecraft bootstrap) that guards against
 * divergence of field names between the per-version shims, as required by
 * ADR 0002 and ticket #8.
 */
class TierSavedDataNbtTest {

    private static String readTierSavedData(String version) throws Exception {
        // Try multiple base dirs: gradle may run with project root or subproject as working dir
        String[] candidates = {
                version + "/src/main/java/io/github/hunter1712/infusedmobs/tier/TierSavedData.java",
                "../" + version + "/src/main/java/io/github/hunter1712/infusedmobs/tier/TierSavedData.java",
                "../../" + version + "/src/main/java/io/github/hunter1712/infusedmobs/tier/TierSavedData.java"
        };
        for (String cand : candidates) {
            Path p = Path.of(cand);
            if (Files.exists(p)) return Files.readString(p);
            Path abs = Path.of(System.getProperty("user.dir")).resolve(cand);
            if (Files.exists(abs)) return Files.readString(abs);
        }
        // Fallback: search from user.dir upwards
        Path root = Path.of(System.getProperty("user.dir"));
        for (int i = 0; i < 5; i++) {
            Path p = root.resolve(version + "/src/main/java/io/github/hunter1712/infusedmobs/tier/TierSavedData.java");
            if (Files.exists(p)) return Files.readString(p);
            root = root.getParent();
            if (root == null) break;
        }
        throw new java.io.FileNotFoundException("Cannot find " + version + "/TierSavedData.java from " + System.getProperty("user.dir"));
    }

    @Test
    void nbtShimUsesSameFieldNamesAsCodec() throws Exception {
        // Check codec shim (26.2) uses correct field names
        String modernText = readTierSavedData("26.2");
        assertTrue(modernText.contains("\"rolls\""), "modern codec must use field 'rolls'");
        assertTrue(modernText.contains("\"kind\""), "modern codec must use field 'kind'");
        assertTrue(modernText.contains("\"abilityIds\""), "modern codec must use field 'abilityIds'");
        assertTrue(modernText.contains("\"tier\""), "modern codec must use field 'tier'");

        // Check legacy NBT shims (1.20.1, 1.21.1) use same field names
        for (String version : new String[]{"1.20.1", "1.21.1"}) {
            String shimText = readTierSavedData(version);
            assertTrue(shimText.contains("putString(\"kind\""), version + " NBT shim must use field 'kind'");
            assertTrue(shimText.contains("putString(\"tier\""), version + " NBT shim must use field 'tier'");
            assertTrue(shimText.contains("\"abilityIds\""), version + " NBT shim must use field 'abilityIds'");
            assertTrue(shimText.contains("\"rolls\""), version + " NBT shim must use field 'rolls'");
            assertTrue(shimText.contains("getString(\"kind\""), version + " NBT shim must read field 'kind'");
            assertTrue(shimText.contains("getString(\"tier\""), version + " NBT shim must read field 'tier'");
        }
    }

    @Test
    void splitCopyDoesNotPersistTierField() throws Exception {
        for (String version : new String[]{"1.20.1", "1.21.1"}) {
            String shimText = readTierSavedData(version);
            // Split case must not write tier field (prevent recursion)
            // The save method's split branch should not contain putString("tier"
            int splitIndex = shimText.indexOf("instanceof Rolled.Split");
            int tierAfterSplit = shimText.indexOf("putString(\"tier\"", splitIndex);
            int nothingIndex = shimText.indexOf("putString(\"kind\", \"nothing\"", splitIndex);
            // tier should not appear between split and nothing (i.e., split branch has no tier)
            boolean splitHasTier = tierAfterSplit != -1 && tierAfterSplit < nothingIndex && tierAfterSplit > splitIndex;
            assertTrue(!splitHasTier, version + " split NBT must not persist tier field");
        }
    }

    @Test
    void nbtShimsDegradeCorruptedSavesToNothing() throws Exception {
        // #8: corrupted tier / unknown kind must fall back to Nothing, matching codec DTO.toRolled
        for (String version : new String[]{"1.20.1", "1.21.1"}) {
            String text = readTierSavedData(version);
            assertTrue(text.contains("new Rolled.Nothing()"),
                    version + " NBT shim must degrade corrupted saves to Nothing");
            // Unknown tier name caught, not propagated
            assertTrue(text.contains("catch (IllegalArgumentException"),
                    version + " NBT shim must catch unknown tier names");
            // Missing rolls returns empty store (fresh world), not crash
            assertTrue(text.contains("if (!tag.contains(\"rolls\""),
                    version + " NBT shim must handle missing rolls gracefully");
        }
    }
}
