# Version differences

Infused Mobs targets three Minecraft generations from one shared code base, with a small version-specific **shim** per version where Mojang/Fabric APIs diverge. From a player's point of view the mod behaves the same everywhere, with **one meaningful exception** (shield blocks) and a couple of numeric side effects.

## Player-visible differences

| Behaviour | 1.20.1 | 1.21.1 | 26.2 |
|-----------|--------|--------|------|
| **Shield blocks negate HURT abilities** | ❌ No | ✅ Yes | ✅ Yes |
| **Damage feeding Siphon / Thorns** | pre-mitigation | post-mitigation | post-mitigation |
| Requirement: Java | 17+ | 21+ | 25+ |
| Requirement: Fabric Loader | 0.19.3+ | 0.19.3+ | 0.19.3+ |
| Built against Fabric API | 0.92.0+1.20.1 | 0.116.6+1.21.1 | 0.155.2+26.2 |

### Shield blocks (the one to know)

On **1.21.1 and 26.2**, a successful shield block reports as a blocked hit, and Infused Mobs skip **all** HURT abilities for it — no poison, slowness, wither, weakness, fire, Siphon heal or Vitriol armour damage. Thorns also does not reflect.

On **1.20.1**, Minecraft's legacy damage event reports the hit *before* mitigation and does not carry a shield-block flag. A fully-blocked hit therefore **still fires HURT abilities**. This is an accepted platform limitation: exact parity would require a post-mitigation damage event that 1.20.1 does not expose.

### Damage amounts

Because 1.20.1 reports **pre-mitigation** damage, the amount passed to **Siphon** (heal) and **Thorns** (reflect) is the raw hit rather than the reduced amount actually taken. On 1.20.1 those two abilities can therefore look stronger than on the newer versions. Everything else is identical.

## Same everywhere

- Tier chances, multipliers, ability effects, durations, Rupture splitting, Combust falloff, nametags, commands and the gamerule.
- Single-roll spawn logic and full UUID persistence (mobs keep their roll across reloads).
- Config file name, fields, defaults, clamping and backfill.
- `infusedmobs:enabled` gamerule: default `true`, category Mobs, per-world save.

## Internal differences (for the curious)

None of these change gameplay; they exist because the underlying APIs differ.

| Concern | 1.20.1 / 1.21.1 | 26.2 |
|---------|------------------|------|
| Damage event | `ALLOW_DAMAGE` (1.20.1, legacy) / `AFTER_DAMAGE` (1.21.1) | `AFTER_DAMAGE` (post-mitigation, reports blocked) |
| Gamerule registration | Fabric `GameRuleRegistry` | vanilla `BuiltInRegistries.GAME_RULE` + `GameRule` codec |
| Tier persistence | NBT `Factory`/function path | `SavedDataType` + `Codec` |
| Dimension ids | `ResourceLocation` | `Identifier` |
| Status effects | raw `MobEffect` handles | `Holder<MobEffect>` |
| Spawn creation | `EntityType#create(level)` | `EntityType#create(level, EntitySpawnReason)` |
| Mixin compatibility level | `JAVA_17` (1.20.1) / `JAVA_21` (1.21.1) | `JAVA_25` |

The per-version shims are documented as an accepted duplication — there is no preprocessor that would thin them further. The architecture decisions are recorded in [docs/adr/](../adr/) and the domain vocabulary in [CONTEXT.md](../../CONTEXT.md).
