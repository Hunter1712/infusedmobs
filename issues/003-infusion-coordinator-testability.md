---
number: 3
title: "Deepen Infusion coordinator so it is testable through its seam"
state: open
labels: [ready-for-agent]
created: "2026-10-14T09:26:16Z"
updated: "2026-10-14T09:31:26Z"
assignee: ""
---

## Description

The Infusion module (`tier/MobTierManager.java`, 353 lines, 12 public statics) has real depth — single-roll decision in `TierRoll.decide`, unified `applyHealthBoost`, `trackTiered/trackSplit/restoreRolled` wiring, `Rolled` persistence round-trip — but zero `MobTierManagerTest` coverage. Every public method takes `Mob/ServerLevel/MinecraftServer`, so assign guards, restore, and `refreshNametags` snapshot loop are untested; only pure leaves (`TierRoll`, `NametagFormatter`, `InfusedRegistry`) are pinned. The module also reaches across three seams directly: `ModConfig.get()` at 5 sites, `Platform.hooks()` at 6 sites, `AbilityRegistry` at 5 sites, plus Mob Blacklist check `isMobBlacklisted`.

Source: architecture review candidate 2 (Strong).

Files: `common/src/main/java/io/github/hunter1712/infusedmobs/tier/MobTierManager.java`, backing `tier/InfusedRegistry.java`, `tier/TierRoll.java:33-38`, `tier/NametagFormatter.java`.

## Acceptance Criteria

- [ ] Coordinator logic (roll-decision, ability-id resolution, nametag text) sits behind existing pure helpers; orchestration reads as wiring.
- [ ] Config and platform handles enter narrowly instead of 11 scattered reach-outs.
- [ ] Infusion seam stays where it is (no new module unless the deletion test demands it).
- [ ] First coordinator-level tests exist (assign/restore/refresh) via fakes; `gradle :common:test` passes.

## Comments

### 2026-10-14T09:26:16Z - agent

Created from architecture review candidate 2. Suggested order: second (1 → 2 → 4 → 3 → 5 → 6).

### 2026-10-14T09:31:26Z - agent

Partial progress, kept open. Extracted pure `MobTierManager.isMobBlacklisted(entityKey, config)` (key lookup + config read stay in the `Mob` overload) with `MobTierManagerTest` (4 cases, no bootstrap). Full coordinator coverage (assign/restore/refresh via fakes) remains blocked: every coordinator path takes `Mob/ServerLevel/MinecraftServer` and the suite has no Minecraft bootstrap or mock harness (JUnit only). Next step when wanted: narrow Config/platform handles to explicit parameters so more decisions become pure-tested like this one. `gradle :common:test` green.
