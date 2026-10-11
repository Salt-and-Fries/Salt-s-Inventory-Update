package com.salts_inventory_update.api.server.desktop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.salts_inventory_update.integration.DesktopRegistrationScope;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

public final class DesktopServerApi {
    private static final Map<MenuType<?>, OpeningDataWriter<?>> OPENING_DATA = new LinkedHashMap<>();
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
        if (WINDOW_HANDLERS.containsKey(menuType)) {
            throw DesktopRegistrationScope.conflict("Server window already registered for " + menuType);
        }
        replaceWindow(menuType, handler);
    }

    public static synchronized <T extends AbstractContainerMenu, S> void replaceWindow(
        MenuType<T> menuType, DesktopServerWindowHandler<T, S> handler
    ) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(handler, "handler");
        DesktopServerWindowHandler<?, ?> previous = WINDOW_HANDLERS.put(menuType, handler);
        DesktopRegistrationScope.onRollback(() -> {
            synchronized (DesktopServerApi.class) {
                if (WINDOW_HANDLERS.get(menuType) != handler) return;
                if (previous == null) WINDOW_HANDLERS.remove(menuType);
                else WINDOW_HANDLERS.put(menuType, previous);
            }
        });
    }

    public static synchronized <T extends AbstractContainerMenu> void registerPayload(
        MenuType<T> menuType,
        ResourceLocation channel,
        DesktopServerPayloadHandler<T> handler
    ) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(handler, "handler");
        Map<ResourceLocation, DesktopServerPayloadHandler<?>> handlers =
            PAYLOAD_HANDLERS.computeIfAbsent(menuType, ignored -> new LinkedHashMap<>());
        if (handlers.containsKey(channel)) {
            throw DesktopRegistrationScope.conflict("Server payload already registered for " + menuType + " / " + channel);
        }
        handlers.put(channel, handler);
        DesktopRegistrationScope.onRollback(() -> {
            synchronized (DesktopServerApi.class) {
                handlers.remove(channel, handler);
                if (handlers.isEmpty()) PAYLOAD_HANDLERS.remove(menuType, handlers);
            }
        });
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

    /** Serializes data for the client menu factory when normal menu opening is captured. */
    public static synchronized <T extends AbstractContainerMenu> void registerOpeningData(MenuType<T> type, OpeningDataWriter<T> writer) {
        Objects.requireNonNull(type, "type");
        Objects.requireNonNull(writer, "writer");
        if (OPENING_DATA.putIfAbsent(type, writer) != null) {
            throw DesktopRegistrationScope.conflict("Opening data writer already registered for " + type);
        }
        DesktopRegistrationScope.onRollback(() -> {
            synchronized (DesktopServerApi.class) { OPENING_DATA.remove(type, writer); }
        });
    }

    @SuppressWarnings("unchecked")
    public static synchronized @Nullable OpeningDataWriter<AbstractContainerMenu> findOpeningDataWriter(MenuType<?> type) {
        return (OpeningDataWriter<AbstractContainerMenu>) OPENING_DATA.get(type);
    }

    @FunctionalInterface
    public interface OpeningDataWriter<T extends AbstractContainerMenu> {
        void write(net.minecraft.server.level.ServerPlayer player, T menu, net.minecraft.network.FriendlyByteBuf buffer);
    }

    /** Checks both registration and mod-specific per-instance eligibility. */
    public static boolean supportsWindow(net.minecraft.server.level.ServerPlayer player, AbstractContainerMenu menu) {
        if (!hasWindowSupport(menu.getType())) return false;
        DesktopServerWindowHandler<AbstractContainerMenu, Object> handler = findWindowHandler(menu.getType());
        try {
            return handler == null || handler.supports(player, menu);
        } catch (RuntimeException failure) {
            com.salts_inventory_update.api.integration.DesktopDiagnostics.report(
                com.salts_inventory_update.api.integration.DesktopDiagnostics.Level.ERROR,
                String.valueOf(menu.getType()), "supports-window", "Eligibility check failed; using normal UI", failure);
            return false;
        }
    }

    public static synchronized boolean hasWindowSupport(MenuType<?> menuType) {
        return WINDOW_HANDLERS.containsKey(menuType) || PAYLOAD_HANDLERS.containsKey(menuType);
    }
}
