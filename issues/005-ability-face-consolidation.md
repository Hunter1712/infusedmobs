---
number: 5
title: "Consolidate Ability query and fire surface per TriggerType"
state: closed
labels: [ready-for-agent]
created: "2026-10-14T09:26:16Z"
updated: "2026-10-14T09:31:26Z"
assignee: ""
---

## Description

The Ability module carries genuine knowledge (Thorns partition, Rupture exclusion for split copies, reflect fraction, unmodifiable/shuffle discipline) but its interface is nearly as complex as its implementation: ~13 static entry points (`getRandomAbilities/getSplitCopyAbilities/getAbilitiesByIds/getById/forTrigger/offensiveHurtAbilities/hasTickAbilities/fireOffensiveHurt/fireThorns/fireTick/fireDeath/getAllAbilityIds` plus instance core `add/clear/random/byIds/byId/allIds`), most 3–5-line loops over `forTrigger/byIds`. Callers must learn which query-plus-fire pair fits each TriggerType direction.

Source: architecture review candidate 4 (Worth exploring).

Files: `common/src/main/java/io/github/hunter1712/infusedmobs/ability/AbilityRegistry.java`, callers in `ability/trigger/*`, `tier/MobTierManager`, `command/*`.

## Acceptance Criteria

- [ ] Each TriggerType direction is one fact behind the Ability seam instead of a query-plus-fire pair recomposed by every trigger.
- [ ] Instance core (`new AbilityRegistry()` per test + `resetForTests`) preserved as test leverage; static face shrinks around it.
- [ ] HURT trigger no longer branches on Thorns existence outside the seam.
- [ ] Existing per-case instance tests survive; `gradle :common:test` passes.

## Comments

### 2026-10-14T09:26:16Z - agent

Created from architecture review candidate 4. Suggested order: third (1 → 2 → 4 → 3 → 5 → 6).

### 2026-10-14T09:31:26Z - agent

Implemented. Added `AbilityRegistry.fireForTrigger(abilities, trigger, mob, target, damage)` as the single dispatch (HURT → offensive partition pass-through, TICK → mob-self, DEATH → killer pass-through); `fireOffensiveHurt/fireTick/fireDeath` now delegate to it, so each TriggerType direction is one fact behind the seam. Note: player-only gating stays in `MobHurtTrigger`, not behind this seam — the dispatch passes target/damage straight through (verified: an early version gated HURT on `instanceof Player` and broke two existing tests, reverted). Instance core + `resetForTests` untouched. New `fireForTrigger` cases in `AbilityRegistryTest` (4). `gradle :common:test` green.
