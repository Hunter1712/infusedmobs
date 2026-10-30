---
number: 9
title: "Narrow install claims to exact built versions (1.20.1 / 1.21.1 / 26.3)"
state: open
labels: [ready-for-agent]
created: "2026-10-30T00:00:00Z"
updated: "2026-10-30T00:00:00Z"
assignee: ""
---

## Problem Statement

The mod builds one jar per Minecraft generation (1.20.x, 1.21.x, 26.x), but every jar claims its whole generation in its Fabric manifest — `>=1.20.1 <1.21`, `>=1.21.1 <1.22`, `>=26.1 <27` — and the guides repeat those ranges. Nothing is tested outside the single baseline each subproject compiles against (1.20.1, 1.21.1, 26.3), so a player on 1.20.6, 1.21.11, 26.1 or 26.2 is told the mod supports them and then debugs behaviour nobody verified. The claim is also invisible to CI: it drifted from the build version with no failure.

## Solution

Narrow every current install claim to the exact version that is actually built and smoke-tested: **1.20.1, 1.21.1, 26.3**. A jar refuses to load on any other game version. The support promise becomes three major generations, one exact version each; a future major generation adds a fourth folder, while minor versions are never chased. A unit test in the shared test source set fails whenever a manifest's `minecraft` claim differs from that subproject's `minecraft_version`, so the claim cannot drift again.

## User Stories

1. As a player on 1.20.1, I want the mod to install with an exact version claim, so that I know my game version is supported rather than guessed at.
2. As a player on 1.21.1, I want the same exact claim, so that the install is unambiguous.
3. As a player on 26.3, I want the same exact claim, so that the newest generation is equally clear.
4. As a player on 1.20.6, 1.21.11, 26.1 or 26.2, I want the mod to refuse to load instead of half-working, so that I do not chase bugs from an untested combination.
5. As a player, I want the download table to list my exact game version, so that I pick the right jar on the first try.
6. As a player, I want the FAQ's wrong-jar advice to name exact versions, so that the fix is obvious.
7. As a player, I want the shield-block difference explained per exact version, so that I know what to expect on mine.
8. As a player, I want the same abilities and balance on every supported version, so that a guide written for one applies to all three.
9. As a player, I want the version-differences guide to stop implying 26.1–26.3 support, so that its header matches the jar.

10. As a server operator, I want the compatibility table to list the exact supported game versions, so that I can decide before installing.
11. As a server operator, I want a jar that will not load on an untested minor, so that I do not run unsupported code on a live server.
12. As a server operator upgrading from 2.9.1 while on an untested minor, I want a clear warning to stay on 2.9.1, so that a working server is not broken by an update.
13. As a server operator, I want the Java and Fabric API requirements to remain per generation, so that the narrowing does not change my runtime setup.

14. As a modpack author, I want exact game-version pins, so that a pack does not silently update onto an unsupported version.
15. As a modpack author, I want the tested-versions line in the modpack-author guide to list 1.20.1 / 1.21.1 / 26.3, so that pack metadata is honest.
16. As a modpack author, I want Modrinth game-version tags to shrink to those three, so that filtering matches reality.
17. As a modpack author, I want the folder and jar suffix to name the exact game version (`+1.20.1` / `+1.21.1` / `+26.3`), so that the filename itself cannot imply an untested minor is supported.

18. As a maintainer, I want CI to fail when a manifest's `minecraft` claim differs from that subproject's `minecraft_version`, so that the claim cannot rot again.
19. As a maintainer, I want the check to live in the shared test source set, so that `:common:test` catches it without a new build task or a second linter.
20. As a maintainer, I want the three versioned subprojects to remain the only build targets, so that the matrix stays exactly three.
21. As a maintainer, I want the rule "one folder per major generation; a new generation adds a folder; minor versions are never chased" written down, so that the next generation is a decision already made.
22. As a maintainer, I want historical changelog entries, ADRs and research notes left untouched, so that the record of what shipped when stays true.
23. As a maintainer, I want 2.9.1 (the wildcard feature) committed before this change, so that each release is internally consistent.
24. As a maintainer, I want the narrowing to ship as 2.9.2 with a changelog entry, so that users can tell the two releases apart.

25. As a guide reader, I want one policy line stating three major generations and no minor chasing, so that the support promise is explicit.
26. As an agent doing manual verification, I want the deploy instructions to name the 1.20.1, 1.21.1 and 26.3 Prism instances, so that smoke tests cover the whole matrix.

