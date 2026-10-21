package com.salts_inventory_update.compat.sophisticated;

import io.netty.buffer.Unpooled;

import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.api.client.desktop.DesktopMenuFactories;
import com.salts_inventory_update.compat.sophisticated.client.SophisticatedHostedScreenDefinition;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.LimitedBarrelScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.LimitedBarrelSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageScreen;
import net.p3pp3rf1y.sophisticatedstorage.client.gui.StorageSettingsScreen;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.LimitedBarrelSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.common.gui.StorageSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedstorage.init.ModBlocks;

public final class SophisticatedStorageCompat {
    private SophisticatedStorageCompat() {
    }

    public static void initializeCommon() {
        MenuType<StorageContainerMenu> storage = ModBlocks.STORAGE_CONTAINER_TYPE.get();
        MenuType<LimitedBarrelContainerMenu> limited = ModBlocks.LIMITED_BARREL_CONTAINER_TYPE.get();
        MenuType<StorageSettingsContainerMenu> settings = ModBlocks.SETTINGS_CONTAINER_TYPE.get();
        MenuType<LimitedBarrelSettingsContainerMenu> limitedSettings = ModBlocks.LIMITED_BARREL_SETTINGS_CONTAINER_TYPE.get();

        registerMain(storage);
        registerMain(limited);
        registerSettings(settings);
        registerSettings(limitedSettings);
    }

    public static void initializeClient() {
        MenuType<StorageContainerMenu> storage = ModBlocks.STORAGE_CONTAINER_TYPE.get();
        MenuType<LimitedBarrelContainerMenu> limited = ModBlocks.LIMITED_BARREL_CONTAINER_TYPE.get();
        MenuType<StorageSettingsContainerMenu> settings = ModBlocks.SETTINGS_CONTAINER_TYPE.get();
        MenuType<LimitedBarrelSettingsContainerMenu> limitedSettings = ModBlocks.LIMITED_BARREL_SETTINGS_CONTAINER_TYPE.get();

        DesktopMenuFactories.register(storage, (minecraft, player, id, data) ->
            fromOpeningData(minecraft, data, buffer -> StorageContainerMenu.fromBuffer(id, player.getInventory(), buffer))
        );
        DesktopMenuFactories.register(limited, (minecraft, player, id, data) ->
            fromOpeningData(minecraft, data, buffer -> LimitedBarrelContainerMenu.fromBuffer(id, player.getInventory(), buffer))
        );
        DesktopMenuFactories.register(settings, (minecraft, player, id, data) ->
            fromOpeningData(minecraft, data, buffer -> StorageSettingsContainerMenu.fromBuffer(id, player.getInventory(), buffer))
        );
        DesktopMenuFactories.register(limitedSettings, (minecraft, player, id, data) ->
            fromOpeningData(minecraft, data, buffer -> LimitedBarrelSettingsContainerMenu.fromBuffer(id, player.getInventory(), buffer))
        );

        DesktopClientApi.registerWindow(
            storage,
            new SophisticatedHostedScreenDefinition<>(SophisticatedHostedScreenDefinition.LayoutKind.STANDARD, StorageScreen::constructScreen)
        );
        DesktopClientApi.registerWindow(
            limited,
            new SophisticatedHostedScreenDefinition<>(SophisticatedHostedScreenDefinition.LayoutKind.LIMITED_BARREL, LimitedBarrelScreen::new)
        );
        DesktopClientApi.registerWindow(
            settings,
            new SophisticatedHostedScreenDefinition<>(SophisticatedHostedScreenDefinition.LayoutKind.SETTINGS, StorageSettingsScreen::constructScreen)
        );
        DesktopClientApi.registerWindow(
            limitedSettings,
            new SophisticatedHostedScreenDefinition<>(SophisticatedHostedScreenDefinition.LayoutKind.LIMITED_BARREL_SETTINGS, LimitedBarrelSettingsScreen::new)
        );
    }

    private static void registerMain(MenuType<?> menuType) {
        SophisticatedCoreCompat.registerMainMenu(menuType);
        DesktopMenuBindings.register(
            menuType,
            menu -> ((StorageContainerMenuBase<?>) menu).getTotalSlotsNumber(),
            true,
            SophisticatedStorageCompat::storageSource,
            SophisticatedCoreCompat::prepareMainMenuSnapshot,
            SophisticatedCoreCompat.mainMenuGesturePolicy()
        );
    }

    private static void registerSettings(MenuType<?> menuType) {
        SophisticatedCoreCompat.registerSettingsMenu(menuType);
        DesktopMenuBindings.register(
            menuType,
            menu -> ((SettingsContainerMenu<?>) menu).getNumberOfSlots(),
            true,
            SophisticatedStorageCompat::storageSource,
            null,
            SophisticatedCoreCompat.settingsMenuGesturePolicy()
        );
    }

    private static String storageSource(net.minecraft.server.level.ServerPlayer player, AbstractContainerMenu menu, byte[] openingData) {
        // Source identity comes from the authenticated native server menu. Keep the native
        // opening bytes intact: newer releases append linked-storage state after the position.
        BlockPos pos;
        if (menu instanceof StorageContainerMenu storage) {
            pos = storage.getBlockPosition().orElseThrow(() -> new IllegalArgumentException("Storage has no block position"));
        } else if (menu instanceof StorageSettingsContainerMenu settings) {
            pos = settings.getBlockPosition();
        } else {
            throw new IllegalArgumentException("Unsupported native storage menu " + menu.getClass().getName());
        }
        return DesktopSessions.blockSourceKey(player, pos);
    }

    private static AbstractContainerMenu fromOpeningData(
        net.minecraft.client.Minecraft minecraft,
        byte[] data,
        java.util.function.Function<FriendlyByteBuf, AbstractContainerMenu> factory
    ) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        try {
            // Match the native factory contract. Limited barrels intentionally leave
            // optional storage fields unread; the authenticated payload remains intact.
            return factory.apply(buffer);
        } finally {
            buffer.release();
        }
    }

}
