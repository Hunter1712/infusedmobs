# Configuration

Everything about Infused Mobs — spawn odds, ability counts, stat scaling, effect strength and the gating lists — is driven by one JSON file:

```
config/infusedmobs.json
```

It is generated with defaults on first run. Edit it and run `/infusedmobs reload`, or use the in-game commands (see [commands.md](commands.md)).

## Default file

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
  "combustExplosionPower": 4.0,
  "showNametags": true,
  "worldBlacklist": [],
  "mobBlacklist": [],
  "configVersion": 4
}
```

## Per-tier settings

Each of `cinder`, `shade` and `doom` takes the same four fields.

| Field | Type | Default (Cinder / Shade / Doom) | Valid range |
|-------|------|-------------------------------|-------------|
| `spawnChance` | number | `0.4` / `0.2` / `0.1` | `(0, 1]` |
| `abilityCount` | integer | `1` / `2` / `3` | `1 … 14` (the pool size) |
| `healthMultiplier` | number | `1.5` / `2.0` / `4.0` | `≥ 1.0` |
| `xpMultiplier` | number | `1.5` / `2.0` / `4.0` | `≥ 1.0` |

- `spawnChance` is the tier's **effective** share of hostile spawns (the roll is a single uniform interval — see [tiers.md](tiers.md#how-the-roll-works)). The three don't have to sum to 1; the remainder rolls vanilla.
- `abilityCount` is capped at the live ability pool size (14). It controls how many abilities the tier draws; there is no per-tier ability selection.
- `healthMultiplier` scales the mob's max health; `xpMultiplier` scales its XP reward.

## Global settings

| Field | Type | Default | Valid range | Applies to |
|-------|------|---------|-------------|------------|
| `hurtEffectDuration` | integer (ticks) | `60` | `≥ 1` | Bane, Chill, Decay, Hex |
| `hurtEffectAmplifier` | integer | `0` | `0 … 5` | Bane, Chill, Decay, Hex |
| `tickEffectDuration` | integer (ticks) | `60` | `≥ 1` | Ward, Frenzy, Wraith, Blight |
| `tickEffectAmplifier` | integer | `0` | `0 … 5` | Ward, Frenzy, Wraith, Blight |
| `infernoFireSeconds` | integer (seconds) | `5` | `≥ 1` | Hellfire |
| `acidArmorDamage` | integer | `4` | `≥ 1` | Vitriol |
| `combustExplosionPower` | number | `4.0` | `0.5 … 10.0` | Combust |
| `showNametags` | boolean | `true` | `true`/`false` | Tier nametag visibility |
| `worldBlacklist` | list of strings | `[]` | `namespace:path` ids | Gating (below) |
| `mobBlacklist` | list of strings | `[]` | `namespace:path` ids | Gating (below) |
| `configVersion` | integer | `4` | — | Internal; **do not edit** |

Notes:

- **Durations are in ticks** — 20 ticks = 1 second, so the default `60` is 3 seconds. (`infernoFireSeconds` is the exception and is in seconds.)
- **Amplifiers are zero-based**: `0` = level I, `1` = level II, and so on.
- `infernoFireSeconds` keeps the name of the old ability spelling; it sets **Hellfire**'s burn time.
- `combustExplosionPower` controls the Combust radius (`power × 2` blocks) and its damage. `4.0` matches TNT's power; `0.5` is a small pop, `10` is huge. Combust still causes no block damage.
- `showNametags` is the same setting toggled by `/infusedmobs nametag` — the command rewrites this field.

## Gating

Three independent controls decide whether a mob is infused. A mob is infused only when **all** applicable gates are open.

| Gate | Scope | Set by |
|------|-------|--------|
| `infusedmobs:enabled` gamerule | Per world save | `/gamerule`, datapacks, gamerule mods |
| `worldBlacklist` | Per dimension | Config or `/infusedmobs world …` |
| `mobBlacklist` | Per entity type | Config or `/infusedmobs mob …` |

The per-dimension decision is:

```
active  ⇔  dimension is NOT in worldBlacklist  AND  infusedmobs:enabled is true
```

The **World Blacklist dominates**: if a dimension is blacklisted, the gamerule value is irrelevant there. In an inactive dimension, mobs spawn vanilla and `/infusedmobs summon` is refused.

### World Blacklist

Dimension ids where the mod is disabled, e.g.:

```json
"worldBlacklist": ["minecraft:overworld", "mythic:underworld"]
```

### Mob Blacklist

Entity type ids excluded from **natural** infusion. Explicit `/infusedmobs summon` **bypasses** this list.

```json
"mobBlacklist": ["minecraft:spider", "spiders:polymerized_spider"]
```

Both lists accept vanilla or modded ids. Entries are trimmed, blanks are dropped, duplicates are collapsed (first occurrence kept), and ids that aren't shaped `namespace:path` are dropped with a warning.

## Validation, clamping and backfill

On load (at startup and on `/infusedmobs reload`) the config is **repaired rather than discarded**:

1. **Malformed JSON** (unparseable) → the file is rewritten with full defaults.
2. **Valid JSON** → every out-of-range value is **clamped to its nearest bound**, with one warning per fix in the log. For example:
   - `spawnChance` above `1` clamps to `1.0`; zero, negative or `NaN` falls back to that tier's default chance (the open lower bound has no nearest value).
   - `abilityCount` clamps into `1 … 14`.
   - multipliers below `1.0` clamp to `1.0`.
   - `combustExplosionPower` clamps into `0.5 … 10`.
   - malformed blacklist ids are dropped.
3. **Missing fields** (e.g. an older config) are **backfilled from defaults**, so your existing tier/effect edits survive an upgrade. Unknown or removed fields (such as `showAnnouncements` from older versions) are ignored.

If anything was repaired, the corrected file is **written back to disk**. A valid, in-range, current-version config is left untouched.

> This corrects a common misconception: it isn't only malformed JSON that gets rewritten. A config missing newer fields, or holding any out-of-range value, is normalised and saved. See `configVersion` in the file — `4` is current, and older versions are backfilled.

`configVersion` is internal bookkeeping (`v1` = 2.6.0, `v2` = 2.7.0 added `worldBlacklist`, `v3` = announcements removed, `v4` = 2.8.0 added `mobBlacklist`). Leave it alone.

## Reloading

- `/infusedmobs reload` re-reads the file and refreshes nametags — no restart needed.
- Ability effect values are read **at fire time**, so duration/amplifier changes apply to the next hit or tick.
- Blacklist changes apply on the next spawn decision.
- Tier spawn chances apply to future rolls; already-assigned mobs keep their persisted roll.
