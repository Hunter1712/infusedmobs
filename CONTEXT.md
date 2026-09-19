# Infused Mobs

A Fabric mod that gives vanilla hostile mobs occult-infused tiers with randomized abilities, configurable via JSON and gated per dimension by world blacklist and per-save gamerule.

## Language

### Infusion

**Infused Mob**:
A hostile Mob that was assigned a Tier and one or more Abilities at spawn.
_Avoid_: Tiered Mob, Infused Entity

**Tier**:
A power level — Cinder (40%, 1 ability, 1.5× HP/XP, green), Shade (20%, 2, 2×, yellow) or Doom (10%, 3, 4×, red) — that sets spawn chance, ability count and multipliers.
_Avoid_: Level, Rank, Difficulty

**Ability**:
A named power such as Bane or Thorns that an Infused Mob can use. Rupture spawning two Cinder-stat copies is part of the Ability contract.
_Avoid_: Effect, Skill, Power

**TriggerType**:
The moment an Ability fires: HURT on damage involving an Infused Mob and a player (offensive melee plus mob-owned projectiles, or reactive Thorns when the Infused Mob is damaged), TICK passively each second, or DEATH on death.
_Avoid_: Event Type, Ability Type, Trigger

### Gating

**World Blacklist**:
A config list of dimension ids (e.g. `minecraft:overworld`) where infusion is disabled for that dimension.
_Avoid_: Dimension Blacklist, World Ban, Blacklisted World

**Mob Blacklist**:
A config list of entity type ids (e.g. `minecraft:spider`) excluded from natural Tier rolls; the summon command bypasses it as explicit operator intent.
_Avoid_: Entity Blacklist, Mob Ban

**Gamerule Gate**:
A per-save boolean gamerule `infusedmobs:enabled` combined per dimension with the World Blacklist; infusion is active in a dimension only if that dimension is not blacklisted and the gamerule is true.
_Avoid_: Enabled Flag, Global Toggle

### Versioning

**Versioned Source Set**:
A per-Minecraft-version Gradle subproject (`1.20.1`, `1.21.1`, `26.2`) plus `common` that holds shared code; each version compiles `common/src/main/java` plus its own shims and `fabric.mod.json`.
_Avoid_: Version overlay, Chiseled source set

**Shim**:
A small file duplicated per version where Mojang/Fabric APIs diverge (e.g. `gamerules/ModGameRules.java`, `tier/TierSavedData.java`, `infusedmobs.mixins.json` with `JAVA_17`/`JAVA_21`/`JAVA_25`).
_Avoid_: Version-specific file, Compat layer

**Artifact Suffix**:
The `+mc` qualifier appended to `mod_version` for published jars (`2.7.1+1.20.1`, `2.7.1+1.21.1`, `2.7.1+26.2`) so Modrinth channels stay distinct.
_Avoid_: Version suffix, Build suffix
