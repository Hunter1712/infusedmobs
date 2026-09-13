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
A named power such as Bane or Thorns that an Infused Mob can use.
_Avoid_: Effect, Skill, Power

**TriggerType**:
The moment an Ability fires: HURT on melee hit, TICK passively each second, or DEATH on death.
_Avoid_: Event Type, Ability Type, Trigger

### Gating

**World Blacklist**:
A config list of dimension ids (e.g. `minecraft:overworld`) where infusion is disabled for that dimension.
_Avoid_: Dimension Blacklist, World Ban, Blacklisted World

**Gamerule Gate**:
A per-save boolean gamerule `infusedmobs:enabled` combined per dimension with the World Blacklist; infusion is active in a dimension only if that dimension is not blacklisted and the gamerule is true.
_Avoid_: Enabled Flag, Global Toggle
