# Infused Mobs — Guide

**Infused Mobs** is a Fabric mod that turns ordinary hostile mobs into *Infused Mobs*: mobs that spawn with an occult **Tier** and a random set of **Abilities**. Every ability comes from one shared pool, so a Cinder zombie, a Shade skeleton and a Doom creeper can each surprise you differently.

This guide is the detailed reference. The one-screen pitch and download links live on the Modrinth and CurseForge project pages.

> **Reusing this guide:** you may copy guide text into modpack docs, wikis or personal notes with credit to *Infused Mobs* / **hunter1712**. The mod's code, assets and jar are **All Rights Reserved** — see [LICENSE](../../LICENSE).

## 60-second quickstart

1. Install **Fabric Loader 0.19.3+** for your game version.
2. Install **[Fabric API](https://modrinth.com/mod/fabric-api)** matching your game version.
3. Drop the jar matching your game version into your `mods/` folder.
4. Launch. The mod generates `config/infusedmobs.json` on first run.
5. Hostile mobs now spawn infused — green/yellow/red nametags mark their Tier.

Pick your version:

| Minecraft | Java | Jar |
|-----------|------|-----|
| 1.20.1 | 17+ | `infusedmobs-<mod>+1.20.1.jar` |
| 1.21.1 | 21+ | `infusedmobs-<mod>+1.21.1.jar` |
| 26.2 | 25+ | `infusedmobs-<mod>+26.2.jar` |

Full details in [installation.md](installation.md) and [compatibility.md](compatibility.md).

## Pages

| Page | What it covers |
|------|----------------|
| [Installation](installation.md) | Loader/API requirements, picking the right jar, client vs server, first run |
| [Tiers](tiers.md) | Cinder / Shade / Doom, spawn odds, stat multipliers, nametags |
| [Abilities](abilities.md) | HURT / TICK / DEATH tables, exact effects, shield rules, Rupture splitting |
| [Commands](commands.md) | Every `/infusedmobs` subcommand and the gamerule |
| [Configuration](configuration.md) | Every JSON field, defaults, valid ranges, clamping, gating lists |
| [Compatibility](compatibility.md) | Supported versions, dependencies, safe add/remove |
| [Version differences](version-differences.md) | What changes between 1.20.1, 1.21.1 and 26.2 |
| [Modpack authors](modpack-authors.md) | Licence terms, launch-time gating, balance defaults |
| [FAQ & troubleshooting](faq.md) | Common questions and known behaviour |

## Elsewhere in this repo

- [CHANGELOG.md](../../CHANGELOG.md) — full version history
- [LICENSE](../../LICENSE) — All Rights Reserved terms (incl. modpack clause)
- [CONTEXT.md](../../CONTEXT.md) — the project's domain vocabulary
- [docs/adr/](../adr/) — architecture decision records (developer-facing)
- [docs/smoke-checklist.md](../smoke-checklist.md) — manual in-game test checklist
