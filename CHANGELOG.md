# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.7.1] - 2026-08-02

### Added
- **Gamerule** — `infusedmobs:enabled` (default `true`) fully disables the mod in a world (no tiers, abilities, or nametags; `/infusedmobs summon` refused). Persists in the world save, survives restarts, works in world-creation gamerule screens, and can be set at launch by modpack makers (datapacks / gamerule-modifying mods). Combined with the config settings: the mod is active unless blacklisted **or** `enabled` is off
- **Gamerule registration** — `ModGameRules` registers the rule into `BuiltInRegistries.GAME_RULE` during mod init (26.2 registry-based gamerule system) with null-safe reads that fall back to the rule default when a world save has no stored value

### Removed
- **Announcements scrapped entirely** — the first-encounter chat message ("⚡ DOOM Zombie has: ..."), the `showAnnouncements` config field, the `/infusedmobs announce` command, and the `infusedmobs:announcements` gamerule are all gone. Config version bumped to 3; old configs upgrade automatically (the stale field is ignored)

### Changed
- **`/infusedmobs summon` respects gamerules** — refuses with a clear message when `infusedmobs:enabled` is off (distinct from the blacklist message)
- **`/infusedmobs help`** — documents the gamerule
- **Rupture split copies draw any ability except Rupture** — a copy's random ability can now be anything (including Combust); only Rupture itself is excluded, since it would split forever. Previously all DEATH abilities were filtered out
- **Internal refactor** — sealed persistence model (`Tiered` / `Split` / `Nothing`) in the world save, id-based ability-draw exclusions, single hurt handler with a thorns-damage guard (no reentrancy state), extracted testable command helpers

### Fixed
- **`/infusedmobs summon` persistence** — summoned mobs now save their tier + abilities to the world save; on chunk reload or world restart the exact summon is restored instead of being silently re-rolled
- **Split copies keep their Cinder HP across chunk reloads** — restored copies re-apply the 1.5× health multiplier and 60% spawn health
- **Corrupted saved tier data degrades safely** — an unknown tier value in `infusedmobs_tiers.dat` no longer fails world load; the mob falls back to vanilla (codec reports errors instead of throwing)
- **Config write failures are logged** — silent disk errors when saving `infusedmobs.json` now produce a warning instead of failing invisibly

## [2.7.0] - 2026-08-01

### Added
- **World blacklist** — `worldBlacklist` config field (list of dimension IDs, e.g. `"minecraft:overworld"`). In blacklisted worlds mobs spawn as vanilla — no tiers, no abilities, no nametags. Managed at runtime via `/infusedmobs world add|remove <world>` and `/infusedmobs world list` (with tab-completion of loaded dimension IDs)
- **Announcement toggle** — `showAnnouncements` config field (default `true`) disables the first-encounter chat announcement ("⚡ DOOM Zombie has: ...") while abilities still fire. Toggled via `/infusedmobs announce [on|off]`
- **Config backfill on upgrade** — new fields are backfilled from defaults when loading a pre-2.7.0 config, preserving existing tier/effect settings (prevents announcements silently disabling after upgrade)

### Changed
- **`/infusedmobs summon` respects the blacklist** — refuses to summon in blacklisted worlds with a clear failure message
- **`assignSpecificTier()` returns boolean** — `false` (no modification) when the level is blacklisted, guarding against future callers

### Fixed
- **Gson null blacklist** — a config file missing `worldBlacklist` is accepted and backfilled to empty instead of being rejected
- **`/infusedmobs world add|remove` parse error** — dimension IDs like `minecraft:overworld` failed with "Expected whitespace to end one argument" because the `string()` argument type rejects `:`. Now uses `IdentifierArgument`, so unquoted resource IDs parse correctly (and `overworld` shorthand defaults to `minecraft:overworld`); tab-completion highlighting works too

## [2.6.0] - 2026-07-29

