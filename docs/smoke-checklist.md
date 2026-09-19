# Infused Mobs — In-Game Smoke Checklist (1.21.1)

Manual checklist, nice-to-have rather than a gate. Jar: `infusedmobs-2.7.1+1.21.1.jar` (from `1.21.1/build/libs/`).
Needs: fresh world, cheats on, op level 2. Report failures as: version + steps + what happened vs expected.

## Setup

- [ ] Install the jar, fresh world, run `/gamerule infusedmobs:enabled` -> reads true, no console errors on startup.

## Core loop

- [ ] Wild hostile mobs spawn with green (Cinder) / yellow (Shade) / red (Doom) nametags; roughly 30% stay vanilla at defaults (single-roll 40%/20%/10% shares).
- [ ] Bare `/infusedmobs` shows the help text (same as `/infusedmobs help`).
- [ ] `/infusedmobs summon doom zombie "bane thorns"` spawns a red-tagged zombie at your crosshair (abilities require an entity argument).
- [ ] Summon with a typo (e.g. "banne") prints an unknown-ID message with a "did you mean" hint and the valid ID list.
- [ ] Combat: an Infused Mob hitting you fires offensive HURT effects (poison, slowness, fire, weakness — melee or projectile, players only); hitting a Thorns mob reflects 15% back at you (reactive HURT, never passive); TICK effects refresh about every second (resistance, strength, speed shimmer, no particles).
- [ ] Kill a Rupture mob -> splits into 2 grey-tagged copies with full Cinder health and XP (no Tier) that never split again (other abilities on copies OK, including Combust).
- [ ] Kill a Combust mob -> nearby damage + explosion sound, NO block damage, NO particles.
- [ ] Infused kills grant multiplied XP (about 1.5x Cinder / 2x Shade / 4x Doom).

## Config + gating

- [ ] `/infusedmobs nametag off` hides ALL tier tags (abilities still fire); `/infusedmobs nametag on` restores them.
- [ ] `/infusedmobs world add minecraft:the_nether` -> nether mobs spawn vanilla and summon is refused there with a clear message.
- [ ] `/infusedmobs world list` shows the entry; `/infusedmobs world remove minecraft:the_nether` re-enables the mod there.
- [ ] `/infusedmobs mob add minecraft:spider` -> spiders spawn vanilla; `mob list` shows it; `mob remove minecraft:spider` re-enables natural infusion.
- [ ] `/gamerule infusedmobs:enabled false` disables the mod fully (vanilla spawns, summon refused); true re-enables. Blacklist + gamerule combine correctly.

## Persistence

- [ ] Note a few infused mobs (screenshot/F3), save-quit-reload -> same tiers and abilities, split copies stay split copies.
- [ ] Note a boosted infused mob's max health (e.g. Cinder zombie 30 HP), save-quit-reload -> same max health: not multiplied again (stacking) and not deflated to vanilla (#27).
- [ ] Fly far away and back (chunk unload/reload) -> mobs keep their tiers.
- [ ] Hand-edit `config/infusedmobs.json`, run `/infusedmobs reload` -> changes apply without restart, and the file is NOT rewritten with defaults (rewrite = invalid config, report it).

## Regression watch

- [ ] No console errors during startup, exploring new chunks (spawn bursts), mob death, or despawn.
- [ ] Animals/villagers/other non-monsters are NEVER infused.
- [ ] `/infusedmobs list` shows the hostile mob types; `/infusedmobs help` lists all subcommands.
