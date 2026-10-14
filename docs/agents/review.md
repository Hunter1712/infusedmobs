# Review

Review runs on the diff, not the codebase: enforce the rules below, nothing else.

## Seams

A new type (class, interface, record) with one or zero production callers is
presumptive dead weight: ask for a one-line justification or request the
inline. Keepers share one trait — 2+ callers or 2+ implementations:
shared pure rules (`BlacklistEdit`), version polymorphism (`EffectToken`),
otherwise-untestable dispatch (`tickOnce`, `isMobAttack`).
