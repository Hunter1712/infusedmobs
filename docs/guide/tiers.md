# Tiers

A **Tier** is the power level an Infused Mob rolls at spawn. It decides how many Abilities the mob gets, how tough it is, and what colour its nametag is. A mob that rolls no Tier stays vanilla.

## The three tiers

| Tier | Colour | Spawn chance | Abilities | Max health | XP reward |
|------|--------|--------------|-----------|------------|-----------|
| **Cinder** | §aGreen | 40% | 1 | 1.5× | 1.5× |
| **Shade** | §eYellow | 20% | 2 | 2× | 2× |
| **Doom** | §cRed | 10% | 3 | 4× | 4× |

At defaults that leaves roughly **30% of hostile mobs vanilla** (1 − 0.4 − 0.2 − 0.1). Infused mobs are common; individual abilities are kept weak on purpose, and everything above is configurable — see [configuration.md](configuration.md).

Spawn chance, ability count, health multiplier and XP multiplier are the four per-tier tunables. Their defaults live in one place in the code and flow into the config, so the table above is always the config's default state.

## How the roll works

All three chances are resolved in a **single uniform roll** in `[0, 1)`:

| Roll range | Result |
|------------|--------|
| `[0.0, 0.1)` | **Doom** |
| `[0.1, 0.3)` | **Shade** |
| `[0.3, 0.7)` | **Cinder** |
| `[0.7, 1.0)` | nothing — vanilla |

Because the intervals are disjoint and sized exactly by each tier's configured chance, the **effective share of each tier equals its configured chance** (10% / 20% / 40% at defaults). There is no second roll and no chance of an "empty" tier slot.

The tiers are checked **rarest first** (Doom, then Shade, then Cinder). If your configured chances add up to more than 100%, this means the *common* tier is truncated first — the rare tier is never the one you lose.

> The single-roll rule lives in its own unit-tested class; see [docs/adr/](../adr/) for the surrounding architecture.

## Which mobs can be infused

- Only mobs in the **`MONSTER` category** — zombies, skeletons, spiders, creepers, and other hostile mobs.
- **Animals, villagers and other non-hostiles are never infused** naturally.
- Mobs on the **Mob Blacklist** are excluded from natural rolls (see [configuration.md](configuration.md#mob-blacklist)).
- Worlds that are blacklisted, or where the gamerule is off, roll nothing at all (see [configuration.md](configuration.md#gating)).
- The `/infusedmobs summon` command **bypasses the category and Mob Blacklist**, so it can tier any mob on demand — see [commands.md](commands.md#infusedmobs-summon).

## When the roll happens, and that it sticks

The Tier is decided when the mob first enters the world, then **persisted by the mob's UUID**. On chunk reloads and world restarts the mob restores the *exact same* Tier and ability set — it is never re-rolled, and stats are never multiplied twice. Mobs that rolled nothing stay vanilla permanently for that life.

- **Health** is set to `base max health × health multiplier`, and the mob is fully healed.
- **XP** is the mob's normal reward multiplied by the tier's XP multiplier, rounded to the nearest whole point. The multiplier is read from the config, so changing a tier's XP value affects future kills.
- **Abilities** are drawn at random from the shared pool, with no duplicates within one mob (see [abilities.md](abilities.md)).

Persistence is stored per world in `data/infusedmobs_tiers.dat`. Corrupted or unknown saved data degrades to "vanilla" rather than failing the world load. Tracking is cleared when the mob dies or despawns.

## Nametags

Each Infused Mob gets a coloured nametag showing its ability names and its vanilla mob name:

```
§aBane§7, Thorns §fZombie
```

- The **tier colour** starts the tag (green/yellow/red for Cinder/Shade/Doom).
- Ability names follow in the tier colour, separated by grey commas.
- The mob's **vanilla name** is white at the end.

Nametags can be hidden globally with the `showNametags` config field or `/infusedmobs nametag off` — abilities still fire, you just don't see the tag. See [commands.md](commands.md#infusedmobs-nametag-onoff).

**Rupture split copies** (see [abilities.md](abilities.md#rupture)) are not a Tier: they carry Cinder *stats* but show a **grey** nametag with no tier colour.
