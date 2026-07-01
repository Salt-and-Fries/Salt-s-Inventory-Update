package com.salts_inventory_update.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.SaltsInventoryRuntime;
import com.salts_inventory_update.debug.DesktopDebug;

public final class SaltsInventoryConfig {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path CONFIG_PATH = FabricLoader.getInstance()
        .getConfigDir()
        .resolve(SaltsInventoryUpdate.MOD_ID)
        .resolve("config.json");
    private static ConfigFile current;

    private SaltsInventoryConfig() {
    }

    public static ConfigFile get() {
        if (current == null) {
            load();
        }
        return current;
    }

    public static ConfigFile load() {
        ConfigFile loaded = null;
        if (Files.isRegularFile(CONFIG_PATH)) {
            try (Reader reader = Files.newBufferedReader(CONFIG_PATH)) {
                loaded = GSON.fromJson(reader, ConfigFile.class);
            } catch (IOException | RuntimeException exception) {
                DesktopDebug.warn("client config load failed path={} reason={}", CONFIG_PATH, exception.toString());
            }
        }

        current = loaded == null ? new ConfigFile() : loaded.normalized();
        applyRuntimeState(current);
        save();
        return current;
    }

    public static ConfigFile reload() {
        current = null;
        return load();
    }

    public static void update(Consumer<ConfigFile> updater) {
        ConfigFile config = get();
        updater.accept(config);
        current = config.normalized();
        applyRuntimeState(current);
        save();
    }

    public static void save() {
        try {
            Files.createDirectories(CONFIG_PATH.getParent());
            try (Writer writer = Files.newBufferedWriter(CONFIG_PATH)) {
                GSON.toJson(get().normalized(), writer);
            }
        } catch (IOException | RuntimeException exception) {
            DesktopDebug.warn("client config save failed path={} reason={}", CONFIG_PATH, exception.toString());
        }
    }

    public static boolean isForcedContainerWindow(String menuId) {
        return get().forcedContainerWindows.contains(menuId);
    }

    public static void setForcedContainerWindow(String menuId, boolean enabled) {
        update(config -> {
            if (enabled) {
                config.forcedContainerWindows.add(menuId);
            } else {
                config.forcedContainerWindows.remove(menuId);
            }
        });
    }

    private static void applyRuntimeState(ConfigFile config) {
        SaltsInventoryRuntime.setConfiguredEnabled(config.enableMod);
        SaltsInventoryRuntime.setDetailedConsoleLogs(config.enableDetailedConsoleLogs);
        SaltsInventoryRuntime.setForcedContainerWindowIds(config.forcedContainerWindows);
    }

    public static final class ConfigFile {
        public boolean enableMod = true;
        public boolean expandableInventory = false;
        public boolean enableDetailedConsoleLogs = false;
        public String windowOpeningStyle = WindowOpeningStyle.AROUND_INVENTORY.name();
        public boolean openUnlocked = false;
        public boolean allowResizing = false;
        public boolean enableWindowSnapping = true;
        public boolean resetLockedWindows = true;
        public boolean enableGhostPins = false;
        public double ghostWindowOpacity = 0.5D;
        public double eHoldCloseAllSeconds = 0.5D;
        public List<String> forcedContainerWindows = new ArrayList<>();

        private ConfigFile normalized() {
            this.windowOpeningStyle = WindowOpeningStyle.parse(this.windowOpeningStyle).name();
            this.ghostWindowOpacity = clamp(this.ghostWindowOpacity, 0.15D, 0.90D);
            this.eHoldCloseAllSeconds = clamp(roundToQuarter(this.eHoldCloseAllSeconds), 0.5D, 10.0D);
            this.forcedContainerWindows = normalizeIds(this.forcedContainerWindows);
            return this;
        }

        public WindowOpeningStyle windowOpeningStyle() {
            return WindowOpeningStyle.parse(this.windowOpeningStyle);
        }

        public void setWindowOpeningStyle(WindowOpeningStyle style) {
            this.windowOpeningStyle = (style == null ? WindowOpeningStyle.AROUND_INVENTORY : style).name();
        }

        public float ghostWindowOpacity() {
            return (float) clamp(this.ghostWindowOpacity, 0.15D, 0.90D);
        }

        public long eHoldCloseAllMs() {
            return Math.round(clamp(this.eHoldCloseAllSeconds, 0.5D, 10.0D) * 1000.0D);
        }

        public long eHoldOverlayDelayMs() {
            return Math.max(0L, this.eHoldCloseAllMs() / 4L);
        }

        public void resetToDefaults() {
            ConfigFile defaults = new ConfigFile();
            this.enableMod = defaults.enableMod;
            this.expandableInventory = defaults.expandableInventory;
            this.enableDetailedConsoleLogs = defaults.enableDetailedConsoleLogs;
            this.windowOpeningStyle = defaults.windowOpeningStyle;
            this.openUnlocked = defaults.openUnlocked;
            this.allowResizing = defaults.allowResizing;
            this.enableWindowSnapping = defaults.enableWindowSnapping;
            this.resetLockedWindows = defaults.resetLockedWindows;
            this.enableGhostPins = defaults.enableGhostPins;
            this.ghostWindowOpacity = defaults.ghostWindowOpacity;
            this.eHoldCloseAllSeconds = defaults.eHoldCloseAllSeconds;
            this.forcedContainerWindows = new ArrayList<>(defaults.forcedContainerWindows);
        }

        private static List<String> normalizeIds(List<String> ids) {
            if (ids == null || ids.isEmpty()) {
                return new ArrayList<>();
            }

            LinkedHashSet<String> normalized = new LinkedHashSet<>();
            for (String id : ids) {
                if (id != null && !id.isBlank()) {
                    normalized.add(id.trim());
                }
            }
            ArrayList<String> sorted = new ArrayList<>(normalized);
            Collections.sort(sorted);
            return sorted;
        }

        private static double roundToQuarter(double value) {
            return Math.round(value * 4.0D) / 4.0D;
        }

        private static double clamp(double value, double min, double max) {
            if (Double.isNaN(value) || Double.isInfinite(value)) {
                return min;
            }
            return Math.max(min, Math.min(max, value));
        }
    }
}
