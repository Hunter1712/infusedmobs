---
number: 11
title: "Claim only the exact built version, with a guard"
state: open
labels: [ready-for-agent]
created: "2026-10-30T00:00:00Z"
updated: "2026-10-30T00:00:00Z"
assignee: ""
---

## Parent

Issue #9 — narrow install claims to exact built versions.

## What to build

Each versioned subproject's Fabric manifest claims exactly the game version that subproject is built and smoke-tested against, so 1.20.6, 1.21.11, 26.1 and 26.2 installs refuse to load. Add a test in the shared test source set that fails whenever a manifest's `minecraft` value differs from that subproject's `minecraft_version`, so the claim cannot drift again.

## Acceptance criteria

- [ ] The 1.20.x manifest claims 1.20.1; the 1.21.x manifest claims 1.21.1; the 26.x manifest claims 26.3. No range operators remain.
- [ ] A new unit test in the shared test source set reads each subproject's Gradle properties and manifest, asserts equality, and names the generation and both values on failure. It discovers the versioned subprojects from the Gradle settings file rather than hardcoding folder names, so the 2.9.2 folder rename needs no test edit.
- [ ] The test passes on the real tree and fails when a manifest is deliberately mismatched.
- [ ] `gradle :common:test` is green.
- [ ] `gradle build` succeeds and a spot-check of one built jar's manifest shows the exact claim.

## Blocked by

- #10 — ship the blacklist wildcard feature as 2.9.1.
