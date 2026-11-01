package io.github.hunter1712.infusedmobs.tier;

import io.github.hunter1712.infusedmobs.ability.Ability;
import io.github.hunter1712.infusedmobs.ability.AbilityRegistry;

import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.entity.Mob;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Nametag presentation behind the Infusion seam: roll -> coloured text,
 * visibility, and the tracked-mob refresh scan.
 * <p>
 * The roll coordinator ({@link MobTierManager}) owns state; this module owns
 * how a roll is shown. Text formatting is pure (no mob, no level) so it is
 * unit-tested without bootstrap; only the refresh boundary scans server
 * levels and writes nametags.
 */
public final class NametagPresentation {

    private NametagPresentation() {}

    /** Roll -> coloured nametag text. Pure; no mob or level required. */
    static String text(Rolled rolled, List<String> abilityNames, String entityName) {
        String colour = rolled instanceof Rolled.Tiered tiered
                ? tiered.tier().colourCode()
                : "§7";
        if (abilityNames.isEmpty()) return colour + entityName;
        return colour + String.join("§7, ", abilityNames) + " §f" + entityName;
    }

    /** Base entity-type name for nametags. */
    static String entityTypeName(Mob mob) {
        return mob.getType().getDescription().getString();
    }

    /** Ability display names in input order. */
    static List<String> abilityNames(List<Ability> abilities) {
        return abilities.stream().map(Ability::name).toList();
    }

    /** Writes (or clears) a mob's custom nametag per the visibility flag. */
    static void setNametag(Mob mob, String text, boolean show) {
        if (!show) {
            mob.setCustomName(null);
            mob.setCustomNameVisible(false);
            return;
        }
        mob.setCustomName(Component.literal(text));
        mob.setCustomNameVisible(true);
    }

    /**
     * Applies or removes nametags for all tracked mobs based on the current
     * visibility setting. Called when the toggle changes via command.
     * <p>
     * The server-level scan lives here, not in the roll coordinator: the
     * coordinator hands over its tracked snapshot and this module finds each
     * mob and applies (or clears) its nametag.
     */
    public static void refresh(MinecraftServer server, boolean show) {
        for (Map.Entry<UUID, Rolled> entry : MobTierManager.trackedSnapshot().entrySet()) {
            Mob mob = MobTierManager.findMob(server, entry.getKey());
            if (mob == null) continue;
            Rolled rolled = entry.getValue();

            if (!show) {
                setNametag(mob, null, false);
            } else if (rolled instanceof Rolled.Tiered || rolled instanceof Rolled.Split) {
                List<Ability> abilities = AbilityRegistry.getAbilitiesByIds(rolled.abilityIds());
                setNametag(mob, text(rolled, abilityNames(abilities), entityTypeName(mob)), true);
            }
            // Rolled nothing — leave the mob vanilla.
        }
    }
}
