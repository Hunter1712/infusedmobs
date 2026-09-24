# Abilities

An **Ability** is a named power an Infused Mob can use. Every ability lives in one shared pool, and a mob draws its abilities from that pool based only on its Tier's ability count — there are no per-tier restricted lists. A Cinder mob might get a poison hit, a passive speed buff, or a death explosion; a Doom mob draws three from the same pool.

There are **14 abilities** across three **TriggerTypes**:

| Trigger | Fires when… |
|---------|-------------|
| **HURT** | damage is exchanged between an Infused Mob and a player |
| **TICK** | passively, every second, while the mob is alive |
| **DEATH** | the mob dies |

## HURT

HURT covers two directions in one event:

- **Offensive** abilities fire when an **Infused Mob damages a player** — a melee hit or a projectile the mob fired (arrow, fire charge, trident, etc.).
- **Reactive** Thorns fires when an **Infused Mob is damaged by a player**.

Rules that apply to all HURT abilities:

- **Players only.** Mob-vs-mob hits never trigger abilities.
- **A shield block negates every HURT ability** on 1.21.1 and 26.2. On 1.20.1 the legacy damage event doesn't report shield blocks, so a fully-blocked hit still fires them — see [version-differences.md](version-differences.md).
- **Thorns reflection never re-triggers abilities** — the reflected damage uses a distinct damage type, so two Thorns mobs can't ping-pong forever.
- Effect-based abilities use the configured HURT duration and amplifier (see [configuration.md](configuration.md)).

| ID | Ability | Effect |
|----|---------|--------|
| `bane` | **Bane** | Poison I for the configured HURT duration (3s default) |
| `chill` | **Chill** | Slowness I for the configured HURT duration |
| `decay` | **Decay** | Wither I for the configured HURT duration |
| `hex` | **Hex** | Weakness I for the configured HURT duration |
| `hellfire` | **Hellfire** | Sets the target on fire for `infernoFireSeconds` (5s default) |
| `siphon` | **Siphon** | The mob heals for the damage it dealt |
| `vitriol` | **Vitriol** | Deals `acidArmorDamage` durability (4 default) to every worn armour piece |
| `thorns` | **Thorns** | Reflects **15%** of incoming damage back at the attacking player |

Notes:

- **Hellfire's config field is named `infernoFireSeconds`** for historical reasons; it is the fire duration, not a separate ability.
- **Siphon** heals the *mob* (not the player) by the damage dealt, and only when the damage is greater than zero.
- **Vitriol** only affects players, and damages all four armour slots by the same amount.
- **Thorns** works on any incoming player damage, including projectiles; the reflected portion is 15% of the damage taken and is never itself an ability trigger.

## TICK

TICK abilities apply a **buff to the mob itself**, refreshing every second. They have no particles, so you'll only notice them in the mob's behaviour.

| ID | Ability | Effect |
|----|---------|--------|
| `ward` | **Ward** | Resistance I |
| `frenzy` | **Frenzy** | Strength I |
| `wraith` | **Wraith** | Speed I |
| `blight` | **Blight** | Regeneration I |

Duration and amplifier are configurable (`tickEffectDuration`, `tickEffectAmplifier`); the 3-second default means the buff is always active as long as the mob lives. Only mobs that actually rolled a TICK ability are processed each second.

## DEATH

| ID | Ability | Effect |
|----|---------|--------|
| `rupture` | **Rupture** | Splits into 2 copies on death |
| `combust` | **Combust** | Explodes on death |

### Rupture

When a mob with Rupture dies it spawns **2 copies of itself**:

- Each copy has **Cinder stats** (1.5× base health) but starts at **60% of that boosted max health**.
- Each copy gets **1 random ability**, drawn with `rupture` excluded — so a copy can never split again. Any other ability, including **Combust**, is possible.
- Copies show a **grey nametag** with no Tier colour, and grant the **Cinder XP multiplier**.
- Their split-copy status is persisted **before** they enter the world, so chunk reloads and restarts keep them as copies — they can never be re-rolled into a normal Infused Mob.

### Combust

When a mob with Combust dies it detonates around itself:

- **Radius** = `combustExplosionPower` × 2 blocks (8 blocks at the default power of `4.0`).
- **Damage** starts at 4.0 at point-blank and falls off **linearly to a minimum of 1.0** at the rim of the radius.
- It damages **any nearby living entity** except the dying mob itself — this can include the player who killed it and other mobs.
- It plays an **explosion sound** but causes **no particles and no block damage**. It is not a real explosion.

## The ability pool

| | |
|---|---|
| Total abilities | 14 (8 HURT, 4 TICK, 2 DEATH) |
| Draw | Random, **no duplicates within a mob**, from the whole pool |
| Count | Set by the tier's `abilityCount` (1 / 2 / 3 by default) |
| Cap | The configured count is clamped to the live pool size (14) |

Because the pool mixes all three trigger types, a mob's abilities can be a mix — for example a Doom mob could have one HURT ability, one TICK buff and Combust. A Rupture copy draws from the same pool but always with `rupture` removed, and its single draw is filtered the same way.

The ability **IDs** above are what you type for `/infusedmobs summon`. See [commands.md](commands.md#infusedmobs-summon) for syntax and tab-completion.
