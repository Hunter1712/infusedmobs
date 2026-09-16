package io.github.hunter1712.infusedmobs.ability;

/**
 * Test-only helper to populate the Ability pool without Minecraft bootstrap.
 */
public final class TestAbilities {
    private TestAbilities() {}

    public static void register(String id, TriggerType trigger) {
        AbilityRegistry.all(id, id, trigger, (mob, target, damage) -> {});
    }
}
