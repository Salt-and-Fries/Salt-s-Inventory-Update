package com.salts_inventory_update.compat.sophisticated;

import io.netty.buffer.Unpooled;

import com.salts_inventory_update.api.desktop.SaltsInventoryDesktopApi;
import com.salts_inventory_update.api.server.desktop.DesktopServerPayloadHandler;
import com.salts_inventory_update.api.server.desktop.DesktopTypedServerPayloadHandler;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Exact, bounded decoding for authenticated Sophisticated session packets. */
final class SophisticatedPayloadRegistration {
    private SophisticatedPayloadRegistration() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    static <P> void register(
        MenuType<?> menuType,
        ResourceLocation channel,
        StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
        DesktopTypedServerPayloadHandler<AbstractContainerMenu, P> handler
    ) {
        DesktopServerPayloadHandler<AbstractContainerMenu> exactHandler = context -> {
            RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer(context.data()),
                context.player().registryAccess()
            );
            try {
                P payload = codec.decode(buffer);
                if (buffer.isReadable()) {
                    throw new IllegalArgumentException(
                        "Trailing detached Sophisticated payload data for " + channel + ": " + buffer.readableBytes()
                    );
                }
                handler.handle(context, payload);
            } finally {
                buffer.release();
            }
        };
        SaltsInventoryDesktopApi.registerServerPayload(
            (MenuType<AbstractContainerMenu>) menuType,
            channel,
            exactHandler
        );
    }
}
