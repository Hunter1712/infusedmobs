---
number: 6
title: "Narrow Config reads and isolate file IO seam"
state: closed
labels: [ready-for-agent]
created: "2026-10-14T09:26:16Z"
updated: "2026-10-14T09:31:26Z"
assignee: ""
---

## Description

The Config module has real depth (malformed-JSON fallback, per-field clamp warnings, blacklist normalisation, upgrade backfill) and well-pinned pure parts (`clamped/normaliseBlacklist/isListed`). Friction is at the seam: init order is implicit (`load(abilityPoolSize)` so the module never pulls the Ability pool itself — keep this), `load/save/swapInstance` touch `FabricLoader` + disk with no hermetic IO seam, fire-time `ModConfig.get()` inside Ability effect lambdas re-reads Config on every HURT/TICK fire, and callers needing only `forTier/is*Blacklisted` face a 14-field record plus lifecycle methods.

Source: architecture review candidate 5 (Worth exploring).

Files: `common/src/main/java/io/github/hunter1712/infusedmobs/config/ModConfig.java` (lifecycle `load/get/swapInstance/save`, `Instance:130-145`), readers in `ability/AbilityRegistry.java:101-139`, `tier/MobTierManager.java:82-219`, `ability/effect/CombustEffect.java:31`, init `InfusedMobsMod.java:35-36`.

## Acceptance Criteria

- [ ] Fire-time code receives resolved numbers instead of re-reading the Config module per hit.
- [ ] File IO isolated behind the pure `clamped/backfill` core so corrupt-file and upgrade paths are exercisable without disk.
- [ ] `load(abilityPoolSize)` stays parameterised (caller owns pool-then-config order).
- [ ] `gradle :common:test` passes; hermetic IO coverage added for corrupt-file/upgrade paths.

## Comments

### 2026-10-14T09:26:16Z - agent

Created from architecture review candidate 5. Suggested order: fifth (1 → 2 → 4 → 3 → 5 → 6).

### 2026-10-14T09:31:26Z - agent

Implemented (IO half). Extracted `ModConfig.parseOrNull(json)` (raw Gson parse, null on malformed/`null` literal) + `toJson` test helper; `load()` delegates so the corrupt-file decision is exercised disk-free in `ModConfigParseTest` (4 cases: round-trip, malformed, null input, null literal). Note: fire-time `ModConfig.get()` inside Ability effect lambdas deliberately kept — it is the reload-without-restart contract documented in `InfusedMobsMod` — and `load(abilityPoolSize)` stays parameterised. `gradle :common:test` green.
