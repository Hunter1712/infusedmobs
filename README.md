# Infused Mobs

A Fabric mod for Minecraft **1.20.1 / 1.21.1 / 26.x (26.1–26.3+)** that gives vanilla hostile mobs **occult-infused tiers** with randomized abilities — fully configurable via JSON.

One project page covers all three generations. Download the jar whose `+<mc>` suffix matches your generation:

| Minecraft | Java | Jar |
|-----------|------|-----|
| 1.20.1–1.20.6 | 17+ | `infusedmobs-2.8.0+1.20.x.jar` |
| 1.21.1–1.21.11 | 21+ | `infusedmobs-2.8.0+1.21.x.jar` |
| 26.1–26.3+ | 25+ | `infusedmobs-2.8.0+26.x.jar` |

## Quick install

1. Install **Fabric Loader 0.19.3+**.
2. Install **[Fabric API](https://modrinth.com/mod/fabric-api)** for your game version.
3. Drop the matching jar in your `mods/` folder.
4. Launch — `config/infusedmobs.json` generates on first run.

## Tiers

- **Cinder** — 40% · 1 ability · 1.5× HP/XP · green
- **Shade** — 20% · 2 abilities · 2× HP/XP · yellow
- **Doom** — 10% · 3 abilities · 4× HP/XP · red

Tiers roll as a single uniform decision, so the effective shares equal the configured chances and about **30% of hostile mobs stay vanilla** at defaults.

## Documentation

📖 **[Read the full guide →](docs/guide/README.md)**

- [Installation](docs/guide/installation.md) · [Tiers](docs/guide/tiers.md) · [Abilities](docs/guide/abilities.md)
- [Commands](docs/guide/commands.md) · [Configuration](docs/guide/configuration.md) · [Compatibility](docs/guide/compatibility.md)
- [Version differences](docs/guide/version-differences.md) · [Modpack authors](docs/guide/modpack-authors.md) · [FAQ](docs/guide/faq.md)

## Building from source

```bash
./gradlew build
```

Jars land in `<version>/build/libs/` (e.g. `1.20.x/build/libs/infusedmobs-2.8.0+1.20.x.jar`). Use the JDK for the target version: 17 / 21 / 25. Build with system Gradle (`gradle build`); the wrapper is kept for contributors without a local install.

## Licence

**All Rights Reserved** — see [LICENSE](LICENSE). Modpack inclusion with credit is permitted; redistribution and derivatives require permission. See the [modpack author guide](docs/guide/modpack-authors.md).

Full version history: [CHANGELOG.md](CHANGELOG.md).
