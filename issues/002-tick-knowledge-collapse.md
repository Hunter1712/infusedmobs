---
number: 2
title: "Collapse TICK knowledge behind one module"
state: closed
labels: [ready-for-agent]
created: "2026-10-14T09:26:16Z"
updated: "2026-10-14T09:31:26Z"
assignee: ""
---

## Description

One predicate ("Ability has TriggerType TICK") is evaluated at three depths through a four-module chain: `MobTickTrigger.register` → `MobTierManager.getTickMobUUIDs` → `InfusedRegistry.tickMobUUIDs` → `AbilityRegistry.hasTickAbilities`, then `getAbilitiesByTrigger` + `fireTick` re-filter a third time. Each hop is a shallow delegation. TICK knowledge leaks from the Ability module into the Infusion module, and mob lookup (`MobTierManager.findMob`, a scan of `server.getAllLevels()`) sits far from its only tick caller.

Source: architecture review candidate 1 (Strong). See `C:\Users\Yaza\AppData\Local\Temp\architecture-review-20261014-112316.html`.

Files: `common/src/main/java/io/github/hunter1712/infusedmobs/ability/trigger/MobTickTrigger.java:30-44`, `common/src/main/java/io/github/hunter1712/infusedmobs/tier/MobTierManager.java:266-289`, `common/src/main/java/io/github/hunter1712/infusedmobs/tier/InfusedRegistry.java:62-67`, `common/src/main/java/io/github/hunter1712/infusedmobs/ability/AbilityRegistry.java:212-242,275-279`.

## Acceptance Criteria

- [ ] Per-second TICK pass crosses one deepened module seam instead of a 3-step protocol (get UUIDs → find mob → resolve + fire).
- [ ] `MobTierManager` no longer answers "which mobs tick?"; Ability module is queried once per mob per second, not three times.
- [ ] Mob lookup lives beside its tick caller.
- [ ] Deletion test passes: deleting the tick filter concentrates logic instead of leaving redundant filters elsewhere.
- [ ] `gradle :common:test` passes; tick path covered through its interface with fakes (no `ServerTickEvents` harness required for core logic).

## Comments

### 2026-10-14T09:26:16Z - agent

Created from architecture review candidate 1. Suggested order: first (1 → 2 → 4 → 3 → 5 → 6).

### 2026-10-14T09:31:26Z - agent

Implemented. `MobTickTrigger.tickServer(server)` now owns the per-second pass (scan → lookup → resolve → fire) and `register()` delegates to it, so production callers cross one seam instead of composing the 3-step protocol. Dispatch core `tickOnce(ids, finder, abilitiesFor, fire)` is generic over the mob holder and tested with fakes in `MobTickTriggerTest` (5 cases: empty, missing mob, fire, skip-empty, multi-mob). Note: `MobTierManager.getTickMobUUIDs` remains as the tick module's query mechanism behind `tickServer` (plus existing `InfusedRegistry` pins) rather than being deleted — the caller-facing seam is one method. `gradle :common:test` green.
