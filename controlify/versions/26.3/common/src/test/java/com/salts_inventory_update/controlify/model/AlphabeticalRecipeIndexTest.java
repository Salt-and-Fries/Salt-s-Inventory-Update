package com.salts_inventory_update.controlify.model;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class AlphabeticalRecipeIndexTest {
    private static AlphabeticalRecipeIndex.Entry<String> entry(String name, String key) {
        return new AlphabeticalRecipeIndex.Entry<>(key, name, key);
    }
    @Test void displayNamesDetermineOrderInsteadOfRecipeOrModIdentifiers() {
        var catalog = AlphabeticalRecipeIndex.build(List.of(entry("Zinc Ingot", "a:first"), entry("apple", "z:last"),
            entry("Acacia Planks", "m:middle"), entry("Barrel", "b:second")), "en_us");
        assertEquals(List.of("Acacia Planks", "apple", "Barrel", "Zinc Ingot"), catalog.entries().stream().map(AlphabeticalRecipeIndex.Entry::displayName).toList());
    }
    @Test void eachPresentLetterAppearsOnceAtItsFirstIndividualRecipe() {
        var catalog = AlphabeticalRecipeIndex.build(List.of(entry("Apple", "1"), entry("Acacia", "2"), entry("Carrot", "3"), entry("Coal", "4"), entry("Zinc", "5")), "en_us");
        assertEquals(List.of(new AlphabeticalRecipeIndex.Group("A", 0), new AlphabeticalRecipeIndex.Group("C", 2), new AlphabeticalRecipeIndex.Group("Z", 4)), catalog.groups());
        assertFalse(catalog.groups().stream().anyMatch(group -> group.letter().equals("B")));
        for (var group : catalog.groups()) {
            var selection = new ContextualWheelSelection();
            double angle = ContextualWheelSelection.outerMarkerAngle(group.firstIndex(), catalog.entries().size());
            selection.update(catalog.entries().size(), (float)Math.sin(angle), (float)-Math.cos(angle), 0, 0, false, false, 0);
            assertEquals(group.firstIndex(), selection.selected(catalog.entries().size()));
        }
    }
    @Test void equalNamesUseStableIdentityAsTieBreakerAcrossRefreshes() {
        var first = AlphabeticalRecipeIndex.build(List.of(entry("Oak Log", "recipe:9"), entry("oak log", "recipe:2")), "en_us");
        var second = AlphabeticalRecipeIndex.build(List.of(entry("oak log", "recipe:2"), entry("Oak Log", "recipe:9")), "en_us");
        assertEquals(List.of("recipe:2", "recipe:9"), first.entries().stream().map(AlphabeticalRecipeIndex.Entry::stableKey).toList());
        assertEquals(first, second);
    }
    @Test void translatedNamesRebuildLettersAndOrderForTheSameRecipeIdentities() {
        var english = AlphabeticalRecipeIndex.build(List.of(entry("Apple", "apple"), entry("Stick", "stick")), "en_us");
        var french = AlphabeticalRecipeIndex.build(List.of(entry("Pomme", "apple"), entry("Bâton", "stick")), "fr_fr");
        assertEquals(List.of("A", "S"), english.groups().stream().map(AlphabeticalRecipeIndex.Group::letter).toList());
        assertEquals(List.of("B", "P"), french.groups().stream().map(AlphabeticalRecipeIndex.Group::letter).toList());
        assertEquals(List.of("stick", "apple"), french.entries().stream().map(AlphabeticalRecipeIndex.Entry::value).toList());
    }
    @Test void accentedLatinNamesFoldWithoutDroppingUnmarkedOrNonLatinRecipes() {
        var catalog = AlphabeticalRecipeIndex.build(List.of(entry("Émeraude", "emerald"), entry("échelle", "ladder"), entry("棒", "stick"), entry("12 blocks", "blocks")), "fr_fr");
        assertEquals(4, catalog.entries().size());
        assertEquals(List.of("E"), catalog.groups().stream().map(AlphabeticalRecipeIndex.Group::letter).toList());
        assertEquals(4, catalog.entries().stream().map(AlphabeticalRecipeIndex.Entry::value).distinct().count());
    }
    @Test void emptyOrFilteredCatalogHasOnlyItsOwnPresentLetters() {
        assertTrue(AlphabeticalRecipeIndex.build(List.of(), "en_us").groups().isEmpty());
        var filtered = AlphabeticalRecipeIndex.build(List.of(entry("Carrot", "carrot")), "en_us");
        assertEquals(List.of(new AlphabeticalRecipeIndex.Group("C", 0)), filtered.groups());
        assertEquals(1, filtered.entries().size());
    }
}
