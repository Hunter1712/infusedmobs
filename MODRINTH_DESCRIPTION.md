# Infused Mobs

> **Hostile mobs spawn with occult-infused tiers and randomized abilities — fully configurable via JSON.**

---

## 🎮 Overview

**Infused Mobs** transforms vanilla hostile mobs into dynamic, unpredictable threats. Instead of every zombie or creeper behaving identically, mobs now spawn with **occult tiers** that grant them randomized abilities — making every encounter unique and every farm run a gamble.

Abilities are drawn from a **unified pool** (all trigger types mixed together). A Cinder mob might get a HURT poison ability, a TICK speed buff, or a DEATH explosion — you never know what you'll face.

| Tier | Spawn Chance | Health | XP | Abilities |
|------|-------------|--------|-----|-----------|
| **🟢 Cinder** | 40% | 1.5× | 1.5× | 1 (any type) |
| **🟡 Shade** | 20% | 2× | 2× | 2 (any type) |
| **🔴 Doom** | 10% | 4× | 4× | 3 (any type) |

> **~30% of hostile mobs remain vanilla** — infused mobs are common but effects are weak (configurable).

---

## ⚔️ Ability Types

### HURT (fire on melee hit or projectile hit, blocked by shields)

| Ability | Effect | Duration |
|---------|--------|----------|
| **Bane** | Poison I | 3s |
| **Chill** | Slowness I | 3s |
| **Decay** | Wither I | 3s |
| **Hellfire** | Ignite | 5s |
| **Siphon** | Heal 100% of damage dealt | — |
| **Vitriol** | 4 durability to all armor | — |
| **Hex** | Weakness I | 3s |

> **Shield blocks negate all HURT abilities.** Projectiles (arrows, fire charges, etc.) fired by infused mobs also trigger their HURT abilities on hit.

---

### TICK (passive, refreshes every 1s, no particles)

| Ability | Effect | Duration |
|---------|--------|----------|
| **Ward** | Resistance I | 3s |
| **Frenzy** | Strength I | 3s |
| **Wraith** | Speed I | 3s |
| **Blight** | Regeneration I | 3s |
| **Thorns** | Reflects 15% melee damage | — |

---

### DEATH (trigger on kill)

| Ability | Effect |
|---------|--------|
| **Rupture** | Splits into **2 Cinder-tier copies** at 60% health (each gets 1 random ability — any except Rupture itself, preventing recursion) |
| **Combust** | Area damage + explosion sound at death (configurable power, default 4.0 — TNT = 4.0) |

---

## 🎮 Commands

All commands require **gamemaster-level permission** (level 2 ops).

| Command | Description |
|---------|-------------|
| `/infusedmobs help` | List all available subcommands |
| `/infusedmobs nametag [on\|off]` | Toggle tier nametags globally (persisted in config) |
| `/infusedmobs list` | List all hostile mob types that can be infused |
| `/infusedmobs summon <tier> [entity] [abilities]` | Spawn an infused mob at crosshair (defaults to zombie). Abilities are optional space-separated IDs (e.g., `bane thorns`) |
| `/infusedmobs reload` | Reload `config/infusedmobs.json` from disk at runtime |
| `/infusedmobs world add\|remove <world>` | Blacklist/unblacklist a world dimension (disables the mod there) |
| `/infusedmobs world list` | Show all blacklisted worlds |
| `/gamerule infusedmobs:enabled` | Enable/disable the mod in the current world (default `true`) |

### 🎮 Gamerules

The mod ships one gamerule for per-world control at launch — set it with any gamerule-modifying mod, a datapack, or in-game. It persists in the world save and survives restarts:

| Gamerule | Default | When `false` |
|----------|---------|--------------|
| `infusedmobs:enabled` | `true` | Mod fully disabled in this world — vanilla mobs, no tiers/abilities/nametags, `/infusedmobs summon` refused |

The gamerule is ANDed with the config settings: the mod is active unless the world is blacklisted **or** `infusedmobs:enabled` is `false`.

