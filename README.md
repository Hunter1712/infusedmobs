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

### TICK (passive, refreshes every 2s, no particles)

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

## Configuration

File: `config/infusedmobs.json` (auto-generated on first run)

See [MODRINTH_DESCRIPTION.md](MODRINTH_DESCRIPTION.md) for full config reference.

## License

ARR — All Rights Reserved
