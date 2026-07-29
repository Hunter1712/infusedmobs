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
| **Rupture** | Split into 2 Cinder-tier copies at 60% HP (each gets 1 random ability, no DEATH abilities to prevent recursion) |
| **Combust** | Area damage + explosion sound (no particles / block damage, radius configurable) |

## Commands

All commands require **gamemaster-level permission** (level 2 ops).

| Command | Description |
|---------|-------------|
| `/infusedmobs help` | List all available subcommands |
| `/infusedmobs nametag [on\|off]` | Toggle tier nametags globally (persisted in config) |
| `/infusedmobs list` | List all hostile mob types that can be infused |
| `/infusedmobs summon <tier> [entity] [abilities]` | Spawn an infused mob at crosshair (defaults to zombie). Abilities are optional space-separated IDs (e.g., `bane thorns`) |
| `/infusedmobs reload` | Reload `config/infusedmobs.json` from disk at runtime |

### Nametag Toggle

Tier nametags can be hidden globally via the config file or the in-game command:

```json
{
  "showNametags": false
}
```

When disabled, infused mobs appear with their vanilla names — abilities still apply, you just won't see the tier tag.

## Configuration

File: `config/infusedmobs.json` (auto-generated on first run)

See [MODRINTH_DESCRIPTION.md](MODRINTH_DESCRIPTION.md) for full config reference.

## License

All Rights Reserved — see [LICENSE](https://github.com/hunter1712/infusedmobs/blob/master/LICENSE) for full terms.
Modpack inclusion with credit is permitted; redistribution and derivatives require permission.
