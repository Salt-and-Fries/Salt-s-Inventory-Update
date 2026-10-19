package com.salts_inventory_update.controlify.model;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

class RecipeFilterTest {
    @TempDir Path directory;

    @Test void cyclesAllPartsCraftableAndBack() {
        assertEquals(RecipeFilter.CONTAINS_PARTS, RecipeFilter.ALL.next());
        assertEquals(RecipeFilter.CRAFTABLE, RecipeFilter.ALL.next().next());
        assertEquals(RecipeFilter.ALL, RecipeFilter.ALL.next().next().next());
    }
    @Test void partialIngredientOwnershipDoesNotRequireACraftableRecipe() {
        assertTrue(RecipeFilter.ALL.includes(false, false));
        assertFalse(RecipeFilter.CONTAINS_PARTS.includes(false, false));
        assertTrue(RecipeFilter.CONTAINS_PARTS.includes(false, true));
        assertTrue(RecipeFilter.CONTAINS_PARTS.includes(true, true));
        assertFalse(RecipeFilter.CRAFTABLE.includes(false, true));
        assertTrue(RecipeFilter.CRAFTABLE.includes(true, true));
    }
    @Test void everySelectionSurvivesANewSettingsInstance() throws Exception {
        Path path = directory.resolve("config/recipes.properties");
        var settings = RecipeFilterSettings.load(path);
        assertEquals(RecipeFilter.ALL, settings.filter());
        for (RecipeFilter filter : RecipeFilter.values()) {
            settings.set(filter);
            assertEquals(filter, RecipeFilterSettings.load(path).filter());
        }
    }
    @Test void missingUnknownOrMalformedPreferenceUsesAll() throws Exception {
        Path path = directory.resolve("recipes.properties");
        for (String text : new String[]{"", "filter=unknown", "filter=\\uINVALID"}) {
            Files.writeString(path, text);
            assertEquals(RecipeFilter.ALL, RecipeFilterSettings.load(path).filter());
        }
        Files.writeString(path, "filter= CRAFTABLE ");
        assertEquals(RecipeFilter.CRAFTABLE, RecipeFilterSettings.load(path).filter());
    }
}
