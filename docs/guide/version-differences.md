# Version differences

Infused Mobs targets two Minecraft generations from one shared code base, with a small version-specific **shim** per version where Mojang/Fabric APIs diverge. From a player's point of view the mod behaves identically on both — since 1.20.1 support ended with 2.10.0, the remaining versions report damage post-mitigation and both honour shield blocks, so there are **no player-visible differences** left.

## Requirements

| Requirement | 1.21.1 | 26.3 |
|-------------|--------|------|
| **Java** | 21+ | 25+ |
| **Fabric Loader** | 0.19.3+ | 0.19.3+ |
| **Built against Fabric API** | `0.116.6+1.21.1` | `0.162.0+26.3` |

## Same everywhere

- Tier chances, multipliers, ability effects, durations, Rupture splitting, Combust falloff, Bequest migration, nametags, commands and the gamerule.
- Single-roll spawn logic and full UUID persistence (mobs keep their roll across reloads).
- Config file name, fields, defaults, clamping and backfill.
- `infusedmobs:enabled` gamerule: default `true`, category Mobs, per-world save.
- **Shield blocks negate HURT abilities** on both versions, and the damage fed to Siphon and Thorns is post-mitigation on both.

## Internal differences (for the curious)

None of these change gameplay; they exist because the underlying APIs differ.

| Concern | 1.21.1 | 26.3 |
|---------|--------|------|
| Damage event | `AFTER_DAMAGE` (post-mitigation, reports blocked) | `AFTER_DAMAGE` (post-mitigation, reports blocked) |
| Gamerule registration | Fabric `GameRuleRegistry` | vanilla `BuiltInRegistries.GAME_RULE` + `GameRule` codec |
| Tier persistence | NBT `Factory`/function path | `SavedDataType` + `Codec` |
| Dimension ids | `ResourceLocation` | `Identifier` |
| Status effects | `Holder<MobEffect>` | `Holder<MobEffect>` |
| Spawn creation | `EntityType#create(level)` | `EntityType#create(level, EntitySpawnReason)` |
| Mixin compatibility level | `JAVA_21` | `JAVA_25` |

The per-version shims are documented as an accepted duplication — there is no preprocessor that would thin them further. The architecture decisions are recorded in [docs/adr/](../adr/) and the domain vocabulary in [CONTEXT.md](../../CONTEXT.md).
