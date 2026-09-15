package io.github.hunter1712.infusedmobs.ability.trigger;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

/**
 * Version shim for HURT trigger registration (1.20.1 — legacy event).
 * Fabric API 0.92.0 has no {@code AFTER_DAMAGE}, so HURT abilities
 * (Bane, Thorns, …) are driven by {@code ALLOW_DAMAGE} instead, adapted
 * to {@link MobHurtTrigger.HurtHandler}.
 * <p>
 * Known divergences from the newer versions, owned by #9 (remaining
 * shims — exact trigger parity):
 * <ul>
 *   <li>the amount is pre-mitigation (armour/enchantments not yet applied),
 *       which marginally affects Siphon healing and Thorns reflection;</li>
 *   <li>shield blocks are not reported ({@code blocked} is always false),
 *       so a fully-blocked hit still fires HURT abilities.</li>
 * </ul>
 * The handler never cancels damage — it always returns true.
 */
public final class HurtTriggerHelper {
    private HurtTriggerHelper() {}

    public static void register(MobHurtTrigger.HurtHandler handler) {
        ServerLivingEntityEvents.ALLOW_DAMAGE.register((entity, source, amount) -> {
            handler.onHurt(entity, source, amount, amount, false);
            return true;
        });
    }
}
