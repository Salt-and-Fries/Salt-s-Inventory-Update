package com.salts_inventory_update.controlify.model;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.*;

final class WorkspaceSoundSettingsTest {
    @TempDir Path directory;

    @Test void newFileUsesOffPresetWithSilentHover() throws Exception {
        Path file = directory.resolve("nested/sounds.properties");
        var settings = WorkspaceSoundSettings.load(file);
        assertTrue(Files.isRegularFile(file));
        assertEquals(WorkspaceSoundSettings.Preset.OFF, settings.preset());
        assertEquals(0, settings.masterVolume());
        assertEquals(0.3, settings.navigationVolume());
        assertEquals(0.65, settings.transitionsVolume());
        assertEquals(0.8, settings.actionsVolume());
        assertFalse(settings.hoverEnabled());
        assertEquals(settings.preset(), WorkspaceSoundSettings.load(file).preset());
        assertEquals(0, WorkspaceSoundSettings.load(file).masterVolume());
    }

    @Test void customMixRoundTripsAndPreservesUnrecognizedProperties() throws Exception {
        Path file = directory.resolve("sounds.properties");
        Files.writeString(file, "future-setting=retain\npreset=quiet\n");
        var settings = WorkspaceSoundSettings.load(file);
        settings.setMasterVolume(0.45);
        settings.setNavigationVolume(0);
        settings.setTransitionsVolume(0.9);
        settings.setActionsVolume(0.6);
        settings.setHoverEnabled(true);
        settings.save();
        var restored = WorkspaceSoundSettings.load(file);
        assertEquals(WorkspaceSoundSettings.Preset.CUSTOM, restored.preset());
        assertEquals(0.45, restored.masterVolume());
        assertEquals(0, restored.navigationVolume());
        assertEquals(0.9, restored.transitionsVolume());
        assertEquals(0.6, restored.actionsVolume());
        assertTrue(restored.hoverEnabled());
        Properties properties = new Properties();
        try (var input = Files.newInputStream(file)) { properties.load(input); }
        assertEquals("retain", properties.getProperty("future-setting"));
        try (var files = Files.list(directory)) { assertEquals(1, files.count()); }
    }

    @Test void malformedValuesCannotCreateUnsafeVolume() throws Exception {
        Path file = directory.resolve("sounds.properties");
        Files.writeString(file, "preset=unknown\nvolume=NaN\nnavigation=-2\ntransitions=Infinity\nactions=12\nhover=maybe\n");
        var settings = WorkspaceSoundSettings.load(file);
        assertEquals(0, settings.masterVolume());
        assertEquals(0, settings.navigationVolume());
        assertEquals(0.65, settings.transitionsVolume());
        assertEquals(1, settings.actionsVolume());
        assertFalse(settings.hoverEnabled());
        settings.setActionsVolume(Double.NEGATIVE_INFINITY);
        settings.setMasterVolume(Double.NaN);
        assertEquals(1, settings.actionsVolume());
        assertEquals(0, settings.masterVolume());
    }

    @Test void minimalKeepsActionFeedbackAndRestoreReturnsOffDefaults() {
        var settings = WorkspaceSoundSettings.defaults();
        settings.applyPreset(WorkspaceSoundSettings.Preset.MINIMAL);
        assertEquals(0, settings.navigationVolume());
        assertEquals(0, settings.transitionsVolume());
        assertTrue(settings.actionsVolume() > 0);
        settings.setMasterVolume(0);
        assertEquals(WorkspaceSoundSettings.Preset.CUSTOM, settings.preset());
        assertEquals(0, settings.masterVolume());
        settings.resetToDefaults();
        assertEquals(WorkspaceSoundSettings.Preset.OFF, settings.preset());
        assertEquals(0, settings.masterVolume());
        assertFalse(settings.hoverEnabled());
    }

    @Test void unchangedAndInvalidEditsKeepSelectedPreset() {
        var settings = WorkspaceSoundSettings.defaults();
        settings.setMasterVolume(settings.masterVolume());
        settings.setHoverEnabled(false);
        settings.setNavigationVolume(Double.NaN);
        assertEquals(WorkspaceSoundSettings.Preset.OFF, settings.preset());
        settings.applyPreset(WorkspaceSoundSettings.Preset.STANDARD);
        double oldGain = settings.navigationVolume();
        settings.applyPreset(WorkspaceSoundSettings.Preset.CUSTOM);
        assertEquals(oldGain, settings.navigationVolume());
    }

    @Test void failedLoadCanRecoverBySavingDefaultsToTheSameLocation() throws Exception {
        Path file = directory.resolve("sounds.properties");
        Files.writeString(file, "preset=\\uZZZZ\n");
        assertThrows(java.io.IOException.class, () -> WorkspaceSoundSettings.load(file));
        var recovery = WorkspaceSoundSettings.defaults(file);
        assertEquals(0, recovery.masterVolume());
        recovery.setMasterVolume(0.25);
        recovery.save();
        assertEquals(0.25, WorkspaceSoundSettings.load(file).masterVolume());
    }

    @Test void missingUnknownAndOffPresetsCannotEnableStaleVolume() throws Exception {
        Path file = directory.resolve("sounds.properties");
        for (String preset : new String[] { "", "preset=\n", "preset=unknown\n", "preset=off\n" }) {
            Files.writeString(file, preset + "volume=0.7\n");
            var settings = WorkspaceSoundSettings.load(file);
            assertEquals(0, settings.masterVolume(), preset);
            assertEquals(WorkspaceSoundSettings.Preset.OFF, settings.preset(), preset);
            settings.save();
            assertEquals(0, WorkspaceSoundSettings.load(file).masterVolume(), preset);
        }
        Files.writeString(file, "# Empty configuration\n");
        assertEquals(0, WorkspaceSoundSettings.load(file).masterVolume());
    }

    @Test void customPresetNeedsAnExplicitValidVolumeToEnableSounds() throws Exception {
        Path file = directory.resolve("sounds.properties");
        for (String volume : new String[] { "", "volume=\n", "volume=NaN\n", "volume=Infinity\n", "volume=invalid\n" }) {
            Files.writeString(file, "preset=custom\n" + volume);
            assertEquals(0, WorkspaceSoundSettings.load(file).masterVolume(), volume);
        }
    }

    @Test void savedAudibleOptInsArePreserved() throws Exception {
        Path file = directory.resolve("sounds.properties");
        for (String preset : new String[] { "standard", "quiet", "minimal", "custom" }) {
            Files.writeString(file, "preset=" + preset + "\nvolume=0.45\n");
            var settings = WorkspaceSoundSettings.load(file);
            assertEquals(0.45, settings.masterVolume(), preset);
            settings.save();
            assertEquals(0.45, WorkspaceSoundSettings.load(file).masterVolume(), preset);
        }
    }
}
