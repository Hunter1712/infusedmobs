# Infused Mobs

A Fabric mod for Minecraft 1.20.1 / 1.21.1 / 26.2 that gives vanilla hostile mobs occult-infused
tiers with randomized abilities — fully configurable via JSON.

One project page covers all three versions — download `infusedmobs-<mod>+<mc>.jar` matching your game version
(e.g. `infusedmobs-2.7.1+1.20.1.jar`, `infusedmobs-2.7.1+1.21.1.jar`, `infusedmobs-2.7.1+26.2.jar`).

## Tiers

| Tier | Spawn Chance | Abilities | HP | XP | Colour |
|------|-------------|-----------|----|----|--------|
| **Cinder** | 40% | 1 (any type) | 1.5× | 1.5× | Green |
| **Shade** | 20% | 2 (any type) | 2× | 2× | Yellow |
| **Doom** | 10% | 3 (any type) | 4× | 4× | Red |

~30% of hostile mobs remain vanilla at defaults — tiers roll as a single uniform decision (Doom, then Shade, then Cinder intervals), so effective shares equal the configured chances (40% Cinder / 20% Shade / 10% Doom). Infused mobs are common but effects are weak.

## Abilities

HURT fires on damage involving an Infused Mob and a player: offensive Abilities fire when the mob damages a player (melee or projectile); Thorns fires reactively when the mob is damaged by a player, reflecting a fraction back. Mob-vs-mob hits never trigger Abilities.

### HURT (offensive: fire on melee or projectile hit against a player, blocked by shields on 1.21.1/26.2; reactive: Thorns fires when the mob is damaged by a player)

| Ability | Effect | Duration |
|---------|--------|----------|
| **Bane** | Poison I | 3s |
| **Chill** | Slowness I | 3s |
| **Decay** | Wither I | 3s |
| **Hellfire** | Ignite | 5s |
| **Siphon** | Heal 100% of damage dealt | — |
| **Vitriol** | 4 durability to all armor | — |
| **Hex** | Weakness I | 3s |
| **Thorns** | Reflect 15% incoming damage back at the attacker | — |

> **Shield blocks negate all HURT abilities on 1.21.1 and 26.2.** On 1.20.1 the legacy damage event reports pre-mitigation amounts with no shield-block flag, so a fully-blocked hit still fires HURT abilities there.

### TICK (passive, refreshes every 1s, no particles)

| Ability | Effect | Duration |
|---------|--------|----------|
| **Ward** | Resistance I | 3s |
| **Frenzy** | Strength I | 3s |
| **Wraith** | Speed I | 3s |
| **Blight** | Regeneration I | 3s |

### DEATH (trigger on death)

| Ability | Effect |
|---------|--------|
| **Rupture** | Split into 2 copies with full Cinder health and XP at 60% HP (grey tag, no Tier — each gets 1 random ability, any except Rupture itself, preventing recursion) |
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
| `/infusedmobs summon <tier> [entity] [abilities]` | Spawn an Infused Mob at crosshair (defaults to zombie). Abilities are optional space-separated IDs (e.g., `bane thorns`) and require an entity argument |
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
