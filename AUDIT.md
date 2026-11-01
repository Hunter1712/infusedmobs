# Repo audit: infusedmobs

## What this repo does

Infused Mobs is a Fabric mod for Minecraft 1.21.1 and 26.3. Hostile mobs spawn with a random occult tier (Cinder, Shade, Doom, Void) and random abilities from a pool, all configurable through `config/infusedmobs.json`. Rolls are persisted per world so mobs keep their tier and abilities across chunk reloads and restarts; commands and the `infusedmobs:enabled` gamerule gate where the mod runs.

Load assumed: one Minecraft process at a time — a client or a single server with a few players, tens of hostile mobs loaded at once. The config file is written only at startup and by operator commands; there is no network input, no money, and no auth.

## Must fix

1. **Config save can destroy the user's config** (`common/src/main/java/io/github/hunter1712/infusedmobs/config/ModConfig.java:L116-125`, `L56-71`)
   - **What this is:** `save()` writes the new JSON straight over `config/infusedmobs.json` with `Files.writeString`; `load()` replaces any unreadable or malformed file with defaults.
   - **Problem:** A crash or kill mid-write (from `/infusedmobs nametag on`, `world add`, etc.) leaves a truncated file. The next start treats it as malformed, logs a warning, and overwrites it with defaults — all user tier edits, blacklists and the nametag preference are gone with no backup. The same fate awaits a hand-edited file with a typo.
   - **Fix:** Write to a temp file in the same folder and move it atomically (`Files.move` with `REPLACE_EXISTING` and `ATOMIC_MOVE`). On a malformed file, keep the original (rename it to `.bak`) instead of destroying it.
   - **If we skip it:** A crash during a config write silently loses all user config.

2. **`CONTEXT.md` documents the wrong tier balance** (`CONTEXT.md:L14`)
   - **What this is:** `CONTEXT.md` is the repo's domain glossary — the single source of truth for terms such as Tier.
   - **Problem:** Its Tier paragraph says Cinder 40% / 1.5×, Shade 20% / 2×, Doom 10% / 4×, Void 5% / 6×. The code (`MobTier.java:L16-19`) and `tiers.md`, `README.md`, `configuration.md` say 20% / 1.0×, 10% / 2.0×, 5% / 3.0×, 2.5% / 4.0×.
   - **Fix:** Update the Tier paragraph and numbers in `CONTEXT.md` to match `MobTier`.
   - **If we skip it:** Anyone reading the domain doc builds or advises on the wrong balance.

## Should fix

3. **Bequest ignores the per-tier ability limit** (`common/.../tier/MobTierManager.java:L327-357`, `common/.../ability/effect/BequestEffect.java:L54`)
   - **What this is:** When an Infused Mob dies, Bequest migrates its abilities to the nearest living Tiered allies. Natural draws cap each tier at its configured `abilityCount` (Cinder = 1).
   - **Problem:** `planBequest` and `grantAbility` cap at the whole ability pool (15), not at the heir's tier limit. A Cinder can collect bane, thorns, chill… across several nearby deaths and end with far more abilities than its tier allows — every one still firing.
   - **Fix:** Cap grants at `config.forTier(heir's tier).abilityCount()` instead of the pool size, and pass that per-heir cap through `planBequest`.
   - **If we skip it:** The ability-count balance rule is breakable through normal play, and the same rule lives in two places that disagree.

4. **Config disk failures are silent and untested** (`ModConfig.java:L125`; `:common:test`)
   - **What this is:** `load()` and `save()` touch disk (`config/infusedmobs.json`); `save()` reports failures with `LOGGER.warn`, and the test suite explicitly does not load from disk.
   - **Problem:** A read-only config folder, a missing directory, or a full disk makes config changes via `/infusedmobs reload`, `nametag` or `world` silently fail — operators only see it in `latest.log` if they look. No test covers these paths.
   - **Fix:** Log save/load failures at `error` so they cannot be missed; add disk-path tests if a FabricLoader test harness is added later.
   - **If we skip it:** Config edits appear to succeed while nothing is persisted, found only by chance.

## Nice to have

5. **Two roll maps kept in sync by caller discipline** (`MobTierManager.java`, `TierRollStore.java`, `TierSavedData.java`)
   - **What this is:** `MobTierManager.SHARED` (live memory) and each world's `TierSavedData.store` (disk) are separate `UUID -> Rolled` maps. Every write must hit both (`put` then `storeRoll`; `removeMob` clears both).
   - **Problem:** It works today, but a future caller that updates one map without the other diverges live state from persisted state — mobs re-roll or keep old tiers after reload.
   - **Fix:** Funnel persistence through one method (for example `MobTierManager.persist(uuid, rolled)`) so callers cannot forget one side.
   - **If we skip it:** A missed sync call is a silent, hard-to-reproduce bug.

## Verdict

Needs work — fix 1 and 2 first; 1 risks silent data loss and 2 corrupts the domain doc the whole team reads.

## Not checked

- In-game behaviour on 1.21.1 and 26.3 (no Minecraft instance available to launch).
- Full remapped jar builds (`:1.21.1:build`, `:26.3:build` jars were not produced here; `:common:test` — 262 tests — and all `compileJava` tasks passed).
- Offline resolution of `loom_version=1.17-SNAPSHOT`.
