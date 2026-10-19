package com.salts_inventory_update.controlify.model;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Objects;
import java.util.Properties;

/** A small mixer belonging exclusively to the 26.3 controller companion. */
public final class WorkspaceSoundSettings {
    public enum Preset { OFF, STANDARD, QUIET, MINIMAL, CUSTOM }

    private final Path path;
    private final Properties preserved = new Properties();
    private Preset preset;
    private double masterVolume;
    private double navigationVolume;
    private double transitionsVolume;
    private double actionsVolume;
    private boolean hoverEnabled;

    private WorkspaceSoundSettings(Path path) {
        this.path = path;
        applyPreset(Preset.OFF);
    }

    /** In-memory defaults are also usable when a configuration file cannot be read. */
    public static WorkspaceSoundSettings defaults() { return new WorkspaceSoundSettings(null); }

    /** Keep a retryable save location when loading a malformed or temporarily unreadable file. */
    public static WorkspaceSoundSettings defaults(Path path) { return new WorkspaceSoundSettings(Objects.requireNonNull(path, "path")); }

    public static WorkspaceSoundSettings load(Path path) throws IOException {
        Objects.requireNonNull(path, "path");
        WorkspaceSoundSettings settings = new WorkspaceSoundSettings(path);
        if (!Files.isRegularFile(path)) {
            settings.save();
            return settings;
        }
        try (InputStream input = Files.newInputStream(path)) {
            try { settings.preserved.load(input); }
            catch (IllegalArgumentException invalid) { throw new IOException("Invalid controller sound settings", invalid); }
        }
        Preset selected;
        try { selected = Preset.valueOf(settings.preserved.getProperty("preset", "off").trim().toUpperCase(Locale.ROOT)); }
        catch (IllegalArgumentException invalid) { selected = Preset.OFF; }
        settings.applyPreset(selected);
        // Off (including an absent/unknown preset) must stay silent even if a stale
        // volume remains in the file. Valid saved audible/custom mixes are opt-ins.
        if (selected != Preset.OFF)
            settings.setMasterVolume(readVolume(settings.preserved, "volume", settings.masterVolume));
        settings.setNavigationVolume(readVolume(settings.preserved, "navigation", settings.navigationVolume));
        settings.setTransitionsVolume(readVolume(settings.preserved, "transitions", settings.transitionsVolume));
        settings.setActionsVolume(readVolume(settings.preserved, "actions", settings.actionsVolume));
        String hover = settings.preserved.getProperty("hover", "false").trim();
        settings.setHoverEnabled("true".equalsIgnoreCase(hover));
        return settings;
    }

    private static double readVolume(Properties properties, String key, double fallback) {
        try { return volume(Double.parseDouble(properties.getProperty(key, Double.toString(fallback))), fallback); }
        catch (NumberFormatException invalid) { return fallback; }
    }

    private static double volume(double value, double fallback) {
        return Double.isFinite(value) ? Math.max(0.0, Math.min(1.0, value)) : fallback;
    }

    public Preset preset() { return preset; }
    public double masterVolume() { return masterVolume; }
    public double navigationVolume() { return navigationVolume; }
    public double transitionsVolume() { return transitionsVolume; }
    public double actionsVolume() { return actionsVolume; }
    public boolean hoverEnabled() { return hoverEnabled; }

    public void applyPreset(Preset value) {
        Objects.requireNonNull(value, "preset");
        preset = value;
        switch (value) {
            case OFF -> { masterVolume = 0; navigationVolume = 0.3; transitionsVolume = 0.65; actionsVolume = 0.8; hoverEnabled = false; }
            case STANDARD -> { masterVolume = 1.0; navigationVolume = 0.55; transitionsVolume = 0.8; actionsVolume = 1.0; hoverEnabled = false; }
            case QUIET -> { masterVolume = 0.7; navigationVolume = 0.3; transitionsVolume = 0.65; actionsVolume = 0.8; hoverEnabled = false; }
            case MINIMAL -> { masterVolume = 0.6; navigationVolume = 0; transitionsVolume = 0; actionsVolume = 0.65; hoverEnabled = false; }
            case CUSTOM -> { /* Keep the current mix when entering Custom. */ }
        }
    }

    public void resetToDefaults() { applyPreset(Preset.OFF); }

    public void setMasterVolume(double value) {
        value = volume(value, masterVolume);
        if (masterVolume != value) { masterVolume = value; preset = Preset.CUSTOM; }
    }
    public void setNavigationVolume(double value) {
        value = volume(value, navigationVolume);
        if (navigationVolume != value) { navigationVolume = value; preset = Preset.CUSTOM; }
    }
    public void setTransitionsVolume(double value) {
        value = volume(value, transitionsVolume);
        if (transitionsVolume != value) { transitionsVolume = value; preset = Preset.CUSTOM; }
    }
    public void setActionsVolume(double value) {
        value = volume(value, actionsVolume);
        if (actionsVolume != value) { actionsVolume = value; preset = Preset.CUSTOM; }
    }
    public void setHoverEnabled(boolean enabled) {
        if (hoverEnabled != enabled) { hoverEnabled = enabled; preset = Preset.CUSTOM; }
    }

    /** Replacing the complete file prevents a partial slider save from corrupting the mix. */
    public void save() throws IOException {
        if (path == null) return;
        Properties properties = new Properties();
        properties.putAll(preserved);
        properties.setProperty("preset", preset.name().toLowerCase(Locale.ROOT));
        properties.setProperty("volume", Double.toString(masterVolume));
        properties.setProperty("navigation", Double.toString(navigationVolume));
        properties.setProperty("transitions", Double.toString(transitionsVolume));
        properties.setProperty("actions", Double.toString(actionsVolume));
        properties.setProperty("hover", Boolean.toString(hoverEnabled));
        Path destination = path.toAbsolutePath();
        Path parent = destination.getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, ".salts-controlify-sounds-", ".properties");
        try {
            try (OutputStream output = Files.newOutputStream(temporary)) {
                properties.store(output, "Salt's Inventory Update - Controller inventory sounds (native UI and Master volumes still apply)");
            }
            try { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
            catch (AtomicMoveNotSupportedException unsupported) { Files.move(temporary, destination, StandardCopyOption.REPLACE_EXISTING); }
            preserved.clear();
            preserved.putAll(properties);
        } finally { Files.deleteIfExists(temporary); }
    }
}
