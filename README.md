# Infused Mobs

A Fabric mod for Minecraft 26.2 that gives vanilla hostile mobs occult-infused
tiers with randomized abilities — fully configurable via JSON.

## Tiers

| Tier | Spawn Chance | Abilities | HP | XP | Colour |
|------|-------------|-----------|----|----|--------|
| **Cinder** | 40% | 1 (any type) | 1.5× | 1.5× | Green |
| **Shade** | 20% | 2 (any type) | 2× | 2× | Yellow |
| **Doom** | 10% | 3 (any type) | 4× | 4× | Red |

~30% of hostile mobs remain vanilla — infused mobs are common but effects are weak.

## Abilities

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

### TICK (passive, refreshes every 1s, no particles)

| Ability | Effect | Duration |
|---------|--------|----------|
| **Ward** | Resistance I | 3s |
| **Frenzy** | Strength I | 3s |
| **Wraith** | Speed I | 3s |
| **Blight** | Regeneration I | 3s |
| **Thorns** | Reflect 15% melee damage | — |

### DEATH (trigger on death)

| Ability | Effect |
|---------|--------|
| **Rupture** | Split into 2 Cinder-tier copies at 60% HP (each gets 1 random ability — any except Rupture itself, preventing recursion) |
| **Combust** | Area damage + explosion sound (no particles / block damage, radius configurable) |

## Commands

All commands require **gamemaster-level permission** (level 2 ops).

| Command | Description |
|---------|-------------|
| `/infusedmobs help` | List all available subcommands |
| `/infusedmobs nametag [on\|off]` | Toggle tier nametags globally (persisted in config) |
| `/infusedmobs world add <world>` | Add a world to the blacklist (disables the mod there) |
| `/infusedmobs world remove <world>` | Remove a world from the blacklist |
| `/infusedmobs world list` | Show all blacklisted worlds |
| `/infusedmobs list` | List all hostile mob types that can be infused |
| `/infusedmobs summon <tier> [entity] [abilities]` | Spawn an infused mob at crosshair (defaults to zombie). Abilities are optional space-separated IDs (e.g., `bane thorns`) |
| `/infusedmobs reload` | Reload `config/infusedmobs.json` from disk at runtime |
| `/gamerule infusedmobs:enabled` | Enable/disable the mod in the current world (default `true`) |

### Nametag Toggle

Tier nametags can be hidden globally via the config file or the in-game command:

```json
{
  "showNametags": false
}
```

When disabled, infused mobs appear with their vanilla names — abilities still apply, you just won't see the tier tag.

### World Blacklist

Disable the mod in specific worlds (dimensions) via a blacklist. In blacklisted worlds, mobs spawn as vanilla — no tiers, no abilities, no nametags, and `/infusedmobs summon` is refused.

```json
{
  "worldBlacklist": ["minecraft:overworld"]
}
```

Manage at runtime with `/infusedmobs world add|remove <world>` (tab-completes loaded dimension ids; the `minecraft:` namespace is optional, e.g. `overworld` = `minecraft:overworld`). The blacklist is persisted to `config/infusedmobs.json`.

### Gamerules

The same control is exposed as a per-world gamerule, so modpack makers can set it at launch (datapack JSON, gamerule-modifying mods) or in-game:

| Gamerule | Default | Effect when `false` |
|----------|---------|---------------------|
| `infusedmobs:enabled` | `true` | Mod fully disabled in this world — mobs spawn as vanilla, no tiers/abilities/nametags, `/infusedmobs summon` refused |

It persists in the world save (survives restarts) and is combined with the config settings: the mod is active unless the world is blacklisted **or** `infusedmobs:enabled` is `false`.

```mcfunction
/gamerule infusedmobs:enabled false
```

## Configuration

File: `config/infusedmobs.json` (auto-generated on first run)

See [MODRINTH_DESCRIPTION.md](MODRINTH_DESCRIPTION.md) for full config reference.

## License

All Rights Reserved — see [LICENSE](https://github.com/hunter1712/infusedmobs/blob/master/LICENSE) for full terms.
Modpack inclusion with credit is permitted; redistribution and derivatives require permission.
