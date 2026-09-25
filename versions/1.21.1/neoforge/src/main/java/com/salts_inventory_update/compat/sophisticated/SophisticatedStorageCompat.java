package com.salts_inventory_update.compat.sophisticated;

import io.netty.buffer.Unpooled;

import com.salts_inventory_update.api.desktop.SaltsInventoryDesktopApi;
import com.salts_inventory_update.client.DesktopMenuFactories;
import com.salts_inventory_update.compat.sophisticated.client.SophisticatedHostedScreenDefinition;
import com.salts_inventory_update.internal.desktop.DesktopMenuSlots;
import com.salts_inventory_update.server.DesktopContainerSessions;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
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
import net.p3pp3rf1y.sophisticatedstorage.network.OpenStorageInventoryPayload;

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
        registerOpenStorage(settings);
        registerOpenStorage(limitedSettings);
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

        SaltsInventoryDesktopApi.registerClientWindow(
            storage,
            new SophisticatedHostedScreenDefinition<>(SophisticatedHostedScreenDefinition.LayoutKind.STANDARD, StorageScreen::constructScreen)
        );
        SaltsInventoryDesktopApi.registerClientWindow(
            limited,
            new SophisticatedHostedScreenDefinition<>(SophisticatedHostedScreenDefinition.LayoutKind.LIMITED_BARREL, LimitedBarrelScreen::new)
        );
        SaltsInventoryDesktopApi.registerClientWindow(
            settings,
            new SophisticatedHostedScreenDefinition<>(SophisticatedHostedScreenDefinition.LayoutKind.SETTINGS, StorageSettingsScreen::constructScreen)
        );
        SaltsInventoryDesktopApi.registerClientWindow(
            limitedSettings,
            new SophisticatedHostedScreenDefinition<>(SophisticatedHostedScreenDefinition.LayoutKind.LIMITED_BARREL_SETTINGS, LimitedBarrelSettingsScreen::new)
        );
    }

    private static void registerMain(MenuType<?> menuType) {
        SophisticatedCoreCompat.registerMainMenu(menuType);
        DesktopMenuSlots.register(
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
        DesktopMenuSlots.register(
            menuType,
            menu -> ((SettingsContainerMenu<?>) menu).getNumberOfSlots(),
            true,
            SophisticatedStorageCompat::storageSource,
            null,
            SophisticatedCoreCompat.settingsMenuGesturePolicy()
        );
    }

    private static String storageSource(net.minecraft.server.level.ServerPlayer player, AbstractContainerMenu menu, byte[] openingData) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(openingData));
        try {
            BlockPos pos = buffer.readBlockPos();
            if (buffer.isReadable()) {
                throw new IllegalArgumentException("Trailing storage opening data: " + buffer.readableBytes());
            }
            return DesktopContainerSessions.blockSourceKey(player, pos);
        } finally {
            buffer.release();
        }
    }

    private static void registerOpenStorage(MenuType<?> menuType) {
        register(menuType, OpenStorageInventoryPayload.TYPE.id(), OpenStorageInventoryPayload.STREAM_CODEC, (context, payload) -> {
            if (!(context.menu() instanceof StorageSettingsContainerMenu settings)
                || !settings.getBlockPosition().equals(payload.pos())) {
                return;
            }
            SophisticatedCoreCompat.runSessionAction(context, () ->
                OpenStorageInventoryPayload.handlePayload(payload, SophisticatedCoreCompat.officialPayloadContext(context.player()))
            );
        });
    }

    private static AbstractContainerMenu fromOpeningData(
        net.minecraft.client.Minecraft minecraft,
        byte[] data,
        java.util.function.Function<RegistryFriendlyByteBuf, AbstractContainerMenu> factory
    ) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), minecraft.player.registryAccess());
        try {
            AbstractContainerMenu menu = factory.apply(buffer);
            if (buffer.isReadable()) {
                throw new IllegalArgumentException("Trailing storage opening data: " + buffer.readableBytes());
            }
            return menu;
        } finally {
            buffer.release();
        }
    }

    private static <P> void register(
        MenuType<?> menuType,
        net.minecraft.resources.ResourceLocation channel,
        net.minecraft.network.codec.StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
        com.salts_inventory_update.api.server.desktop.DesktopTypedServerPayloadHandler<AbstractContainerMenu, P> handler
    ) {
        SophisticatedPayloadRegistration.register(menuType, channel, codec, handler);
    }
}
