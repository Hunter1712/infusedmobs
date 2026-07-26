# Infused Mobs

> **Hostile mobs spawn with elemental-infused tiers and randomized abilities — fully configurable via JSON.**

---

## 🎮 Overview

**Infused Mobs** transforms vanilla hostile mobs into dynamic, unpredictable threats. Instead of every zombie or creeper behaving identically, mobs now spawn with **elemental tiers** that grant them randomized abilities — making every encounter unique and every farm run a gamble.

| Tier | Rarity | Health | XP | Abilities |
|------|--------|--------|-----|-----------|
| **🟢 Cinder** | 40% | 1.5× | 1.5× | 1 (any type) |
| **🟡 Shade** | 20% | 2× | 2× | 2 (any type) |
| **🔴 Doom** | 10% | 4× | 4× | 3 (any type) |

> **~30% of hostile mobs remain vanilla** — infused mobs are common but weaker.

---

## ⚔️ Ability Types

### HURT

| Ability | Effect | Duration | Amplifier |
|---------|--------|----------|-----------|
| **Bane** | Poison | 3s | I |
| **Chill** | Slowness | 3s | I |
| **Decay** | Wither | 3s | I |
| **Hellfire** | Sets target on fire | 3s | — |
| **Siphon** | Heals mob for damage dealt | — | — |
| **Vitriol** | Damages all 4 armor slots (4 durability each) | — | — |
| **Hex** | Weakness | 3s | I |

> **Shield blocks negate all HURT abilities.**

---

### TICK

| Ability | Effect | Duration | Amplifier |
|---------|--------|----------|-----------|
| **Ward** | Resistance | 3s | I |
| **Frenzy** | Strength | 3s | I |
| **Wraith** | Speed | 3s | I |
| **Blight** | Regeneration | 3s | I |
| **Thorns** | Reflects 15% melee damage | — | — |

---

### DEATH

| Ability | Effect |
|---------|--------|
| **Rupture** | Splits into **2 Cinder-tier copies** at 60% health (each gets 1 random HURT ability, no DEATH abilities to prevent infinite recursion) |
| **Combust** | Area damage + explosion sound at death (configurable radius, default 4.0) |

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
  "hurtEffectDuration": 100,
  "hurtEffectAmplifier": 1,
  "tickEffectDuration": 60,
  "tickEffectAmplifier": 1,
  "infernoFireSeconds": 5,
  "acidArmorDamage": 4,
  "combustExplosionPower": 4.0
}
```

### Tweakable Settings

| Setting | Description |
|---------|-------------|
| `spawnChance` | Relative weight for each tier (sum doesn't need to equal 1.0) |
| `hurtAbilities` / `tickAbilities` / `deathAbilities` | How many abilities of each type per tier |
| `healthMultiplier` / `xpMultiplier` | Stat scaling per tier |
| `hurtEffectDuration` / `tickEffectDuration` | Effect duration in **ticks** (20 ticks = 1 second) |
| `hurtEffectAmplifier` / `tickEffectAmplifier` | Effect level (0 = I, 1 = II, etc.) |
| `infernoFireSeconds` | Seconds of fire from Inferno |
| `acidArmorDamage` | Durability damage per armor slot from Acid |
| `combustExplosionPower` | Explosion strength (TNT = 4.0) |

> **Tip:** Set a tier's `spawnChance` to `0` to disable it entirely.

---

## 🛡️ Compatibility

| Mod Loader | Minecraft | Java | Fabric API |
|------------|-----------|------|------------|
| Fabric | 26.2 | 25+ | 0.155.2+ |

- **Client-side:** Required (for announcements, nametags)
- **Server-side:** Required (all logic runs server-side)
- **No custom status effects** — uses only vanilla effects
- **No world gen / block / item additions** — safe to add/remove mid-save

---

## 📦 Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) for Minecraft 26.2
2. Install [Fabric API](https://modrinth.com/mod/fabric-api) (required)
3. Drop `infusedmobs-2.4.0.jar` into your `mods` folder
4. Launch — config generates at `config/infusedmobs.json`

---

## 🔧 Modpack Authors

- **License:** All Rights Reserved — contact author for redistribution permission
- **Config-driven** — no code changes needed for balance tweaks
- **No hard dependencies** beyond Fabric API
- **Tested on:** Fabric 0.19.3+, MC 26.2, Java 25

---

## 🐛 Known Issues

- **Despawned mobs** leave a tiny UUID leak (~16 bytes per unique mob) — negligible
- **Split copies** from Rupture are always Cinder-tier (by design, to prevent infinite recursion)
- **Nametags** use Minecraft color codes (§a, §e, §c, §7) — visible in vanilla

---

## 📝 Changelog

See [CHANGELOG.md](https://github.com/hunter1712/infusedmobs/blob/main/CHANGELOG.md) for full history.

### v2.4.0 Highlights
- **Tier persistence** — mobs keep their tier across world reloads
- **Despawn cleanup** — tier tracking cleaned up when mobs despawn
- **No particle clutter** — TICK effects no longer show particles on mobs
- **Combust quieted** — silent area damage instead of explosion
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