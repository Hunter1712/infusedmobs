# FAQ & troubleshooting

## Install / launch

**Do I need Fabric API?**
Yes. Install Fabric API for your game version alongside Fabric Loader 0.19.3+.

**The game says I'm missing a dependency.**
Usually a missing or outdated **Fabric API**, or the **wrong jar** for your game version. The `+<mc>` suffix must match exactly — `+1.21.1` will not load on 1.20.1. See [installation.md](installation.md).

**Does it work with vanilla clients?**
The supported setup installs the mod on **both client and server**. All logic is server-side, and tier nametags are vanilla custom names, but install the same jar on both sides to match the tested configuration. See [compatibility.md](compatibility.md#client--server).

**Will it break my existing world?**
No. Add or remove it freely; it adds no blocks, items, effects or worldgen. Removing it leaves a harmless `data/infusedmobs_tiers.dat` in the world folder. See [compatibility.md](compatibility.md).

## Gameplay

**Why do some mobs still spawn vanilla?**
By design. At default settings the three tier chances sum to 70%, so about **30% of hostile mobs stay vanilla**. That's the single-roll model, not a bug — see [tiers.md](tiers.md#how-the-roll-works).

**Why isn't my cow/villager infused?**
Only **hostile (`MONSTER` category)** mobs roll naturally. Animals, villagers and other non-hostiles never do. `/infusedmobs summon` can bypass this for testing.

**A blocked hit still triggered poison/slowness.**
On **1.20.1** shields don't negate HURT abilities due to a platform limitation. On 1.21.1/26.2 a shield block does negate them. See [version-differences.md](version-differences.md#shield-blocks-the-one-to-know).

**Why did the Rupture copy have a grey nametag and not split again?**
By design. Split copies carry Cinder stats but no Tier; their single ability is drawn with `rupture` excluded so they can never split infinitely. See [abilities.md](abilities.md#rupture).

**Combust didn't break any blocks.**
Correct. Combust is a damage-and-sound effect only — no block damage and no particles. Raise `combustExplosionPower` for a bigger radius, not for terrain damage.

**I can't tell which abilities a mob has.**
They're intentionally low-key: TICK buffs show no particles, and HURT abilities only reveal themselves when they hit you. Use nametags to see the ability names, or `/infusedmobs nametag on`.

**Do mobs keep their tier across restart?**
Yes — rolls are persisted by UUID and restored exactly, with no re-roll and no stat stacking. If a mob clearly loses or gains a tier across a restart, please report it (version + steps + expected vs actual).

## Config

**I edited the config but nothing changed.**
Run `/infusedmobs reload`. Ability values are read when they fire, so changes apply to the next hit/tick; spawn chances apply to future rolls. Already-rolled mobs keep their tier.

**My config file got rewritten.**
That happens when it's **malformed JSON**, **missing fields**, or has **out-of-range values** — the mod repairs and saves it. See [configuration.md](configuration.md#validation-clamping-and-backfill). Check the log for the exact field warnings.

**How do I disable infusion entirely?**
Any of:
- `/gamerule infusedmobs:enabled false` (per world save)
- add the dimension to `worldBlacklist`
- set all three `spawnChance` values to `0` (though the gamerule/blacklist is cleaner)

**How do I keep only certain mobs vanilla?**
Add them to `mobBlacklist`, via config or `/infusedmobs mob add <entity>`. Note this affects **natural** rolls only; `/infusedmobs summon` still works. See [configuration.md](configuration.md#mob-blacklist).

**Do modded mobs work?**
Yes — any entity type in the hostile category can roll, and you can blacklist modded ids (`namespace:path`) like any other. `/infusedmobs summon` accepts modded mobs too.

## Performance & development

**Does it cost performance?**
It's light. Only mobs that actually rolled a **TICK** ability are visited each second; everything else is event-driven. Tracking is cleaned up on death/despawn.

**Can I add my own abilities or effects?**
No. The ability pool is fixed and uses vanilla effects only. The source is public for reference and transparency under an All Rights Reserved licence — see [LICENSE](../../LICENSE) and [modpack-authors.md](modpack-authors.md).

**How do I report a bug?**
Open an issue on the [GitHub issue tracker](https://github.com/hunter1712/infusedmobs/issues) with: mod version, Minecraft version, steps to reproduce, and what happened vs what you expected. The [smoke checklist](../smoke-checklist.md) is a good template for a full manual pass.

## Known behaviour (not bugs)

- Rupture copies are grey, carry Cinder stats, and never re-split.
- Nametags use vanilla colour codes (`§a`, `§e`, `§c`, `§7`), so they're visible without any client mod.
- Older config files are backfilled on load, preserving your tier/effect settings.
- Corrupted tier data in a save degrades that mob to vanilla instead of failing the world load.
