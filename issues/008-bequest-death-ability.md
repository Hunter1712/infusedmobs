---
number: 8
title: "Bequest DEATH Ability — ally migration for Infused Mobs"
state: open
labels: [ready-for-agent]
created: "2026-10-14T00:00:00Z"
updated: "2026-10-14T00:00:00Z"
assignee: ""
---

## Problem Statement

Pack fights end when the pack dies: killing an Infused Mob removes its threat from the world. There is no horde continuity — no reason to think about kill order, no coven dynamic, and Doom packs with support Abilities feel like isolated elites rather than a group that fights as one.

## Solution

A new DEATH TriggerType Ability, Bequest, that migrates threat on death: when an Infused Mob with Bequest dies, abilities from its own set pass to nearby living Infused Mob allies, one per heir. The support is consumed with the corpse (Bequest itself never migrates, mirroring the Rupture copy-draw exclusion), heirs may overflow their Tier ability count up to the pool cap, and everything persists and re-tags exactly like a normal roll.

## User Stories

1. As a survival player, I want dying infused packs to pass threat to survivors, so that kill order matters.
2. As a survival player, I want a Shade death to empower at most one nearby ally, so that fights escalate fairly.
3. As a survival player, I want a Doom death to empower up to two nearby allies, so that Doom packs feel dangerous but bounded.
4. As a survival player, I want each heir to gain at most one Ability per death, so that no single survivor becomes a super-mob from one kill.
5. As a survival player, I want duplicate abilities to slide to the next ally lacking them, so that migration is not wasted on collisions.
6. As a survival player, I want abilities I already see on an heir to never duplicate within it, so that the no-duplicates draw contract holds after migration.
7. As a survival player, I want heirs to keep their Tier colour and gain the new Ability name in the nametag immediately, so that I can read the new threat.
8. As a survival player, I want migrated abilities to fire normally (HURT on hit, TICK each second, DEATH on death), so that inheritance is real.
9. As a survival player, I want Rupture split copies to never give or receive Bequest migration, so that disposable grey bodies cannot farm threat.
10. As a survival player, I want Cinder mobs to never naturally roll Bequest, so that I never meet a support with nothing to migrate.
11. As a survival player, I want a sole-Bequest death to fizzle silently, so that support without threat does nothing.
12. As a survival player, I want migration to target living allies only, so that corpses from the same Combust blast do not inherit.
13. As a survival player, I want Bequest to fire in rolled DEATH order alongside Rupture and Combust, so that combined deaths behave deterministically.
14. As a survival player, I want migrated XP and health scaling to stay with the heir's Tier, so that inheritance adds abilities without re-multiplying stats.
15. As a server operator, I want World Blacklist, Mob Blacklist, and the Gamerule Gate to apply unchanged, so that gating behaviour is unsurprising.
16. As a server operator, I want the summon command to accept Bequest IDs with tab-completion and fuzzy validation, so that I can stage support fights on demand.
17. As a server operator, I want summon to still bypass the Mob Blacklist as explicit intent, so that event staging works on blacklisted types.
18. As a server operator, I want heir state to persist across chunk reloads and restarts exactly as migrated, so that inherited power is never re-rolled or doubled.
19. As a modpack author, I want the per-Tier ability count cap to grow with the pool (14 to 15), so that Doom can still draw full sets including Bequest.
20. As a modpack author, I want no new config fields or config version bump, so that existing JSON files keep working untouched.
21. As a modpack author, I want identical behaviour on every Versioned Source Set generation with no new Shim, so that pack balance holds across versions.
22. As a guide reader, I want the ability pool count, Bequest rules, Tier overflow note, and Cinder exclusion documented, so that migration is never a surprise.

## Implementation Decisions

- New Ability `bequest` (display name Bequest) in the DEATH TriggerType, drawn from the shared pool; natural Cinder rolls and split-copy draws exclude it via the existing draw-exclusion parameter (same shape as the Rupture copy exclusion); Shade and Doom draws include it; summon accepts it for any Tier as operator intent.
- Migration source is the dead mob's own ability set minus Bequest itself; transfer count is uncapped (a fresh Doom moves up to two, an overflowed host moves all of its transferable set).
- Recipients are the nearest living Tiered allies within a fixed radius (fixed so existing explosion-power tuning stays untouched), paired one-to-one in the dead mob's rolled ability order; each heir gains at most one per death; either-side excess fizzles.
- Greedy slide on collisions: per transferable Ability, the nearest ally that lacks it, is under the pool cap, and is unused this death receives it; otherwise that Ability fizzles and migration continues with the next.
- Heirs keep their Tier and may overflow its ability count up to the pool cap; overflow is intentional and recorded in a new ADR (hard to reverse, surprising without context, real trade-off against fizzle-at-cap).
- Conservation rule for the ADR: a death removes N instances with the corpse and adds at most N minus one, so population-wide ability instances never grow through migration; fizzles dampen.
- Heir updates flow through the existing live/persisted roll seam (live state plus persisted roll restored on chunk reload), with immediate nametag refresh; dead-mob cleanup (death and despawn) is unchanged with live state dying and persisted state restoring per the twin-store lifetimes.
- No new Shim and no behaviour divergence across generations; the one known shield carve-out is untouched.
- Gating unchanged: per-dimension activity still conjoins the World Blacklist with the gamerule, the Mob Blacklist still covers natural rolls only, summon still refuses in inactive dimensions with distinct messages.

## Testing Decisions

- Good tests assert external behaviour through the highest seam (roll state before/after a DEATH fire: heir sets, nametags, persisted snapshot restored on reload), not implementation details of the pairing loop.
- Modules under test: the shared ability pool (draw exclusion for Cinder and split copies, summon validation set), the DEATH fire path (rolled-order firing with Rupture and Combust present), and the roll store (heir overflow persists exactly, dead-mob cleanup clears).
- Prior art: the single-roll distribution test, the Combust radius/damage falloff tests, the split-copy Cinder-stat and no-resplit tests, the summon fuzzy-validation tests, and the roll-store conformance test pinning live/persisted drift.
- Cases: migrate one (Shade) and two (Doom); sole-Bequest fizzle; Bequest-itself never migrates; duplicate slides to next ally; pool-cap fizzle; copy exclusion both directions; Cinder natural exclusion with summon allowed; alive-only scan with Combust-killed nearest skipped; heir nametag plus reload persistence with no stat re-multiplication; conservation (added never exceeds removed minus one).

## Out of Scope

- New TriggerTypes, per-Ability blacklists or toggles, new config fields or gamerule changes, new Shims or version-divergent behaviour, custom particles/sounds/entities/AI goals, ally healing or targeting abilities, changes to Rupture/Combust/Thorns/shield semantics, and rebalancing existing spawn chances or multipliers.

## Further Notes

- Seam proposal (single seam, no new ones): exercise Bequest entirely through the existing Infusion seam — live query plus persisted roll snapshot driven by DEATH fire — with draw exclusion via the existing excluded-ids draw parameter. This matches maintainer expectations from the roll-store seam ADR and keeps the change to one test surface.
- Guide updates ride with the change: ability pool total, Bequest section (migrate-one-nearest-infused-alive, duplicate slide, pool cap, copy exclusion, gating exception), and Tier overflow plus Cinder-exclusion notes.
