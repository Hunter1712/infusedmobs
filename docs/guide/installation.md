# Installation

## Requirements

| Requirement | Version |
|-------------|---------|
| **Fabric Loader** | 0.19.3 or newer |
| **Fabric API** | Any build matching your Minecraft version |
| **Java** | 21+ on 1.21.1 · 25+ on 26.3 |
| **Side** | Install on **both client and server** |

You must install [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/mod/fabric-api) first. Infused Mobs has no other dependencies and adds no custom blocks, items, effects or world generation.

> The mod builds against Fabric API `0.116.6+1.21.1` and `0.162.0+26.3`, but declares `fabric-api: "*"` — any Fabric API build for your game version works, and you do **not** need to match those exact numbers.

## Steps

1. **Install Fabric Loader 0.19.3+** for your Minecraft version.
2. **Install Fabric API** for that same Minecraft version.
3. **Download the jar for your game version** from the single project page:

   | Minecraft | Download |
   |-----------|----------|
   | 1.21.1 | `infusedmobs-2.10.0+1.21.1.jar` |
   | 26.3 | `infusedmobs-2.10.0+26.3.jar` |

   Each jar loads **only** on the exact Minecraft version it was built and smoke-tested against. Downloading the wrong jar is the most common install mistake.

   > **On an untested minor?** If your game is on 1.21.11, 26.1 or 26.2, stay on **2.9.2** — 2.10.0 will refuse to load there.

   > **Still on 1.20.1?** Support ended with this release — 2.9.2 is the last jar for 1.20.1.

4. **Drop the jar into your `mods/` folder.**
   - Singleplayer: `.minecraft/mods/`
   - Server: the server's `mods/` folder
5. **Launch the game.** Infused Mobs writes `config/infusedmobs.json` on first run and logs `InfusedMobs initializing...` to the console.

> New in 2.10.0: a fourth tier (**Void**) joins Cinder, Shade and Doom, and tier nametags are **off by default** — they never appeared for players before, and existing configs keep their current `showNametags` value. Old configs gain the `void` tier block automatically on first load (see [configuration.md](configuration.md#validation-clamping-and-backfill)).

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
