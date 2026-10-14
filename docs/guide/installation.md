# Installation

## Requirements

| Requirement | Version |
|-------------|---------|
| **Fabric Loader** | 0.19.3 or newer |
| **Fabric API** | Any build matching your Minecraft version |
| **Java** | 17+ on 1.20.x · 21+ on 1.21.x · 25+ on 26.x |
| **Side** | Install on **both client and server** |

You must install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api) first. Infused Mobs has no other dependencies and adds no custom blocks, items, effects or world generation.

> The mod builds against Fabric API `0.92.0+1.20.1`, `0.116.6+1.21.1` and `0.162.0+26.3`, but declares `fabric-api: "*"` — any Fabric API build for your game version works, and you do **not** need to match those exact numbers.

## Steps

1. **Install Fabric Loader 0.19.3+** for your Minecraft version.
2. **Install Fabric API** for that same Minecraft version.
3. **Download the jar for your game version** from the single project page:

   | Minecraft | Download |
   |-----------|----------|
   | 1.20.1–1.20.6 | `infusedmobs-2.9.0+1.20.x.jar` |
   | 1.21.1–1.21.11 | `infusedmobs-2.9.0+1.21.x.jar` |
   | 26.1–26.3+ | `infusedmobs-2.9.0+26.x.jar` |

   One jar per generation covers its whole range — on Modrinth tick every game version in the range. Downloading the wrong generation's jar (1.20 vs 1.21 vs 26.x) is the most common install mistake.

4. **Drop the jar into your `mods/` folder.**
   - Singleplayer: `.minecraft/mods/`
   - Server: the server's `mods/` folder
5. **Launch the game.** Infused Mobs writes `config/infusedmobs.json` on first run and logs `InfusedMobs initializing...` to the console.

## Client vs server

The mod is required on the **server** — all tier rolling, abilities, persistence and gating run server-side. Install the same jar on the **client** as well for the supported setup (the project declares `environment: "*"` and ships on both sides).

Infused mobs' tier nametags are set as vanilla custom names, so they are sent to all nearby players.

## First run

- A config file is generated at `config/infusedmobs.json` in your game/server directory.
- The gamerule `infusedmobs:enabled` defaults to `true` in every world.
- Existing worlds pick up the mod immediately on next load — no world reset required.

## Updating

Replace the old jar with the new one for your version. Your config is preserved: fields added in newer versions are **backfilled from defaults**, and out-of-range values are clamped, so tier/effect settings you edited survive upgrades. See [configuration.md](configuration.md#validation-clamping-and-backfill).

## Uninstalling

Remove the jar. Any infused mobs simply stop being tracked; vanilla behaviour resumes. The per-world file `data/infusedmobs_tiers.dat` is left behind (it is harmless and unread without the mod) and can be deleted to reclaim the space.

## Troubleshooting install problems

See [faq.md](faq.md). If the game reports a missing dependency, the usual cause is a missing/old **Fabric API**, or the **wrong `+<mc>` jar** for your game version.