```mcfunction
/gamerule infusedmobs:enabled false
```

---

## ⚙️ Configuration

**File:** `config/infusedmobs.json` (auto-generated on first run)

```json
{
  "cinder": {
    "spawnChance": 0.4,
    "abilityCount": 1,
    "healthMultiplier": 1.5,
    "xpMultiplier": 1.5
  },
  "shade": {
    "spawnChance": 0.2,
    "abilityCount": 2,
    "healthMultiplier": 2.0,
    "xpMultiplier": 2.0
  },
  "doom": {
    "spawnChance": 0.1,
    "abilityCount": 3,
    "healthMultiplier": 4.0,
    "xpMultiplier": 4.0
  },
  "showNametags": true,
  "hurtEffectDuration": 60,
  "hurtEffectAmplifier": 0,
  "tickEffectDuration": 60,
  "tickEffectAmplifier": 0,
  "infernoFireSeconds": 5,
  "acidArmorDamage": 4,
  "combustExplosionPower": 4.0,
  "worldBlacklist": [],
  "configVersion": 3
}
```

### Tweakable Settings

| Setting | Description |
|---------|-------------|
| `showNametags` | Whether tier nametags are shown on infused mobs (toggleable in-game) |
| `spawnChance` | Probability for this tier (0.0–1.0) |
| `abilityCount` | How many abilities the tier draws from the unified pool |
| `healthMultiplier` / `xpMultiplier` | Stat scaling per tier |
| `hurtEffectDuration` | HURT effect duration in **ticks** (20 = 1s) |
| `hurtEffectAmplifier` | HURT effect amplifier (0 = I, 1 = II, etc.) |
| `tickEffectDuration` | TICK effect duration in **ticks** |
| `tickEffectAmplifier` | TICK effect amplifier |
| `infernoFireSeconds` | Seconds of fire from Hellfire |
| `acidArmorDamage` | Durability damage per armor slot from Vitriol |
| `combustExplosionPower` | Explosion strength (TNT = 4.0) |
| `worldBlacklist` | Dimension IDs where the mod is disabled (e.g. `"minecraft:overworld"`) — managed in-game via `/infusedmobs world add\|remove\|list` |

> **Note:** Values are validated on load — e.g. `spawnChance` and `abilityCount` must be greater than 0. An invalid config is replaced with defaults (with a warning in the log), so tier edits are preserved across upgrades but not past validation errors.

---

## 🛡️ Compatibility

| Mod Loader | Minecraft | Java | Fabric API |
|------------|-----------|------|------------|
| Fabric | 26.2 | 25+ | 0.155.2+ |

- **Client-side:** Required (for nametags)
- **Server-side:** Required (all logic runs server-side)
- **No custom status effects** — uses only vanilla effects
- **No world gen / block / item additions** — safe to add/remove mid-save

---

## 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.2
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) (required)
3. Drop `infusedmobs-2.7.1.jar` into your `mods` folder
4. Launch — config generates at `config/infusedmobs.json`

---

## 🔧 Modpack Authors

