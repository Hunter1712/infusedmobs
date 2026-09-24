# Modpack authors

Infused Mobs is **All Rights Reserved**, but the licence explicitly permits including it in a modpack under a few conditions. This page summarises the practical parts — the authoritative text is [LICENSE](../../LICENSE).

## What you may do

- **Include the mod in a pack** distributed through **CurseForge, Modrinth, or an official launcher with a distribution agreement**. Third-party hosting, mirrors and direct download links are not permitted.
- **Monetise the pack** only through CurseForge/Modrinth's standard systems (sponsored links, project-page banner ads). Any other monetisation needs written permission.
- **Create content** — videos, streams, screenshots, reviews — and monetise it normally. Credit is appreciated, not required, for content.

## What you must do

- Give **clear, prominent credit** in the pack description with:
  1. the mod name — **Infused Mobs**
  2. the author — **hunter1712**
  3. a link to the mod's official exchange page.
- Don't name or brand the pack so it's confusable with Infused Mobs.

## What you may not do

- Re-upload, mirror or rehost the jar outside the allowed platforms.
- Release a fork, port, patch or derivative.
- Extract or reuse the mod's assets.
- Bundle a **modified** build into a pack (private edits are allowed for personal/private-server use only, never redistributed).

## Tuning it for your pack

No code changes are needed — everything pack-relevant is a config value or a gamerule.

### Ship balanced defaults

Put an edited `config/infusedmobs.json` in your pack's overrides so every instance starts with your balance. The file is fully documented in [configuration.md](configuration.md). Common levers:

| Goal | Setting |
|------|---------|
| Make infused mobs rarer | Lower the three `spawnChance` values |
| Make them weaker | Lower `healthMultiplier` / `xpMultiplier`; lower effect amplifiers |
| Make abilities shorter | Lower `hurtEffectDuration` / `tickEffectDuration` |
| Tame death explosions | Lower `combustExplosionPower` (range `0.5 … 10`) |
| Hide tier tags | `showNametags: false` |
| Keep specific mobs vanilla | Add them to `mobBlacklist` |

### Disable at launch, per world

Set the gamerule at pack launch with a datapack or a gamerule-modifying mod:

```mcfunction
/gamerule infusedmobs:enabled false
```

It persists in the world save. For dimension-level control, use `worldBlacklist` in the config. See [configuration.md](configuration.md#gating).

## Dependencies

- **Fabric Loader 0.19.3+** and **Fabric API** are required; nothing else.
- No custom blocks, items, effects or worldgen, so it won't fight your world-gen or content mods.
- Safe to add to or remove from an existing save.

## Quick facts

| | |
|---|---|
| Environment | Client **and** server |
| Config-driven | Yes — no code changes for balance |
| Launch-time toggle | `infusedmobs:enabled` gamerule (per world save) |
| Dimension toggle | `worldBlacklist` config list |
| Type toggle | `mobBlacklist` config list |
| Tested versions | MC 1.20.1 / 1.21.1 / 26.2, Fabric Loader 0.19.3+ |
