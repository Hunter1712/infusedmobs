package io.github.hunter1712.infusedmobs.tier;

/**
 * Instantiable gating decision: the World Blacklist plus Gamerule Gate
 * combination behind the static {@link InfusionGate} facade.
 * <p>
 * The decision is pure — no Minecraft bootstrap needed. Live code uses the
 * facade for one-liner call sites; tests instantiate a fresh decision core
 * per case, matching the
 * {@link io.github.hunter1712.infusedmobs.ability.AbilityPool} pattern.
 * The combination is unchanged: the blacklist dominates, otherwise the
 * per-save rule decides.
 */
public final class GatingDecision {

    /**
     * Decides the gate status from its inputs.
     *
     * @param worldBlacklisted true when the dimension is on the World Blacklist
     * @param storedEnabled    stored {@code infusedmobs:enabled} value, or null when unset
     * @param defaultEnabled   rule default when {@code storedEnabled} is null
     */
    public InfusionGate.Status decide(boolean worldBlacklisted, Boolean storedEnabled, boolean defaultEnabled) {
        if (worldBlacklisted) return InfusionGate.Status.WORLD_BLACKLISTED;
        boolean enabled = storedEnabled != null ? storedEnabled : defaultEnabled;
        return enabled ? InfusionGate.Status.ACTIVE : InfusionGate.Status.RULE_DISABLED;
    }
}
