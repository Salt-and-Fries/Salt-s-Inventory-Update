package com.salts_inventory_update.compat.sophisticated;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

import io.netty.buffer.Unpooled;

import com.salts_inventory_update.api.desktop.SaltsInventoryDesktopApi;
import com.salts_inventory_update.client.DesktopMenuFactories;
import com.salts_inventory_update.compat.sophisticated.client.SophisticatedHostedScreenDefinition;
import com.salts_inventory_update.internal.desktop.DesktopMenuSlots;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.client.gui.BackpackSettingsScreen;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackContainer;
import net.p3pp3rf1y.sophisticatedbackpacks.common.gui.BackpackSettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedbackpacks.init.ModItems;
import net.p3pp3rf1y.sophisticatedbackpacks.network.BackpackOpenPayload;
import net.p3pp3rf1y.sophisticatedbackpacks.network.MobCatcherReleasePayload;
import net.p3pp3rf1y.sophisticatedcore.common.gui.SettingsContainerMenu;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

public final class SophisticatedBackpacksCompat {
    private SophisticatedBackpacksCompat() {
    }

    public static void initializeCommon() {
        MenuType<BackpackContainer> backpack = ModItems.BACKPACK_CONTAINER_TYPE.get();
        MenuType<BackpackSettingsContainerMenu> settings = ModItems.SETTINGS_CONTAINER_TYPE.get();

        SophisticatedBackpackItemLocks.register(backpack, settings);

        SophisticatedCoreCompat.registerMainMenu(backpack);
        SophisticatedCoreCompat.registerSettingsMenu(settings);
        DesktopMenuSlots.register(
            backpack,
            menu -> ((StorageContainerMenuBase<?>) menu).getTotalSlotsNumber(),
            true,
            (player, menu, openingData) -> backpackSource(openingData),
            SophisticatedCoreCompat::prepareMainMenuSnapshot,
            SophisticatedCoreCompat.mainMenuGesturePolicy()
        );
        DesktopMenuSlots.register(
            settings,
            menu -> ((SettingsContainerMenu<?>) menu).getNumberOfSlots(),
            true,
            (player, menu, openingData) -> backpackSource(openingData),
            null,
            SophisticatedCoreCompat.settingsMenuGesturePolicy()
        );

        register(backpack, BackpackOpenPayload.TYPE.id(), BackpackOpenPayload.STREAM_CODEC, (context, payload) ->
            SophisticatedCoreCompat.runSessionAction(context, () ->
                BackpackOpenPayload.handlePayload(payload, SophisticatedCoreCompat.officialPayloadContext(context.player()))
            )
        );
        register(settings, BackpackOpenPayload.TYPE.id(), BackpackOpenPayload.STREAM_CODEC, (context, payload) ->
            SophisticatedCoreCompat.runSessionAction(context, () ->
                BackpackOpenPayload.handlePayload(payload, SophisticatedCoreCompat.officialPayloadContext(context.player()))
            )
        );
        register(backpack, MobCatcherReleasePayload.TYPE.id(), MobCatcherReleasePayload.STREAM_CODEC, (context, payload) ->
            SophisticatedCoreCompat.runSessionAction(context, () ->
                MobCatcherReleasePayload.handlePayload(payload, SophisticatedCoreCompat.officialPayloadContext(context.player()))
            )
        );
    }

    public static void initializeClient() {
        MenuType<BackpackContainer> backpack = ModItems.BACKPACK_CONTAINER_TYPE.get();
        MenuType<BackpackSettingsContainerMenu> settings = ModItems.SETTINGS_CONTAINER_TYPE.get();
        DesktopMenuFactories.register(backpack, (minecraft, player, containerId, data) ->
            fromOpeningData(minecraft, data, buffer -> BackpackContainer.fromBuffer(containerId, player.getInventory(), buffer))
        );
        DesktopMenuFactories.register(settings, (minecraft, player, containerId, data) ->
            fromOpeningData(minecraft, data, buffer -> BackpackSettingsContainerMenu.fromBuffer(containerId, player.getInventory(), buffer))
        );
        SaltsInventoryDesktopApi.registerClientWindow(
            backpack,
            new SophisticatedHostedScreenDefinition<>(SophisticatedHostedScreenDefinition.LayoutKind.STANDARD, BackpackScreen::constructScreen)
        );
        SaltsInventoryDesktopApi.registerClientWindow(
            settings,
            new SophisticatedHostedScreenDefinition<>(
                SophisticatedHostedScreenDefinition.LayoutKind.SETTINGS,
                (menu, inventory, title) -> BackpackSettingsScreen.constructScreen(menu, inventory, title)
            )
        );
    }

    private static String backpackSource(byte[] openingData) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(openingData);
            return "sophisticatedbackpacks:" + HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 unavailable", exception);
        }
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
                throw new IllegalArgumentException("Trailing backpack opening data: " + buffer.readableBytes());
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
