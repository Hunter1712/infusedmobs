# Commands

All `/infusedmobs` subcommands require **gamemaster permission (operator level 2)**. Bare `/infusedmobs` prints the same help as `/infusedmobs help`.

| Command | What it does |
|---------|--------------|
| `/infusedmobs help` | List every subcommand |
| `/infusedmobs nametag [on\|off]` | Show or set tier-nametag visibility (persisted to config) |
| `/infusedmobs world add\|remove <world>` | Add/remove a dimension on the **World Blacklist** |
| `/infusedmobs world list` | Show the World Blacklist |
| `/infusedmobs mob add\|remove <entity>` | Add/remove a type on the **Mob Blacklist** |
| `/infusedmobs mob list` | Show the Mob Blacklist |
| `/infusedmobs list` | List hostile mob types that can be infused |
| `/infusedmobs summon <tier> [entity] [abilities]` | Spawn an Infused Mob at your crosshair |
| `/infusedmobs reload` | Re-read `config/infusedmobs.json` from disk |
| `/gamerule infusedmobs:enabled [true\|false]` | Enable/disable the mod in the current world |

---

## `/infusedmobs nametag [on|off]`

Turns tier nametags on or off **globally** and persists the choice to `showNametags` in the config. With no argument it just reports the current state. Changing it instantly refreshes every loaded Infused Mob.

- `off` hides all tier tags. Abilities **still fire** — you simply can't see the tag.
- This is the same setting as the config field `showNametags`; use whichever you prefer.

## `/infusedmobs world add|remove <world>` and `world list`

Manages the **World Blacklist** — dimensions where the mod is fully disabled. In a blacklisted dimension, mobs spawn vanilla (no tiers, abilities or nametags) and `/infusedmobs summon` is refused.

- `<world>` is a dimension id such as `minecraft:overworld` or `minecraft:the_nether`. The `minecraft:` namespace may be omitted (`overworld` = `minecraft:overworld`).
- Tab-completion offers the dimension you're standing in plus every loaded dimension.
- Changes are persisted to `worldBlacklist` in the config immediately. Running `world` or `world list` with an empty list reports that the mod is active everywhere.

## `/infusedmobs mob add|remove <entity>` and `mob list`

Manages the **Mob Blacklist** — entity types excluded from *natural* infusion. Blacklisted types always spawn vanilla.

- `<entity>` is an entity type id such as `minecraft:spider`; tab-completion offers infusable hostile types.
- **`/infusedmobs summon` bypasses this list** — it is explicit operator intent, so you can still summon a blacklisted type as an Infused Mob.
- Changes are persisted to `mobBlacklist` in the config immediately. No restart is needed.

## `/infusedmobs list`

Lists all **hostile mob types that can be infused** — the natural-roll candidates. Use it to see what's eligible before deciding what to blacklist.

## `/infusedmobs reload`

Re-reads `config/infusedmobs.json` from disk and refreshes nametags, with **no restart**. Use it after hand-editing the config. Effect durations, amplifiers, blacklists and multipliers all take effect on the next relevant event.

> Reloading a **valid, in-range** config doesn't rewrite the file. If the file is missing fields or has out-of-range values, the corrected version is written back — see [configuration.md](configuration.md#validation-clamping-and-backfill).

---

## `/infusedmobs summon <tier> [entity] [abilities]`

Spawns an Infused Mob at the block you're looking at (up to 10 blocks away; console callers spawn at their position).

| Argument | Required? | Notes |
|----------|-----------|-------|
| `<tier>` | Yes | `cinder`, `shade` or `doom` (case-insensitive) |
| `[entity]` | No | Any entity type; defaults to `zombie` |
| `[abilities]` | No | Space-separated ability IDs, e.g. `"bane thorns"` |

Rules:

- **Abilities require an entity argument.** Type `/infusedmobs summon doom zombie "bane thorns"`, not `/infusedmobs summon doom "bane thorns"`.
- If you omit `[abilities]`, the mob draws random abilities for its tier (1/2/3 by default).
- Ability IDs are tab-completed and fuzzy-checked. A typo produces an **unknown ID** error with a "did you mean" hint and the valid ID list — nothing is spawned.
- Summon **bypasses the Mob Blacklist and the MONSTER-category restriction**, so you can tier any mob, including passive ones.
- Summon **refuses** in a blacklisted dimension or when `infusedmobs:enabled` is `false`, with a distinct message telling you which gate is closed.
- The result is persisted exactly as chosen: the mob keeps the tier and ability set you gave it across chunk reloads and restarts — it is never re-rolled.

Examples:

```mcfunction
/infusedmobs summon cinder
/infusedmobs summon shade skeleton
/infusedmobs summon doom zombie "bane thorns siphon"
```

The valid ability IDs are listed in [abilities.md](abilities.md).

---

## Gamerule: `infusedmobs:enabled`

The mod ships one gamerule for per-world control at launch:

| Gamerule | Default | Category | Persists |
|----------|---------|----------|----------|
| `infusedmobs:enabled` | `true` | Mobs | In the world save |

When set to `false`, the mod is **fully disabled in that world**: mobs spawn vanilla, no tiers/abilities/nametags, and `/infusedmobs summon` is refused.

```mcfunction
/gamerule infusedmobs:enabled false
```

The gamerule is combined with the World Blacklist: the mod is active in a dimension only if that dimension is **not blacklisted** *and* the gamerule is on. The blacklist dominates. It persists across restarts and can be set by datapacks or gamerule-modifying mods at pack launch. See [configuration.md](configuration.md#gating) for the full decision logic.