## Implementation Decisions

- The three versioned subprojects' Fabric manifests declare the exact `minecraft_version` from their own Gradle properties — one exact string per manifest, no range operators.
- No Java, mixin, shim, config-schema or gameplay code changes. The only behaviour change is Fabric refusing to load the jar on an unclaimed game version.
- Current support claims are rewritten in: the root README, the compatibility guide, the installation guide, the guide index, the project description, the version-differences guide, the FAQ (wrong-jar and shield-block wording), and the modpack-author guide's tested-versions line.
- A policy line is added to the compatibility guide and the domain context document: one folder per major generation, a new generation adds a folder, minor versions are not chased; each jar claims its exact build version.
- Historical material is not rewritten: old changelog entries, ADRs and research notes keep the ranges that were true when written.
- Subproject folders and Gradle project names are renamed to the exact versions: `1.20.x` → `1.20.1`, `1.21.x` → `1.21.1`, `26.x` → `26.3`. Because `version = mod_version + project.name`, the jars follow automatically (`+1.20.1`, `+1.21.1`, `+26.3`); no version override is added. Current build, CI, test and doc references to the old paths are updated.
- The agent instructions name the three Prism instances used for the per-release smoke test; instance names only.
- New guard: a JUnit test in the shared test source set. For each of the three generations it reads `minecraft_version` from the subproject's Gradle properties and the `minecraft` value from its Fabric manifest, and asserts equality. The test discovers the repository root by walking up from the working directory until it finds the Gradle settings file, and parses the two values by simple text extraction rather than adding a JSON dependency. No docs-linter rule and no new Gradle task.
- Release split: the uncommitted wildcard feature ships as 2.9.1 first, with the README's range claims restored so that release stays internally consistent. This spec then bumps to 2.9.2 and lands every narrowing change together.
- The 2.9.2 changelog gains a Changed entry explaining the narrowed support and naming 2.9.1 as the last release for untested minors. The installation guide gains the same warning.
- Modrinth and CurseForge game-version tags are edited by hand at release time; the repository cannot enforce them.

## Testing Decisions

- A good test asserts the external contract — what a user can install on — not how the manifest or properties file is formatted internally. Reading the two published values and comparing them is the whole assertion.
- Module under test: the shared `common` test source set. It already compiles without a Minecraft bootstrap and runs as part of `:common:test`.
- The new test reads the three sibling subprojects' sources. Since resource processing only expands the mod version and leaves the `minecraft` claim untouched, source equality implies the built jar carries the same exact claim.
- Cases: all three generations match; a deliberately mismatched manifest fails with a message naming the generation and both values.
- Prior art: the pure-logic unit tests already in the shared test source set, which construct no game state.
- Verification: `gradle :common:test` (guard and existing suite), `gradle build` (all three jars), and one spot check of a built jar's manifest with an unzip/read.
- The existing behavioural parity test is untouched, because the feature changes no behaviour.

## Out of Scope

- Building or smoke-testing 1.21.11, 26.2 or any other minor version.
- Proving that a jar happens to work on a neighbour minor; the claim is deliberately narrower than "might work".
- Dropping a generation or adding a fourth subproject.
- Editing Modrinth/CurseForge pages from the repository.
- The blacklist wildcard feature and its 2.9.1 release content.
- Rewriting historical changelog entries, ADRs or research notes.
- Changing Java-version claims or Fabric API version claims.
- Renaming the 1.21.x smoke checklist, which names a generation, not a support claim.

## Further Notes

- 2.9.1 is committed as `14edf8b` with the wildcard feature, tests and docs; the README ranges were restored before that commit so the two releases do not overlap.
- The domain context document currently says each jar loads on its whole generation via the manifest predicate; that sentence is the one that must change to "exact build version".
- A follow-up decision on 2026-10-30 brought the folder rename into 2.9.2. 2.9.1 keeps the old `1.20.x` / `1.21.x` / `26.x` folders and jar names; the rename lands with the narrowing so the two releases stay separable.
- The version-differences guide legitimately uses generation names as column headers; only the `26.1–26.3+` range in the header is a claim and must go.
- Risk accepted: players on untested minors lose the upgrade path. The changelog and installation note are the only warning they get; the manifest refusal is the enforcement.
- The guard deliberately checks sources, not built jars, to stay fast and dependency-free; a jar-level check would duplicate the same two files.
