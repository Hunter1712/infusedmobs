---
number: 13
title: "Verify and release 2.9.2"
state: open
labels: [ready-for-human]
created: "2026-10-30T00:00:00Z"
updated: "2026-10-30T12:00:00Z"
assignee: ""
---

## Parent

Issue #9 — narrow install claims to exact built versions.

## What to build

Prove 2.9.2 is the release it claims to be, then publish it: three jars with exact claims, a green suite and docs lint, a smoke pass on each supported instance, and only the three exact game versions tagged on Modrinth.

## Acceptance criteria

- [x] `gradle :common:test`, `gradle build` and `bash scripts/check-docs.sh` all pass from a clean tree.
- [x] Each of the three jars carries its exact `minecraft` claim; spot-check at least one per generation.
- [x] Smoke test passes on the 1.20.1, 1.21.1 and 26.3 instances: summon an infused mob, reload the world, read a tier nametag, and see no errors in the latest log.
- [ ] Modrinth game-version tags list only 1.20.1, 1.21.1 and 26.3.
- [ ] `v2.9.2` is tagged on the release commit.

## Blocked by

- #12 — rewrite current support claims and ship 2.9.2.

## Comments

**2026-10-30 — agent:** Verification passed. `:common:test`, `build` and `bash scripts/check-docs.sh` are green; built jars are `infusedmobs-2.9.2+1.20.1.jar`, `+1.21.1.jar` and `+26.3.jar`, and each manifest claims exactly `1.20.1`, `1.21.1` or `26.3`. Maintainer smoke pass recorded for all three versions in `docs/smoke-checklist.md`. Remaining release-only steps: Modrinth game-version tags and the `v2.9.2` git tag.
