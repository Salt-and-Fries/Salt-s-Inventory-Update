package com.salts_inventory_update.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Consumer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import com.salts_inventory_update.SaltsInventoryUpdate;
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

    public static final class ConfigFile {
        public boolean expandableInventory = true;
        public String windowOpeningStyle = WindowOpeningStyle.TOP_OUTSIDE.name();
        public boolean openUnlocked = false;
        public boolean allowResizing = true;

        private ConfigFile normalized() {
            this.windowOpeningStyle = WindowOpeningStyle.parse(this.windowOpeningStyle).name();
            return this;
        }

        public WindowOpeningStyle windowOpeningStyle() {
            return WindowOpeningStyle.parse(this.windowOpeningStyle);
        }

        public void setWindowOpeningStyle(WindowOpeningStyle style) {
            this.windowOpeningStyle = (style == null ? WindowOpeningStyle.TOP_OUTSIDE : style).name();
        }
    }
}
