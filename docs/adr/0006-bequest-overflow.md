# Bequest heirs may overflow their Tier ability count

SUPERSEDED by issue #014 ("Bequest converts vanilla mobs into Cinder").

The old behavior documented here — Bequest migration appends abilities to a
living Tiered heir, letting it hold more than its Tier's `abilityCount` —
was removed. Bequest no longer buffs existing tiered heirs; instead a death
converts nearby vanilla mobs into Cinder, each receiving exactly one
(non-Bequest) ability, honouring Cinder's ability-count cap of 1.

The ADR's original rationale (overflow keeps Bequest useful in full packs)
no longer applies: Bequest creates new Cinder mobs rather than buffing
existing ones, so the Tier-count contract is absolute again. The conservation
rule (a death removes N instances and adds at most N minus one; Bequest is
consumed, never inherited) still holds.
