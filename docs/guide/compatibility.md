# Compatibility

## Supported versions

One project page ships all three versions. Download the jar whose `+<mc>` suffix matches your game version.

| Mod loader | Minecraft | Java | Fabric API (built against) | Jar |
|------------|-----------|------|----------------------------|-----|
| Fabric | 1.20.1 | 17+ | `0.92.0+1.20.1` | `infusedmobs-<mod>+1.20.1.jar` |
| Fabric | 1.21.1 | 21+ | `0.116.6+1.21.1` | `infusedmobs-<mod>+1.21.1.jar` |
| Fabric | 26.2 | 25+ | `0.155.2+26.2` | `infusedmobs-<mod>+26.2.jar` |

The Fabric API numbers are the versions the mod was **built and tested against**. The mod declares `fabric-api: "*"`, so any Fabric API build for your game version works — you don't need to match those exact numbers.

## Dependencies

| Dependency | Required? | Notes |
|------------|-----------|-------|
| **Fabric Loader** | Yes | `0.19.3` or newer |
| **Fabric API** | Yes | Any build matching your Minecraft version |
| Anything else | No | No hard dependencies |

## Client / server

| Side | Required | Why |
|------|----------|-----|
| Server | **Yes** | All logic — rolls, abilities, persistence, gating — runs server-side |
| Client | **Yes** (supported configuration) | The mod ships `environment: "*"` and is installed on both sides; tier nametags are sent as vanilla custom names |

## What it does **not** add

- **No custom status effects** — every ability uses a vanilla effect.
- **No blocks, items, entities or world generation.**
- **No custom GUI or keybinds.**

Because of this, the mod is **safe to add to or remove from an existing save**. When removed, mobs simply stop being tracked and vanilla behaviour resumes; the per-world file `data/infusedmobs_tiers.dat` is left behind harmless. Reinstalling later restores the tiers of mobs still alive in that save.

## Persistence and mixins

- Infused rolls are stored per world in `data/infusedmobs_tiers.dat`, keyed by mob UUID, and restored exactly on reload. Corrupted data degrades to vanilla instead of failing the world load.
- The mod registers two required mixins, `EntityRemoveMixin` (cleanup on despawn) and `LivingEntityMixin` (XP multiplier). They inject narrowly and cancel nothing, so conflicts with other mods are unlikely — but if you run a large pack and see a mixin failure, check for another mod touching entity removal or XP rewards.

## Version-specific behaviour

The three versions are **intentionally not byte-identical**. The most visible difference is shield-block handling for HURT abilities. See [version-differences.md](version-differences.md) for the full list.

## Licence

**All Rights Reserved** — see [LICENSE](../../LICENSE). Modpack inclusion with credit is permitted; redistribution and derivatives require permission. See [modpack-authors.md](modpack-authors.md) for how that applies to packs.
