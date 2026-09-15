package io.github.hunter1712.infusedmobs.ability.trigger;

import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;

/**
 * Version shim for HURT trigger registration (26.2 — modern event).
 * Adapts Fabric's {@code AFTER_DAMAGE} to {@link MobHurtTrigger.HurtHandler}.
 */
public final class HurtTriggerHelper {
    private HurtTriggerHelper() {}

    public static void register(MobHurtTrigger.HurtHandler handler) {
        ServerLivingEntityEvents.AFTER_DAMAGE.register(handler::onHurt);
    }
}
