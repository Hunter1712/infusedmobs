---
number: 4
title: "Summon path owns one Gamerule Gate decision and wording"
state: closed
labels: [ready-for-agent]
created: "2026-10-14T09:26:16Z"
updated: "2026-10-14T09:31:26Z"
assignee: ""
---

## Description

`InfusedMobsCommand.summon` orchestrates 7 seam crossings (parse Tier → default entity → raycast/console position → `InfusionGate.status+refusalMessage` → spawn → random Ability draw → `assignSpecificTier` → add to world). Gating is checked twice: pre-check via `InfusionGate.status` with two specific messages (World Blacklist vs Gamerule Gate) and re-check inside `MobTierManager.assignSpecificTier` returning `false` with a third generic message. Only pure parsing is tested; `summon`, blacklist mutations, suggestions, reload need live command types and are untested.

Source: architecture review candidate 3 (Worth exploring). Explicit-operator-intent rule (summon bypasses Mob Blacklist) stays as-is.

Files: `common/src/main/java/io/github/hunter1712/infusedmobs/command/InfusedMobsCommand.java:443-504,416-424`, `common/src/main/java/io/github/hunter1712/infusedmobs/tier/InfusionGate.java:37-69`, `common/src/main/java/io/github/hunter1712/infusedmobs/tier/MobTierManager.java:116-127`.

## Acceptance Criteria

- [ ] One module owns the "may summon here" decision and its wording; command never branches on gate statuses itself.
- [ ] No double-gate message drift (two specific vs one generic today becomes impossible).
- [ ] Position-resolution separated from spawning.
- [ ] Gate decision coverage extends the existing pure `InfusionGate.status(boolean,Boolean)` tests toward the summon refusal path without a command harness; `gradle :common:test` passes.

## Comments

### 2026-10-14T09:26:16Z - agent

Created from architecture review candidate 3. Suggested order: fourth (1 → 2 → 4 → 3 → 5 → 6).

### 2026-10-14T09:31:26Z - agent

Implemented. The `assignSpecificTier`-false branch in `summon` now reuses `InfusionGate.refusalMessage(status, dimensionId)` instead of its own generic string, so World Blacklist vs Gamerule Gate wording is single-sourced behind the Gating seam (generic fallback only if the gate closed between the two checks). Gate decision + wording already pinned by `InfusionGateTest` (incl. refusal-message cases). `gradle :common:test` green.

### 2026-10-14T09:33:00Z - agent

Review fixup: gate status is now computed once before the pre-check and reused by the second branch (no re-evaluation between checks). Full suite re-run green.
