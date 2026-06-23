package com.salts_inventory_update.api.server.desktop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class DesktopServerApi {
    private static final Map<MenuType<?>, Map<ResourceLocation, DesktopServerPayloadHandler<?>>> PAYLOAD_HANDLERS = new LinkedHashMap<>();
    private static final Map<MenuType<?>, DesktopServerWindowHandler<?, ?>> WINDOW_HANDLERS = new LinkedHashMap<>();

    private DesktopServerApi() {
    }

    public static synchronized <T extends AbstractContainerMenu, S> void registerWindow(
        MenuType<T> menuType,
        DesktopServerWindowHandler<T, S> handler
    ) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(handler, "handler");
        WINDOW_HANDLERS.put(menuType, handler);
    }

    public static synchronized <T extends AbstractContainerMenu> void registerPayload(
        MenuType<T> menuType,
        ResourceLocation channel,
        DesktopServerPayloadHandler<T> handler
    ) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(handler, "handler");
        PAYLOAD_HANDLERS.computeIfAbsent(menuType, ignored -> new LinkedHashMap<>()).put(channel, handler);
    }

    @SuppressWarnings("unchecked")
    public static synchronized @Nullable DesktopServerPayloadHandler<AbstractContainerMenu> findPayloadHandler(MenuType<?> menuType, ResourceLocation channel) {
        Map<ResourceLocation, DesktopServerPayloadHandler<?>> handlers = PAYLOAD_HANDLERS.get(menuType);
        if (handlers == null) {
            return null;
        }

        return (DesktopServerPayloadHandler<AbstractContainerMenu>) handlers.get(channel);
    }

    @SuppressWarnings("unchecked")
    public static synchronized @Nullable DesktopServerWindowHandler<AbstractContainerMenu, Object> findWindowHandler(MenuType<?> menuType) {
        return (DesktopServerWindowHandler<AbstractContainerMenu, Object>) WINDOW_HANDLERS.get(menuType);
    }

    public static synchronized boolean hasWindowSupport(MenuType<?> menuType) {
        return WINDOW_HANDLERS.containsKey(menuType) || PAYLOAD_HANDLERS.containsKey(menuType);
    }
}
