package com.salts_inventory_update.controlify.model;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

/** Reuses collation and letter positions, never recipe values or ingredient availability. */
public final class AlphabeticalRecipeCache {
    private record Label(String key, String name) { }
    private List<Label> order = List.of();
    private List<AlphabeticalRecipeIndex.Group> groups = List.of();
    private String language;
    private Object resources;

    public <T> AlphabeticalRecipeIndex.Catalog<T> index(List<AlphabeticalRecipeIndex.Entry<T>> current,
                                                       String language, Object resources) {
        var byKey = new HashMap<String, AlphabeticalRecipeIndex.Entry<T>>(current.size());
        boolean unique = true;
        for (var entry : current) if (byKey.put(entry.stableKey(), entry) != null) unique = false;
        if (unique && Objects.equals(this.language, language) && this.resources == resources
                && order.size() == current.size() && order.stream().allMatch(label -> {
                    var entry = byKey.get(label.key());
                    return entry != null && entry.displayName().equals(label.name());
                })) {
            return new AlphabeticalRecipeIndex.Catalog<>(order.stream().map(label -> byKey.get(label.key())).toList(), groups);
        }
        var catalog = AlphabeticalRecipeIndex.build(current, language);
        this.language = unique ? language : null;
        this.resources = resources;
        order = unique ? catalog.entries().stream().map(entry -> new Label(entry.stableKey(), entry.displayName())).toList() : List.of();
        groups = catalog.groups();
        return catalog;
    }
}
