---
number: 10
title: "Ship the blacklist wildcard feature as 2.9.1"
state: closed
labels: [ready-for-agent]
created: "2026-10-30T00:00:00Z"
updated: "2026-10-30T06:40:00Z"
assignee: ""
---

## Parent

Issue #9 — narrow install claims to exact built versions.

## What to build

Commit the already-implemented blacklist wildcard feature as the 2.9.1 release, with the README's install claims restored to the generation ranges it actually shipped with. This is the clean starting point for the narrowing work: 2.9.1 stays internally consistent, and 2.9.2 begins from a tree with no mixed releases.

## Acceptance criteria

- [ ] The README intro and install table claim the generation ranges again (1.20.1–1.20.6, 1.21.1–1.21.11, 26.1–26.3+), matching the 2.9.1 jars.
- [ ] The wildcard feature, its tests and its docs are committed on `master` as one 2.9.1 commit.
- [ ] Only intended files are staged; line-ending-only churn is not committed.
- [ ] `gradle build` produces the three 2.9.1 jars and `:common:test` is green.

## Blocked by

None (can start immediately).

## Comments

**2026-10-30 — agent:** Closed by commit `14edf8b` (`feat: blacklist wildcards for 2.9.1`). README ranges restored, only the 19 intended files staged (CRLF-only churn discarded), `:common:test` and `bash gradlew build` green, jars `2.9.1+1.20.x`, `2.9.1+1.21.x`, `2.9.1+26.x` produced.
