package com.salts_inventory_update.api.server.desktop;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import com.salts_inventory_update.integration.DesktopRegistrationScope;

import org.jspecify.annotations.Nullable;

import net.minecraft.resources.Identifier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

import com.salts_inventory_update.api.desktop.DesktopPayloadCodecs;

public final class DesktopServerApi {
    private static final Map<MenuType<?>, OpeningDataWriter<?>> OPENING_DATA = new LinkedHashMap<>();
    private static final Map<MenuType<?>, Map<Identifier, DesktopServerPayloadHandler<?>>> PAYLOAD_HANDLERS = new LinkedHashMap<>();
    private static final Map<MenuType<?>, Map<Identifier, DesktopServerPayloadHandler<?>>> NATIVE_PAYLOAD_HANDLERS = new LinkedHashMap<>();
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
        Identifier channel,
        DesktopServerPayloadHandler<T> handler
    ) {
        registerPayload(PAYLOAD_HANDLERS,menuType,channel,handler);
    }

    /** Adds an authenticated action to a native template without replacing its original opening transport. */
    public static synchronized <T extends AbstractContainerMenu> void registerNativePayload(
        MenuType<T> menuType, Identifier channel, DesktopServerPayloadHandler<T> handler
    ) {
        registerPayload(NATIVE_PAYLOAD_HANDLERS,menuType,channel,handler);
    }

    private static <T extends AbstractContainerMenu> void registerPayload(
        Map<MenuType<?>,Map<Identifier,DesktopServerPayloadHandler<?>>> registry,
        MenuType<T> menuType,Identifier channel,DesktopServerPayloadHandler<T> handler
    ) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(handler, "handler");
        if (findPayloadHandler(menuType,channel)!=null) {
            throw DesktopRegistrationScope.conflict("Server payload already registered for " + menuType + " / " + channel);
        }
        Map<Identifier, DesktopServerPayloadHandler<?>> handlers =
            registry.computeIfAbsent(menuType, ignored -> new LinkedHashMap<>());
        handlers.put(channel, handler);
        DesktopRegistrationScope.onRollback(() -> {
            synchronized (DesktopServerApi.class) {
                handlers.remove(channel, handler);
                if (handlers.isEmpty()) registry.remove(menuType, handlers);
            }
        });
    }

    public static synchronized <T extends AbstractContainerMenu, P> void registerPayload(
        MenuType<T> menuType,
        Identifier channel,
        StreamCodec<? super RegistryFriendlyByteBuf, P> codec,
        DesktopTypedServerPayloadHandler<T, P> handler
    ) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(channel, "channel");
        Objects.requireNonNull(codec, "codec");
        Objects.requireNonNull(handler, "handler");
        registerPayload(menuType, channel, context ->
            handler.handle(context, DesktopPayloadCodecs.decode(context.player().registryAccess(), codec, context.data()))
        );
    }

    @SuppressWarnings("unchecked")
    public static synchronized @Nullable DesktopServerPayloadHandler<AbstractContainerMenu> findPayloadHandler(MenuType<?> menuType, Identifier channel) {
        Map<Identifier, DesktopServerPayloadHandler<?>> handlers = PAYLOAD_HANDLERS.get(menuType);
        var handler=handlers==null?null:handlers.get(channel);
        if(handler==null){handlers=NATIVE_PAYLOAD_HANDLERS.get(menuType);handler=handlers==null?null:handlers.get(channel);}
        return (DesktopServerPayloadHandler<AbstractContainerMenu>) handler;
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
        void write(net.minecraft.server.level.ServerPlayer player, T menu, net.minecraft.network.RegistryFriendlyByteBuf buffer);
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
