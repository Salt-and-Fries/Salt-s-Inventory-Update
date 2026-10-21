package com.salts_inventory_update.compat.toms_storage.client;

import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.inventory.MenuType;

import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.compat.toms_storage.TomsStorageCompat;

public final class TomsStorageClientCompat {
    private static boolean lifecycleHookRegistered;

    private TomsStorageClientCompat() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static synchronized void initialize() {
        if (!TomsStorageCompat.loaded()) {
            return;
        }

        TomsStorageCompat.info("client compat initialize");
        // Loader client setup may already have fired the startup callback.
        registerAll("initializer");
        if (!lifecycleHookRegistered) {
            lifecycleHookRegistered = true;
            DesktopClientApi.registerHelpPage(TomsStorageCompat.id("instructions"),
                new com.salts_inventory_update.api.client.desktop.DesktopHelpPage("Tom's Simple Storage", "T", java.util.List.of(
                    new com.salts_inventory_update.api.client.desktop.DesktopHelpPage.Section("When installed", java.util.List.of(
                        "Storage and crafting terminals appear as Salt windows.", "Terminal search, sorting, and recipes stay inside the window.")),
                    new com.salts_inventory_update.api.client.desktop.DesktopHelpPage.Section("Other supported screens", java.util.List.of(
                        "Filters, inventory links and level emitters use Salt layouts.")))));
            DesktopClientApi.onClientStarted(client -> registerAll("client-started"));
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static synchronized void registerAll(String phase) {
        int count = 0;
        count += register(TomsStorageCompat.STORAGE_TERMINAL, new TomsStorageDesktopWindows.Terminal(false)) ? 1 : 0;
        count += register(TomsStorageCompat.CRAFTING_TERMINAL, new TomsStorageDesktopWindows.Terminal(true)) ? 1 : 0;
        count += register(TomsStorageCompat.INVENTORY_CONFIGURATOR, new TomsStorageDesktopWindows.InventoryConfigurator()) ? 1 : 0;
        count += register(TomsStorageCompat.LEVEL_EMITTER, new TomsStorageDesktopWindows.LevelEmitter()) ? 1 : 0;
        count += register(TomsStorageCompat.INVENTORY_LINK, new TomsStorageDesktopWindows.InventoryLink()) ? 1 : 0;
        count += register(TomsStorageCompat.ITEM_FILTER, new TomsStorageDesktopWindows.ItemFilter()) ? 1 : 0;
        count += register(TomsStorageCompat.TAG_ITEM_FILTER, new TomsStorageDesktopWindows.TagItemFilter()) ? 1 : 0;
        TomsStorageCompat.info("client compat registered phase={} menuCount={}", phase, count);
        if (count < TomsStorageCompat.NATIVE_MENU_COUNT) {
            TomsStorageCompat.warn("client compat registered fewer menus than expected phase={} menuCount={} expected=7", phase, count);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static boolean register(String menuId, TomsStorageDesktopWindows.Definition definition) {
        MenuType menuType = TomsStorageCompat.menu(menuId);
        if (menuType != null) {
            DesktopClientApi.replaceWindow(menuType, definition);
            TomsStorageCompat.info(
                "client definition registered menuId={} registryKey={} menuType={} definition={} desktopScreenOverride=false",
                menuId,
                BuiltInRegistries.MENU.getKey(menuType),
                menuType,
                definition.getClass().getName()
            );
            return true;
        }
        TomsStorageCompat.warn("client definition missing menuId={} registryKey=toms_storage:{}", menuId, menuId);
        return false;
    }
}
