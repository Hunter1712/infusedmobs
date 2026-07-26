# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.4.0] - 2026-07-26

### Added
- **Persistent tier state** (`TierSavedData.java`) — mobs keep their tier (or rolled-nothing state) across world reloads
- **Despawn cleanup** (`EntityRemoveMixin`) — tier tracking cleaned up when mobs despawn, not just on death
- **Immutable collection safety** — codec deserialization wrapped in `HashMap`/`HashSet` to prevent crash

### Fixed
- **Duplicated nametags** — `mob.getName()` was reading previously-set custom names; now uses `getType().getDescription()` for the base entity name
- **Duplicated announcement** — same fix in `MobHurtTrigger` chat message

### Changed
- **No particles on TICK abilities** — status effects on mobs (Strength, Resistance, etc.) no longer show particles
- **Combust quieted** — silent area damage instead of explosion (no particles, no block damage)
- Version bump to 2.4.0

## [2.3.0] - 2026-07-25

### Added
- **Configuration system** (`ModConfig.java`) — JSON-driven config at `config/infusedmobs.json` with:
  - Per-tier overrides (spawn chance, ability counts, health/XP multipliers)
  - HURT/TICK effect durations and amplifiers
  - Inferno fire duration, Acid armor damage, Combust explosion power
  - Auto-generates defaults on first run
- **Unit test suite** (20 tests) — Pure-logic tests for `DamageContext`, `MobTier`, `ModConfig`, `AbilityRegistry`
- **Fission split copies** now receive full Ember-tier stats:
  - 3× health multiplier
  - 1 random HURT ability (no Fission/Combust to prevent infinite recursion)
  - 60% of boosted max health
  - Greyscale nametag

### Changed
- **Performance optimizations**:
  - `MobTickTrigger` now iterates tracked UUIDs (O(tracked)) instead of scanning all entities (O(entities))
  - `AbilityRegistry` pre-indexes abilities by trigger type (`EnumMap`) for O(1) filtering
  - `getRandomAbilities` pre-sizes result list and copies candidate pool to avoid mutating cache
- **Code cleanup**:
  - Removed deprecated `assignSplitAbility()` method
  - Extracted helper methods in `AbilityRegistry` (`registerHurtEffect`, `registerTickEffect`)
  - Extracted announcement/firing logic in `MobHurtTrigger`
  - Extracted `placeCopy()` helper in `SplitEffect`
  - Consolidated nametag building in `MobTierManager` (`setNametag` shared method)
  - Replaced magic numbers with named constants (`SPLIT_HEALTH_FRACTION`, `COPY_COUNT`, `COPY_OFFSET`, `TICK_INTERVAL`, `ARMOR_SLOTS`)
  - Fixed typo "EMBERE" → "EMBER" in test assertions
  - Inlined single-use UUID local in `MobDeathTrigger`
  - Added `MinecraftServer` import in `MobTickTrigger` (removed fully-qualified reference)

### Fixed
- `MobSpawnMixin` flag order: tier assignment now only runs on server level (no behavioral change, just cleaner)

## [2.2.0] - 2026-07-25

### Added
- **Combust** DEATH ability — explodes on death (configurable power)
- **Strict ability spread** — tiers now have fixed HURT/TICK/DEATH counts (Ember 1/0/0, Surge 1/1/0, Tempest 1/1/1)

### Changed
- Renamed project from "MobAbilities" → "Infused Mobs"
- Elemental theme: tiers now Ember/Surge/Tempest (was Necro/...)
- Rebalanced tier spawn chances and multipliers

## [2.1.0] - 2026-07-24

### Changed
- Elemental theme overhaul
- Cleanups and testing tweaks

## [2.0.0] - 2026-07-23

### Changed
- Complete redesign: tier system, balance, naming

## [1.1.0] - 2026-07-22

### Fixed
- Memory leaks, build errors, performance

## [1.0.0] - 2026-07-21

### Added
- Initial release: MobAbilities for MC 26.2