package io.github.hunter1712.infusedmobs.platform;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The Fabric manifest may claim only the game version its subproject is built
 * against. A range claim silently promises untested minors.
 */
class ManifestVersionClaimTest {

    private static final Pattern INCLUDE = Pattern.compile("(?m)^\\s*include\\s+['\"]([^'\"]+)['\"]");
    private static final Pattern MINECRAFT_VERSION = Pattern.compile("(?m)^\\s*minecraft_version\\s*=\\s*(\\S+)\\s*$");
    private static final Pattern MANIFEST_CLAIM = Pattern.compile("\"minecraft\"\\s*:\\s*\"([^\"]+)\"");

    @Test
    void everyManifestClaimsItsExactBuiltVersion() throws IOException {
        Path root = repositoryRoot();
        List<String> generations = includedGenerations(root);
        assertFalse(generations.isEmpty(), "no versioned subprojects found in settings.gradle");

        List<String> mismatches = new ArrayList<>();
        for (String generation : generations) {
            Path subproject = root.resolve(generation);
            String built = capture(subproject.resolve("gradle.properties"), MINECRAFT_VERSION, "minecraft_version");
            String claimed = capture(subproject.resolve("src/main/resources/fabric.mod.json"), MANIFEST_CLAIM, "\"minecraft\"");
            if (!built.equals(claimed)) {
                mismatches.add(generation + ": manifest claims '" + claimed + "' but minecraft_version is '" + built + "'");
            }
        }

        assertTrue(mismatches.isEmpty(),
                "Fabric manifests must claim their exact built version:\n" + String.join("\n", mismatches));
    }

    private static Path repositoryRoot() {
        Path directory = Path.of("").toAbsolutePath();
        while (directory != null) {
            if (Files.isRegularFile(directory.resolve("settings.gradle"))
                    || Files.isRegularFile(directory.resolve("settings.gradle.kts"))) {
                return directory;
            }
            directory = directory.getParent();
        }
        assertNotNull(directory, "could not find the repository root (settings.gradle) above the working directory");
        return directory;
    }

    private static List<String> includedGenerations(Path root) throws IOException {
        Matcher matcher = INCLUDE.matcher(Files.readString(root.resolve("settings.gradle")));
        List<String> generations = new ArrayList<>();
        while (matcher.find()) {
            if (!matcher.group(1).equals("common")) {
                generations.add(matcher.group(1));
            }
        }
        return generations;
    }

    private static String capture(Path file, Pattern pattern, String field) throws IOException {
        assertTrue(Files.isRegularFile(file), "missing " + file);
        Matcher matcher = pattern.matcher(Files.readString(file));
        assertTrue(matcher.find(), "no " + field + " in " + file);
        return matcher.group(1);
    }
}
