package com.salts_inventory_update.api.server.desktop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class DesktopServerApi {
    private static final Map<MenuType<?>, Map<Identifier, DesktopServerPayloadHandler<?>>> PAYLOAD_HANDLERS = new LinkedHashMap<>();

    private DesktopServerApi() {
    }

    public static synchronized <T extends AbstractContainerMenu> void registerPayload(
        MenuType<T> menuType,
        Identifier channel,
        DesktopServerPayloadHandler<T> handler
    ) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(handler, "handler");
        PAYLOAD_HANDLERS.computeIfAbsent(menuType, ignored -> new LinkedHashMap<>()).put(channel, handler);
    }

    @SuppressWarnings("unchecked")
    public static synchronized @Nullable DesktopServerPayloadHandler<AbstractContainerMenu> findPayloadHandler(MenuType<?> menuType, Identifier channel) {
        Map<Identifier, DesktopServerPayloadHandler<?>> handlers = PAYLOAD_HANDLERS.get(menuType);
        if (handlers == null) {
            return null;
        }

        return (DesktopServerPayloadHandler<AbstractContainerMenu>) handlers.get(channel);
    }
}
