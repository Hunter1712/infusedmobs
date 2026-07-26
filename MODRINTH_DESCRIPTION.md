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

### HURT (fire on melee hit, blocked by shields)

| Ability | Effect | Duration |
|---------|--------|----------|
| **Bane** | Poison I | 3s |
| **Chill** | Slowness I | 3s |
| **Decay** | Wither I | 3s |
| **Hellfire** | Ignite | 3s |
| **Siphon** | Heal 100% of damage dealt | — |
| **Vitriol** | 4 durability to all armor | — |
| **Hex** | Weakness I | 3s |

> **Shield blocks negate all HURT abilities.**

---

### TICK (passive, refreshes every 2s, no particles)

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
| **Rupture** | Splits into **2 Cinder-tier copies** at 60% health (each gets 1 random ability from the unified pool, DEATH abilities filtered to prevent recursion) |
| **Combust** | Area damage + explosion sound at death (configurable power, default 4.0 — TNT = 4.0) |

---

## 🎯 Announcements

The **first time** an infused mob hits you, a chat message announces its tier and all abilities:

```
⚡ DOOM Zombie has: Bane, Ward, Rupture
```

This only happens once per mob per player — no spam.

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
  "hurtEffectDuration": 60,
  "hurtEffectAmplifier": 0,
  "tickEffectDuration": 60,
  "tickEffectAmplifier": 0,
  "infernoFireSeconds": 5,
  "acidArmorDamage": 4,
  "combustExplosionPower": 4.0
}
```

### Tweakable Settings

| Setting | Description |
|---------|-------------|
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

> **Tip:** Set a tier's `spawnChance` to `0` to disable it entirely.

---

## 🛡️ Compatibility

| Mod Loader | Minecraft | Java | Fabric API |
|------------|-----------|------|------------|
| Fabric | 26.2 | 21+ | 0.158.0+ |

- **Client-side:** Required (for announcements, nametags)
- **Server-side:** Required (all logic runs server-side)
- **No custom status effects** — uses only vanilla effects
- **No world gen / block / item additions** — safe to add/remove mid-save

---

## 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.2
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) (required)
3. Drop `infusedmobs-2.5.0.jar` into your `mods` folder
4. Launch — config generates at `config/infusedmobs.json`

---

## 🔧 Modpack Authors

- **License:** All Rights Reserved — contact author for redistribution permission
- **Config-driven** — no code changes needed for balance tweaks
- **No hard dependencies** beyond Fabric API
- **Tested on:** Fabric 0.19.3+, MC 26.2

---

## 🐛 Known Issues

- **Split copies** from Rupture are always Cinder-tier (by design, to prevent recursion)
- **Nametags** use Minecraft color codes (§a, §e, §c, §7) — visible in vanilla
- **Old configs** (pre-2.5.0) must be deleted; the new `abilityCount` field is required

---

## 📝 Changelog

See [CHANGELOG.md](https://github.com/hunter1712/infusedmobs/blob/main/CHANGELOG.md) for full history.

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

**All Rights Reserved** — see [LICENSE](LICENSE) for details.  
Contact the author for redistribution or modpack inclusion permission.

---

*Made with ☕ for Fabric 26.2*