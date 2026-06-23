package com.salts_inventory_update.compat.toms_storage;

import org.jspecify.annotations.Nullable;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.debug.DesktopDebug;
import com.salts_inventory_update.network.DesktopPackets;

public final class TomsStorageCompat {
    public static final String MOD_ID = "toms_storage";

    public static final ResourceLocation NBT_CHANNEL = DesktopPackets.id("toms_storage_nbt");
    public static final ResourceLocation TERMINAL_ACTION_CHANNEL = DesktopPackets.id("toms_storage_terminal_action");
    public static final ResourceLocation TERMINAL_SNAPSHOT_CHANNEL = DesktopPackets.id("toms_storage_terminal_snapshot");
    public static final ResourceLocation LINK_SNAPSHOT_CHANNEL = DesktopPackets.id("toms_storage_link_snapshot");
    public static final ResourceLocation TAG_SNAPSHOT_CHANNEL = DesktopPackets.id("toms_storage_tag_snapshot");

    public static final String STORAGE_TERMINAL = "storage_terminal";
    public static final String CRAFTING_TERMINAL = "crafting_terminal";
    public static final String INVENTORY_CONFIGURATOR = "inventory_configurator";
    public static final String LEVEL_EMITTER = "level_emitter";
    public static final String INVENTORY_LINK = "inventory_link";
    public static final String ITEM_FILTER = "item_filter";
    public static final String TAG_ITEM_FILTER = "tag_item_filter";
    public static final String FILING_CABINET = "filing_cabinet";

    private TomsStorageCompat() {
    }

    public static boolean loaded() {
        return FabricLoader.getInstance().isModLoaded(MOD_ID);
    }

    public static void info(String message, Object... args) {
        SaltsInventoryUpdate.LOGGER.info("[desktop-toms] " + message, args);
    }

    public static void warn(String message, Object... args) {
        SaltsInventoryUpdate.LOGGER.warn("[desktop-toms] " + message, args);
    }

    public static ResourceLocation id(String path) {
        return new ResourceLocation(MOD_ID, path);
    }

    public static @Nullable MenuType<?> menu(String path) {
        if (!loaded()) {
            return null;
        }

        ResourceLocation id = id(path);
        MenuType<?> menuType = BuiltInRegistries.MENU.get(id);
        if (menuType == null) {
            DesktopDebug.warn("Tom's Storage compat missing menu id={}", id);
            warn("missing menu id={}", id);
        }
        return menuType;
    }

    public static boolean isMenu(MenuType<?> menuType, String path) {
        ResourceLocation key = BuiltInRegistries.MENU.getKey(menuType);
        return id(path).equals(key);
    }

    public static boolean isTerminal(MenuType<?> menuType) {
        return isMenu(menuType, STORAGE_TERMINAL) || isMenu(menuType, CRAFTING_TERMINAL);
    }
}
