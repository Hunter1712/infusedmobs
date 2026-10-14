---
number: 7
title: "Fold 1-1 combat passthroughs out of platform seam"
state: closed
labels: [wontfix]
created: "2026-10-14T09:26:16Z"
updated: "2026-10-14T09:31:26Z"
assignee: ""
---

## Description

Four platform-seam combat methods have exactly one caller each and 1-line adapter bodies per Versioned Source Set: `damageArmor`, `reflectThorns`, `hurtFromExplosion` (`hurtServer`), `ignite` (`igniteForSeconds`). Deletion test fails for these four — inlining moves one line, concentrating nothing. `SpawnKind(REINFORCEMENT/COMMAND)` adds a parameter whose only leverage is the per-adapter switch. The seam earns its keep for genuinely divergent areas (effect tokens, persistence, damage events, `EntitySpawnReason`) but not for these passthroughs. ADR-0001/0002 require the Versioned Source Set + Shim split, so the platform seam itself stays — only the thinnest methods fold.

Source: architecture review candidate 6 (Speculative). Verify each method's three Shim bodies are truly 1-line before folding; folding too eagerly re-scatters version divergence.

Files: `common/src/main/java/io/github/hunter1712/infusedmobs/platform/PlatformHooks.java:32-108`, three `VersionPlatform` adapters (`26.x/1.21.x/1.20.x`), `platform/Platform.java:28-34`, sole callers `AbilityRegistry:126,140,70`, `ability/effect/CombustEffect:38`, `ability/effect/SplitEffect:36`.

## Acceptance Criteria

- [ ] Platform seam keeps only methods with real per-Shim divergence; 1:1 lines live with their sole Ability callers.
- [ ] One adapter per Versioned Source Set preserved (real seam, just narrower).
- [ ] `FakePlatform` conformance counters shrink to methods that actually vary.
- [ ] `gradle :common:test` passes (`PlatformTest` holder semantics intact).

## Comments

### 2026-10-14T09:26:16Z - agent

Created from architecture review candidate 6. Suggested order: last (1 → 2 → 4 → 3 → 5 → 6).

### 2026-10-14T09:31:26Z - agent

Closed as wontfix after verifying the three Shim bodies. The four methods genuinely diverge per Versioned Source Set, so folding them into `common` would re-scatter version divergence instead of concentrating it: `damageArmor` is `hurtAndBreak` 4-arg (level) on 26.x vs 3-arg on 1.20.x; `reflectThorns`/`hurtFromExplosion` are `hurtServer` on 26.x vs `hurt` on 1.20.x; `ignite` is `igniteForSeconds` vs `setSecondsOnFire`; `spawn` is reasoned (`EntitySpawnReason`) vs plain `create`. Common compiles per version against these APIs, so no shared body can hold them. Seam stays as-is (one adapter per Versioned Source Set = real seam, per the "two adapters" principle). No code changed.
