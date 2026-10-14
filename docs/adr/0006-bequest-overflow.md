# Bequest heirs may overflow their Tier ability count

Bequest migration appends abilities to a living Tiered heir, so the heir can
hold more abilities than its Tier's `abilityCount` (Cinder 1, Shade 2,
Doom 3) — up to the live pool cap. We allow the overflow instead of
fizzling at the Tier count because fizzle would make Bequest dead weight in
full packs, which is exactly where a coven-support Ability should matter.

Without context a future reader will wonder why a Cinder shows two ability
names; the alternative (fizzle-at-cap) was rejected for that reason, at the
cost of the Tier-count contract no longer being absolute. The counterweight
is the conservation rule: a death removes N instances with the corpse and
adds at most N minus one (Bequest itself is consumed, never inherited), so
population-wide ability instances never grow through migration.
