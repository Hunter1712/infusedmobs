---
number: 12
title: "Rewrite current support claims and ship 2.9.2"
state: open
labels: [ready-for-agent]
created: "2026-10-30T00:00:00Z"
updated: "2026-10-30T00:00:00Z"
assignee: ""
---

## Parent

Issue #9 — narrow install claims to exact built versions.

## What to build

Make every current document promise only 1.20.1, 1.21.1 and 26.3, rename the three Gradle folders and jars to those exact versions, write the support policy down, point manual testing at the three instances, and cut 2.9.2 with a warning for players on untested minors.

## Acceptance criteria

- [ ] The Gradle folders `1.20.x`, `1.21.x` and `26.x` are renamed to `1.20.1`, `1.21.1` and `26.3`; `settings.gradle`, project-path references, CI and docs follow.
- [ ] Jar names follow the folder name: `infusedmobs-2.9.2+1.20.1.jar`, `+1.21.1.jar`, `+26.3.jar`.
- [ ] The root README, compatibility guide, installation guide, guide index, project description, version-differences guide, FAQ and modpack-author guide list the exact three versions and no ranges.
- [ ] The shield-block and wrong-jar advice name exact versions without changing their meaning.
- [ ] One policy line in the compatibility guide and the domain context document says: one folder per major generation, a new generation adds a folder, minor versions are never chased.
- [ ] The agent instructions name the 1.20.1, 1.21.1 and 26.3 Prism instances for the per-release smoke test, and use the new build paths.
- [ ] The version is 2.9.2; the changelog has a Changed entry for the narrowed support naming 2.9.1 as the last release for untested minors; the installation guide repeats that warning.
- [ ] Historical changelog entries, ADRs and research notes are unchanged.
- [ ] `bash scripts/check-docs.sh` is green and `gradle build` produces the three 2.9.2 jars with the new names.

## Blocked by

- #11 — claim only the exact built version, with a guard.
