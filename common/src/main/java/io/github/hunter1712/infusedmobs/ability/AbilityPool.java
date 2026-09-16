package io.github.hunter1712.infusedmobs.ability;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Instantiable Ability pool: registration, id lookup in input order,
 * random draw with exclusions, and id listing.
 * The global facade delegates to a shared instance.
 */
public final class AbilityPool {
    private final List<Ability> all = new ArrayList<>();
    private final Map<String, Ability> byId = new HashMap<>();

    public void register(String id, String name, TriggerType trigger, AbilityEffect effect) {
        if (byId.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate ability id: '" + id + "'");
        }
        Ability ability = new Ability(id, name, trigger, effect);
        all.add(ability);
        byId.put(id, ability);
    }

    public void clear() {
        all.clear();
        byId.clear();
    }

    public List<Ability> random(int count, String... excludedIds) {
        if (count <= 0 || all.isEmpty()) return List.of();
        List<Ability> pool = new ArrayList<>(all);
        if (excludedIds.length > 0) {
            Set<String> excluded = new HashSet<>(List.of(excludedIds));
            pool.removeIf(a -> excluded.contains(a.id()));
        }
        if (pool.isEmpty()) return List.of();
        Collections.shuffle(pool, ThreadLocalRandom.current());
        return Collections.unmodifiableList(pool.subList(0, Math.min(count, pool.size())));
    }

    public List<Ability> byIds(List<String> ids) {
        List<Ability> result = new ArrayList<>(ids.size());
        for (String id : ids) {
            Ability ability = byId.get(id);
            if (ability != null) result.add(ability);
        }
        return result;
    }

    public Ability byId(String id) {
        return byId.get(id);
    }

    public List<String> allIds() {
        return all.stream().map(Ability::id).toList();
    }
}