- **License:** All Rights Reserved — modpack inclusion with credit is allowed (see [LICENSE](https://github.com/hunter1712/infusedmobs/blob/master/LICENSE))
- **Config-driven** — no code changes needed for balance tweaks
- **Gamerule-driven** — the `infusedmobs:enabled` gamerule lets pack makers disable the mod per world at launch
- **No hard dependencies** beyond Fabric API
- **Tested on:** Fabric 0.19.3+, MC 26.2

---

## 🐛 Known Issues

- **Split copies** from Rupture are always Cinder-tier (by design), and never roll Rupture again (recursion guard)
- **Nametags** use Minecraft color codes (§a, §e, §c, §7) — visible in vanilla
- **Old configs** (pre-2.5.0) lack the required `abilityCount` field — they are automatically replaced with defaults on load (tier edits from before 2.5.0 are lost)

---

## 📝 Changelog

See [CHANGELOG.md](https://github.com/hunter1712/infusedmobs/blob/master/CHANGELOG.md) for full history.

### v2.7.1 Highlights
- **Gamerule** — `infusedmobs:enabled` disables the mod per world (blacklist parity for modpack makers); persists in the world save and combines with the config blacklist
- **Announcements removed** — the first-encounter chat message and its config/gamerule toggles have been removed entirely
- **Split copies roll anything except Rupture** — a Rupture copy's ability can now be any ability (including Combust); only Rupture itself is excluded (it would split forever)
- **Summoned mobs persist** — `/infusedmobs summon` results survive chunk reloads and restarts exactly as chosen (no re-roll)
- **Reload-proof copies** — split copies keep their Cinder-tier health across chunk reloads
- **Crash-proof saves** — corrupted tier data degrades to vanilla instead of failing world load

### v2.7.0 Highlights
- **World blacklist** — `worldBlacklist` config field disables the mod in specific dimensions; manage at runtime via `/infusedmobs world add|remove|list` (tab-completes dimension IDs)
- **Announcement toggle** — `showAnnouncements` config field + `/infusedmobs announce [on|off]` hide the first-encounter chat message while abilities still fire
- **Summon respects blacklist** — `/infusedmobs summon` refuses in blacklisted worlds
- **Config backfill on upgrade** — pre-2.7.0 configs upgrade automatically, preserving tier settings

### v2.6.0 Highlights
- **Command system** — `/infusedmobs` with `help`, `nametag`, `list`, `summon`, and `reload` subcommands
- **Summon improvements** — crosshair spawn position, entity tab-completions, ability tab-completions, fuzzy-match validation of ability IDs
- **Ability IDs aligned to display names** — `venom`→`bane`, `freeze`→`chill`, `inferno`→`hellfire`, `acid`→`vitriol`, `fortify`→`ward`, `fury`→`frenzy`, `gust`→`wraith`, `bloom`→`blight`
- **Simplified summon syntax** — removed `[pos]` argument; always spawns at crosshair; no more command priority conflicts
- **Nametag toggle** — `showNametags` config field, toggleable in-game via `/infusedmobs nametag`
- **Projectile support** — projectiles fired by infused mobs now trigger their HURT abilities
- **Config reload** — `/infusedmobs reload` re-reads config at runtime
- **Internal cleanup** — `hurt()`→`hurtServer()`, colour mapping consolidated, cached Gson instances, 28 tests

### v2.5.0 Highlights
- **Unified ability pool** — abilities drawn from all trigger types mixed together (Cinder 1, Shade 2, Doom 3)
- **Increased spawn rates** — Cinder 40%, Shade 20%, Doom 10% (configurable)
- **Debuff pass** — all effects are level I, 3s duration
- **Config validation hardened** — old/invalid configs fall back to defaults
- **Internal cleanup** — dead code removed, tests expanded to 26

### v2.4.0 Highlights
- **Tier persistence** — mobs keep their tier across world reloads
- **Despawn cleanup** — tier tracking cleaned up when mobs despawn
- **No particle clutter** — TICK effects no longer show particles on mobs
- **Combust sound** — explosion sound instead of silent damage
- Fixed duplicated nametags and announcement names

---

## 🔗 Links

- **Source:** [github.com/hunter1712/infusedmobs](https://github.com/hunter1712/infusedmobs)
- **Issues:** [github.com/hunter1712/infusedmobs/issues](https://github.com/hunter1712/infusedmobs/issues)
- **Fabric API:** [modrinth.com/mod/fabric-api](https://modrinth.com/mod/fabric-api)

---

## 📜 License

**All Rights Reserved** — see [LICENSE](https://github.com/hunter1712/infusedmobs/blob/master/LICENSE) for full terms.  
Modpack inclusion with credit is permitted; redistribution and derivatives require permission.

---

*Made with ☕ for Fabric 26.2*