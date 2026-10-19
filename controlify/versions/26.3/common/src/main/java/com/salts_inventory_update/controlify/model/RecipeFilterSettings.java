package com.salts_inventory_update.controlify.model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;

/** 26.3 companion-only preferences, retained independently of any window or world. */
public final class RecipeFilterSettings {
    private final Path path;
    private RecipeFilter filter = RecipeFilter.ALL;
    public RecipeFilterSettings(Path path) { this.path = path; }
    public static RecipeFilterSettings load(Path path) throws IOException {
        var settings = new RecipeFilterSettings(path);
        if (Files.isRegularFile(path)) {
            var properties = new Properties();
            try (var input = Files.newInputStream(path)) { properties.load(input); }
            catch (IllegalArgumentException malformed) { return settings; }
            settings.filter = RecipeFilter.parse(properties.getProperty("filter"));
        }
        return settings;
    }
    public RecipeFilter filter() { return filter; }
    public void set(RecipeFilter filter) throws IOException {
        this.filter = java.util.Objects.requireNonNull(filter);
        Files.createDirectories(path.toAbsolutePath().getParent());
        var properties = new Properties();
        properties.setProperty("filter", filter.key());
        try (var output = Files.newOutputStream(path)) { properties.store(output, "Salt's Controlify recipe wheel"); }
    }
}