### Added
- **Command tree** — `/infusedmobs` with `help`, `nametag [on|off]`, `list`, `summon <tier> [entity] [abilities]`, `reload` subcommands (gamemaster-level permission)
- **Entity tab-completions** — `/infusedmobs summon` now suggests only MONSTER-category entities
- **Ability tab-completions** — space-separated, prefix-filtered, skips already-picked abilities, trailing-space detection for multi-word suggestions
- **Ability validation with fuzzy matching** — unknown ability IDs show "Did you mean?" suggestions via Levenshtein distance
- **Crosshair spawn** — `/infusedmobs summon` spawns at the block the player is looking at (10-block raycast)
- **`/infusedmobs list`** — lists all hostile mob types that can receive a tier
- **Global nametag toggle** — `showNametags` field in config, toggled via `/infusedmobs nametag`
- **Config reload** — `/infusedmobs reload` re-reads `config/infusedmobs.json` at runtime without restart
- **Projectile HURT trigger** — projectiles fired by infused mobs (arrows, fire charges, etc.) now trigger their HURT abilities
- **`MobTier.colourCode()`** — consolidated colour mapping as single source of truth
- **Random ability fallback** — summoning without abilities draws random ones per tier config

### Changed
- **Ability IDs aligned to display names** — `venom`→`bane`, `freeze`→`chill`, `inferno`→`hellfire`, `acid`→`vitriol`, `fortify`→`ward`, `fury`→`frenzy`, `gust`→`wraith`, `bloom`→`blight` (also renamed `fission`→`rupture` in earlier 2.6.0)
- **Simplified summon syntax** — removed `[pos]` argument; always spawns at crosshair; eliminated command priority conflicts with greedyString
- **Improved input validation** — `parseAbilities` now shows fuzzy-matched "Did you mean?" for unknown IDs
- **`hurt()` → `hurtServer()`** — fixed deprecated method call in Combust and Thorns
- **Removed duplicate `findMob`** — `MobTickTrigger` now uses shared `MobTierManager.findMob`
- **Tier colours consolidated** — all colour lookups go through `MobTier.colourCode()` instead of duplicated switches
- **Removed `/infusedmobs info`** — replaced by nametag toggle and `/infusedmobs list`
- **Ability suggestions use spaces** — comma-separated → space-separated (e.g., `bane thorns`)
- **Cached Gson instances** — `ModConfig` reuses static GSON and GSON_PRETTY instead of creating new ones each read/write
- **Split copy ability draw** — retries if a DEATH ability is drawn, preventing empty-ability split copies

### Fixed
- **Entity default fallback bug** — `BuiltInRegistries.ENTITY_TYPE.getValue()` returns default (PIG) for unknown keys; switched to `getOptional()`
- **Tier re-roll bug** — summon command's tier argument was cosmetic; tier was re-rolled by `assignTier()` until `assignSpecificTier()` was added
- **Stale comments** — AbilityRegistry TICK timer said "2 seconds" (corrected to "1 second"); docs refreshed across README + MODRINTH_DESCRIPTION
- **Tab-completion trailing space** — pressing space after an ability now shows remaining abilities instead of `<abilities>` hint
- **Unused imports** — cleaned up in `MobTickTrigger`, `InfusedMobsCommand` (duplicate ArrayList/List)

## [2.5.0] - 2026-07-27

### Added
- **26 unit tests** (up from 21) — new config validation tests for `abilityCount`, `spawnChance`, null tiers
- **Config validation hardened** — `isValid()` now checks all tier fields; old configs fall back to defaults

### Changed
- **Unified ability pool** — abilities drawn from all trigger types mixed together
  - Cinder: 1 ability, Shade: 2, Doom: 3 (was per-type HURT/TICK/DEATH counts)
- **Spawn rates increased** — Cinder 40%, Shade 20%, Doom 10% (was 10%/5%/2.5%)
- **Debuff pass** — all HURT/TICK effects are level I, 3s duration (was II–III, 5s)
  - Chill no longer hardcoded to level III — uses config amplifier like everything else
- **Version bump to 2.5.0**

### Removed
- `BY_TRIGGER` index (dead code — populated but never read since unified pool)
- Redundant shuffle in `getRandomAbilities`
- Stale metadata (fabric.mod.json still referenced Ember/Surge/Tempest)

### Fixed
- Config validation missing `abilityCount` check — old configs silently got 0 abilities

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