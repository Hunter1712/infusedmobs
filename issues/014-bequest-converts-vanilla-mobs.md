---
number: 14
title: "Bequest converts vanilla mobs into Cinder"
state: open
labels: [ready-for-agent]
created: "2026-11-01T14:12:02Z"
updated: "2026-11-01T14:12:02Z"
assignee: ""
---

## Problem Statement

Bequest is a DEATH ability that currently migrates the dead mob's abilities onto
nearby allies that already have a Tier. The migration ignores each tier's
ability-count cap, so a Cinder (abilityCount 1) can accumulate many abilities
across repeated nearby deaths — breaking the single tuning lever pack-makers
edit in `config/infusedmobs.json`. Bequest also only ever targets already-tiered
mobs, so it never creates new Infused Mobs despite its name implying a legacy
passed on.

## Solution

Rework Bequest so a death converts nearby **vanilla** mobs (never tiered, not a
Rupture split copy) into **Cinder**-tiered mobs carrying the dead mob's first
transferable (non-Bequest) ability. Each convert gets exactly one ability,
honouring Cinder's ability-count cap of 1. Conversion is gated by the same
InfusionGate (world blacklist + `infusedmobs:enabled` gamerule) as natural
spawn and summon, respects the mob blacklist and MONSTER-only restriction, and
each convert is persisted via TierSavedData with a health boost, full heal, and
Cinder nametag so it survives reloads.

## User Stories

1. As a player, I want a nearby vanilla hostile mob to become a Cinder when a
   Bequest mob dies nearby, so that bequest spreads infusion instead of only
   strengthening already-infused allies.
2. As a player, I want each converted mob to receive exactly one ability drawn
   from the dead mob's abilities, so that Cinder's one-ability cap is not
   bypassed by bequest.
3. As a player, I want the dead mob's Bequest ability itself to never be passed
   on, so that bequest cannot chain endlessly from convert to convert.
4. As a player, I want a converted mob to keep its tier, ability, health boost
   and nametag after a world reload, so that bequest conversions persist like
   any other roll.
5. As a player, I want bequest to do nothing in a dimension where the mod is
   disabled by the world blacklist or the `infusedmobs:enabled` gamerule, so
   world gating is consistent across spawn, summon and bequest.
6. As a player, I want bequest to never convert a mob type on the mob blacklist,
   so a pack-maker's exclusion is honoured by bequest as it is by natural
   infusion.
7. As a player, I want bequest to only target hostile mobs (MONSTER category),
   so passive or friendly mobs are never converted.
8. As a player, I want bequest to fizzle (do nothing) when the dead mob holds
   no transferable ability, so a bequest-only death is harmless.
9. As a pack-maker, I want Cinder's configured abilityCount to remain the single
   source of truth for how many abilities a converted mob can hold, so my config
   balance is not undermined by bequest.
10. As a mod author, I want bequest to reuse the existing assign-tier persistence
    seam rather than duplicating store/nametag logic, so conversion behaves
    identically to summon and natural spawn on reload.
11. As a server operator, I want converted mobs to be boosted to their tier's
    health and healed to full, so a converted mob is immediately durable like a
    naturally rolled tiered mob.
12. As a player, I want the nearest eligible vanilla mobs to convert first
    (distance order), so bequest feels local and predictable around the death.

## Implementation Decisions

- `BequestEffect.apply(Mob)` is the thin Minecraft-touching entry point: server
  level check, InfusionGate status gate, dead-id extraction, 8-block radius
  scan, per-ally eligibility checks, then `MobTierManager.assignSpecificTier`
  (existing persistence seam) for storeRoll, health boost, heal, and nametag.
- One new pure seam: `BequestEffect.planConversions(List<String> deadIds,
  List<MobCandidate> allies, ModConfig.Instance config, boolean gateActive)`
  returns `List<Conversion>`. Plain data — no Minecraft classes, no bootstrap,
  unit-tested directly. `MobCandidate` is a record of entityKey, category,
  tiered, splitCopy, distance. `Conversion` is allyIndex + abilityId.
- Conversion selection: dead mob's first non-Bequest ability id in rolled order
  (distinct). Each eligible ally (not tiered, not split copy, MONSTER category,
  not mob-blacklisted, nearest first) receives exactly that one ability.
- Bequest id (`AbilityRegistry.BEQUEST_ID`) is excluded from migration, matching
  the natural-draw exclusion for Cinder and split copies.
- `MobTierManager.planBequest`, `grantAbility`, `canAccept`, and the `Grant`
  record are deleted — they were only used by `BequestEffect` and
  `BequestPlanTest`, both removed.
- `BequestPlanTest` is deleted; replaced by unit tests on `planConversions`
  pinning: gate-inactive empty, no-transferable-id empty, Bequest-id exclusion,
  first-transferable-id selection, radius/distance ordering, tiered/split/
  non-monster/blacklist exclusions, exactly one ability per convert.
- Existing `MobTierManager.assignSpecificTier` supplies persistence (TierSavedData),
  health multiplier, full heal, and Cinder nametag — no duplicated logic.
- No changes to `MobTier`, `MobTierManager.decideTier`, `TierRollStore`, or
  `TierSavedData`; no new config fields or gamerules.

## Testing Decisions

- Good tests pin external behaviour through `planConversions` using plain-data
  `MobCandidate` inputs — no FabricLoader, no Minecraft bootstrap, matching the
  style of `MobHurtTriggerTest` (pure boolean seam) and `MobTickTriggerTest`
  (fake holders, canned lists).
- Module under test: `BequestEffect` (new `planConversions` plus existing `apply`
  contract). Prior art: `BequestPlanTest` (deleted) tested the old pairing rule
  directly; the new tests replace it at the same seam.
- Tests assert: gate-inactive → no conversions; sole-Bequest death → empty;
  dead ids [bequest, bane] → abilityId "bane"; tiered ally skipped; split copy
  skipped; non-MONSTER skipped; blacklisted entityKey skipped; nearest-first
  ordering preserved; each convert gets exactly one ability (first non-Bequest id).
- `assignSpecificTier` persistence/health/nametag behaviour is already covered
  by `MobTierManagerTest`, `RestartRestoreTest`, and `InfusionStatsTest` — no
  duplication needed here.

## Out of Scope

- Changing Cinder's configured abilityCount or spawn chance (balance remains in
  `config/infusedmobs.json` and `MobTier`).
- Chaining bequest from a converted mob's death if it carries Bequest — Bequest
  is excluded from migration so this cannot occur.
- Converting split copies or already-tiered mobs — explicitly excluded.
- A cap on the number of mobs converted per death (all eligible in radius
  convert); flagged as a scale consideration only.
- New commands, config fields, or gamerules.

## Further Notes

- Finding #3 from the repo audit (Bequest ignores the per-tier ability limit) is
  resolved by this rework: each convert receives exactly one ability, Cinder's
  cap.
- The `MobCandidate` record deliberately uses plain strings (`entityKey`,
  `category`) rather than Minecraft types so `planConversions` stays hermetic.
- `InfusionGate.refusalMessage` is not reused here — bequest silently does
 nothing when inactive, matching the "no feedback for automatic death effects"
 convention.
