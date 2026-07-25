package io.github.hunter1712.mobabilities.damage;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.server.level.ServerLevel;

/**
 * Registry for custom damage types used by the Mob Abilities mod.
 *
 * <p>Damage types are defined via datapack JSON in
 * {@code data/mobabilities/damage_type/acid.json} and referenced in
 * code via {@link ResourceKey}.
 */
public final class ModDamageTypes {

    private ModDamageTypes() {
        // static-only utility class
    }

    /**
     * Resource key for the acid damage type, used to look up the
     * {@link DamageType} holder from the registry.
     */
    public static final ResourceKey<DamageType> ACID = ResourceKey.create(
            Registries.DAMAGE_TYPE,
            Identifier.fromNamespaceAndPath("mobabilities", "acid")
    );

    /**
     * Ensures the damage type registry class is loaded.
     * Call from {@code ModInitializer.onInitialize()}.
     */
    public static void onInitialize() {
        // Damage types are loaded from datapack JSON automatically.
        // This method ensures the class loads and the ResourceKey is available.
    }

    /**
     * Creates an acid {@link DamageSource} attributed to the given living entity.
     *
     * @param source the entity responsible for the acid damage
     * @return a new acid DamageSource, or generic damage if lookup fails
     */
    public static DamageSource acid(LivingEntity source) {
        if (source.level() instanceof ServerLevel serverLevel) {
            return new DamageSource(
                    serverLevel.registryAccess()
                            .lookupOrThrow(Registries.DAMAGE_TYPE)
                            .getOrThrow(ACID),
                    source
            );
        }
        return source.damageSources().generic();
    }

    /**
     * Creates an acid {@link DamageSource} without a source entity.
     *
     * @param level the server level
     * @return a new acid DamageSource
     */
    public static DamageSource acidWithoutSource(ServerLevel level) {
        return new DamageSource(
                level.registryAccess()
                        .lookupOrThrow(Registries.DAMAGE_TYPE)
                        .getOrThrow(ACID)
        );
    }
}
