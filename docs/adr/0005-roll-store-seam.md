# Shared roll-store seam, separate lifetimes

`InfusedRegistry` (live Infused Mob state) and `TierRollStore` (persisted
roll state behind every `TierSavedData` Shim) implement one `RollStore`
seam (`get` / `put` / `remove` / `snapshot`). Lifetimes stay separate per
ADR-0003 — live state dies with the mob, persisted state restores on
chunk reload — but both adapters answer the same shape behind one
conformance test (`RollStoreConformanceTest`), so callers learn one
interface and drift between the twins is pinned.

Cleanup already unifies behind one method: death (`MobDeathTrigger`)
and despawn (`EntityRemoveMixin`, DISCARDED only) both call
`MobTierManager.removeMob`, which untracks live state and clears the
persisted roll together — no split-brain path.
