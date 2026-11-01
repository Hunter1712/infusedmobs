# Repo audit: infusedmobs

## What this repo does

Infused Mobs is a Fabric mod for Minecraft 1.21.1 and 26.3. Hostile mobs spawn with a random occult tier (Cinder, Shade, Doom, Void) and random abilities from a shared pool, configured through `config/infusedmobs.json`. Rolls are persisted per world so mobs keep their tier and abilities across chunk reloads and restarts; commands (`/infusedmobs`) and the `infusedmobs:enabled` gamerule gate where the mod runs.

## Load assumed

One Minecraft process at a time — a client or a single dedicated server with a few players, tens of hostile mobs loaded at once. The config file is written only at startup and by operator commands. There is no network input, no money, no auth, and no shared state between processes.

## Must fix

None.

## Should fix

1. **Config disk paths are defensive but untested** (`common/src/main/java/io/github/hunter1712/infusedmobs/config/ModConfig.java`, `load` and `save`)
   - **What this is:** `load()` reads `config/infusedmobs.json` through FabricLoader; malformed JSON is moved to `.bak` and replaced with defaults. `save()` rewrites the file atomically (temp file + `ATOMIC_MOVE`) and logs failures at error level.
   - **Problem:** The test suite runs without Minecraft or FabricLoader, so neither the corrupt-file rescue path nor the atomic save is exercised by a test. A future refactor could reintroduce a truncated write or silent disk failure; the first sign would be a user's missing config, visible only in `latest.log`.
   - **Fix:** Add disk-backed tests for `load`/`save` once a FabricLoader test harness exists; keep the atomic move, the `.bak` backup, and error-level logging.
   - **If we skip it:** Config corruption or loss is detected by users, never by CI.

2. **Tiered mob health may multiply on every world restart** (`common/src/main/java/io/github/hunter1712/infusedmobs/tier/MobTierManager.java`, `trackTiered`; `tier/RestartRestoreTest.java`)
   - **What this is:** on chunk/world reload a mob's persisted roll is restored, and restore re-applies the tier health multiplier to the mob's max health.
   - **Problem:** `RestartRestoreTest` assumes vanilla never persists modified attribute bases, so each restore multiplies a fresh vanilla base exactly once. If 1.21.1 or 26.3 persist attribute bases to the world save (the `Attributes` NBT tag), a Cinder mob's max health is multiplied by 1.5 on every restart — 2.25× after two restarts, compounding.
   - **Concrete check:** spawn a Cinder, note its max health, restart the world, confirm max health is unchanged.
   - **Fix:** Verify on a real server; if health creeps, make the restore apply the boost only once.
   - **If we skip it:** stat creep on every restart, compounding across restarts.
   - **Note:** could not be verified here without launching Minecraft.

## Nice to have

3. **Two near-identical roll-tracking methods** (`MobTierManager.trackTiered` and `trackSplit`)
   - **What this is:** both store the roll in the shared registry, apply a health boost, and set the nametag; only the multiplier, health fraction and nametag source differ.
   - **Fix:** Merge into one private `track` method. About -15 lines, no behaviour change.
   - **If we skip it:** two sites to keep in sync when health or nametag logic changes.

## Verdict

Healthy. No must-fix findings; verify finding 2 on a live server and add the disk tests in finding 1 when a test harness exists.

## Lean

- About -15 lines possible (finding 3), 0 dependencies removable.

## Not checked

- In-game behaviour on 1.21.1 and 26.3 (no Minecraft instance available to launch).
- Full remapped jar builds for both subprojects (`:1.21.1:build`, `:26.3:build`).
- Whether Minecraft persists `LivingEntity` attribute bases to NBT — the assumption behind finding 2.