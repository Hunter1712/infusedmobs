# Infused Mobs — In-Game Smoke Checklist

Release gate: run once per supported version. Jars: `infusedmobs-2.10.0+1.21.1.jar` (`1.21.1/build/libs/`) and `infusedmobs-2.10.0+26.3.jar` (`26.3/build/libs/`).
Needs: fresh world, cheats on, op level 2. Report failures as: version + steps + what happened vs expected.

## Smoke test record

| Version | Date | Result |
|---------|------|--------|
| 1.21.1 | — | pending |
| 26.3 | — | pending |

## Setup

- [ ] Install the jar, fresh world, run `/gamerule infusedmobs:enabled` -> reads true, no console errors on startup.

## Core loop

- [ ] Wild hostile mobs spawn infused with NO nametags by default; roughly 62.5% stay vanilla at defaults (single-roll 20%/10%/5%/2.5% shares).
- [ ] `/infusedmobs nametag on` shows green (Cinder) / yellow (Shade) / red (Doom) / purple (Void) tags; `/infusedmobs summon void zombie ""` (no abilities) or any Void spawn shows purple.
- [ ] Bare `/infusedmobs` shows the help text (same as `/infusedmobs help`).
- [ ] `/infusedmobs summon doom zombie "bane thorns"` spawns a red-tagged zombie at your crosshair (abilities require an entity argument).
- [ ] Summon with a typo (e.g. "banne") prints an unknown-ID message with a "did you mean" hint and the valid ID list.
- [ ] Combat: an Infused Mob hitting you fires offensive HURT effects (poison, slowness, fire, weakness — melee or projectile, players only); hitting a Thorns mob reflects 15% back at you (reactive HURT, never passive); TICK effects refresh about every second (resistance, strength, speed shimmer, no particles).
- [ ] Kill a Rupture mob -> splits into 2 grey-tagged copies at 60% of vanilla max health and vanilla XP (no Tier) that never split again (other abilities on copies OK, including Combust).
- [ ] Kill a Combust mob -> nearby damage + explosion sound, NO block damage, NO particles.
- [ ] Kill a Bequest mob with living infused allies nearby -> each nearest ally gains one of the dead mob's other abilities (duplicates slide, copies excluded, nametag refreshes); sole-Bequest death fizzles.
- [ ] Infused kills grant multiplied XP (1x Cinder / 2x Shade / 3x Doom / 4x Void).

## Config + gating

- [ ] `/infusedmobs nametag off` hides ALL tier tags (abilities still fire); `/infusedmobs nametag on` restores them.
- [ ] `/infusedmobs world add minecraft:the_nether` -> nether mobs spawn vanilla and summon is refused there with a clear message.
- [ ] `/infusedmobs world list` shows the entry; `/infusedmobs world remove minecraft:the_nether` re-enables the mod there.
- [ ] `/infusedmobs world add minecraft:*` -> mobs spawn vanilla everywhere and summon is refused (wildcard match); `world remove minecraft:*` restores.
- [ ] `/infusedmobs world add not an id` -> clean invalid-id error, blacklist unchanged.
- [ ] `/infusedmobs mob add minecraft:spider` -> spiders spawn vanilla; `mob list` shows it; `mob remove minecraft:spider` re-enables natural infusion.
- [ ] `/gamerule infusedmobs:enabled false` disables the mod fully (vanilla spawns, summon refused); true re-enables. Blacklist + gamerule combine correctly.

## Persistence

- [ ] Note a few infused mobs (screenshot/F3), save-quit-reload -> same tiers and abilities, split copies stay split copies.
- [ ] Note a boosted infused mob's max health (e.g. Shade zombie 40 HP), save-quit-reload -> same max health: not multiplied again (stacking) and not deflated to vanilla (#27).
- [ ] Fly far away and back (chunk unload/reload) -> mobs keep their tiers.
- [ ] Hand-edit `config/infusedmobs.json`, run `/infusedmobs reload` -> changes apply without restart, and the file is NOT rewritten with defaults (rewrite = invalid config, report it).
- [ ] Carry a 2.9.x-era config (no `void` block, 40% Cinder) into 2.10.0 -> on first load the `void` block appears with the 2.5%/4/4.0/4.0 defaults and existing tier edits (including the old Cinder numbers) are preserved.

## Regression watch

- [ ] No console errors during startup, exploring new chunks (spawn bursts), mob death, or despawn.
- [ ] Animals/villagers/other non-monsters are NEVER infused.
- [ ] `/infusedmobs list` shows the hostile mob types; `/infusedmobs help` lists all subcommands.
