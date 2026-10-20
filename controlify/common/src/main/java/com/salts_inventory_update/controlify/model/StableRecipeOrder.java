package com.salts_inventory_update.controlify.model;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/** Pins a wheel's membership until its category/filter changes, while refreshing recipe state. */
public final class StableRecipeOrder<T> {
    private Map<String, T> entries;

    public List<T> update(List<T> available, List<T> initiallyVisible, Function<T, String> key) {
        if (entries == null) {
            entries = new LinkedHashMap<>();
            initiallyVisible.forEach(value -> entries.putIfAbsent(key.apply(value), value));
        }
        for (T value : available) {
            String id = key.apply(value);
            if (entries.containsKey(id)) entries.put(id, value);
        }
        return List.copyOf(entries.values());
    }

    public void reset() { entries = null; }
}
