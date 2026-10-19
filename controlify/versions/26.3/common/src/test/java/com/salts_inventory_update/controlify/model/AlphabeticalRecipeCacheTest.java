package com.salts_inventory_update.controlify.model;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AlphabeticalRecipeCacheTest {
    private static AlphabeticalRecipeIndex.Entry<Object> entry(String key, String name) {
        return new AlphabeticalRecipeIndex.Entry<>(new Object(), name, key);
    }

    @Test void refreshedAvailabilityKeepsOrderButUsesOnlyFreshRecipeValues() {
        var cache = new AlphabeticalRecipeCache();
        var resources = new Object();
        var before = cache.index(List.of(entry("b", "Birch"), entry("a", "Acacia")), "en_us", resources);
        var freshA = entry("a", "Acacia");
        var freshB = entry("b", "Birch");
        var after = cache.index(List.of(freshB, freshA), "en_us", resources);
        assertSame(before.groups(), after.groups(), "Reuse expensive letter index on refresh or reopen");
        assertEquals(List.of(freshA, freshB), after.entries());
        assertNotSame(before.entries().getFirst().value(), after.entries().getFirst().value());
    }

    @Test void recipeNamesMembershipLocaleAndResourceReloadInvalidateOrdering() {
        var cache = new AlphabeticalRecipeCache();
        var resources = new Object();
        var first = cache.index(List.of(entry("a", "Acacia"), entry("b", "Birch")), "en_us", resources);
        var renamed = cache.index(List.of(entry("a", "Zebra"), entry("b", "Birch")), "en_us", resources);
        assertEquals(List.of("b", "a"), renamed.entries().stream().map(AlphabeticalRecipeIndex.Entry::stableKey).toList());
        assertNotEquals(first.groups(), renamed.groups());
        var subset = cache.index(List.of(entry("b", "Birch")), "en_us", resources);
        assertEquals(1, subset.entries().size());
        var translated = cache.index(List.of(entry("b", "Birch")), "de_de", resources);
        assertNotSame(subset.groups(), translated.groups());
        var reloaded = cache.index(List.of(entry("b", "Birch")), "de_de", new Object());
        assertNotSame(translated.groups(), reloaded.groups());
    }
}
