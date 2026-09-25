package com.salts_inventory_update.compat.sophisticated;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.function.Supplier;

import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;
import net.p3pp3rf1y.sophisticatedcore.network.PacketHandler;

import com.salts_inventory_update.SaltsInventoryUpdate;
import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.desktop.SaltsInventoryDesktopApi;
import com.salts_inventory_update.api.server.desktop.DesktopServerPayloadContext;
import com.salts_inventory_update.compat.sophisticated.client.SophisticatedHostedScreenBridge;
import com.salts_inventory_update.protocol.DesktopProtocol;
import com.salts_inventory_update.server.DesktopContainerSessions;

/** Bridges Sophisticated's Forge-1.20 SimpleChannel messages through an authenticated Salt session. */
public final class SophisticatedLegacyMessageBridge {
    public static final ResourceLocation CHANNEL = new ResourceLocation(SaltsInventoryUpdate.MOD_ID, "sophisticated_legacy_message");
    private static final int MAX_HANDLER_NAME = 160;
    private static final ThreadLocal<DesktopServerPayloadContext<AbstractContainerMenu>> SERVER_CONTEXT = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> HANDLING = new ThreadLocal<>();
    private static final Field BY_TYPE = field("byType");
    private static final Field BY_ID = field("byId");

    private SophisticatedLegacyMessageBridge() {
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void register(net.minecraft.world.inventory.MenuType<?> menuType) {
        SaltsInventoryDesktopApi.registerServerPayload(
            (net.minecraft.world.inventory.MenuType) menuType,
            CHANNEL,
            SophisticatedLegacyMessageBridge::handleServer
        );
    }

    public static boolean sendToServer(PacketHandler handler, Object message) {
        DesktopInputContext<?, ?> context = SophisticatedHostedScreenBridge.inputContext();
        return context != null && context.sendPayload(CHANNEL, encode(handler, message));
    }

    public static boolean sendToClient(PacketHandler handler, ServerPlayer player, Object message) {
        DesktopServerPayloadContext<AbstractContainerMenu> context = SERVER_CONTEXT.get();
        if (context == null || context.player() != player) {
            int sessionId = DesktopContainerSessions.activeNetworkSessionId(player);
            return sessionId > 0 && DesktopContainerSessions.sendSessionPayload(player, sessionId, CHANNEL, encode(handler, message));
        }
        context.sendToClient(CHANNEL, encode(handler, message));
        return true;
    }

    public static boolean handlingMessage() {
        return Boolean.TRUE.equals(HANDLING.get());
    }

    public static CompletableFuture<Void> runImmediately(Runnable action) {
        action.run();
        return CompletableFuture.completedFuture(null);
    }

    public static void handleClient(Minecraft minecraft, AbstractContainerMenu menu, ResourceLocation channel, byte[] data) {
        if (!CHANNEL.equals(channel) || minecraft.player == null || minecraft.getConnection() == null) {
            return;
        }
        AbstractContainerMenu previous = minecraft.player.containerMenu;
        minecraft.player.containerMenu = menu;
        HANDLING.set(Boolean.TRUE);
        try {
            decodeAndHandle(data, minecraft.getConnection().getConnection(), NetworkDirection.PLAY_TO_CLIENT);
        } finally {
            HANDLING.remove();
            minecraft.player.containerMenu = previous;
        }
    }

    private static void handleServer(DesktopServerPayloadContext<AbstractContainerMenu> context) {
        ServerPlayer player = context.player();
        AbstractContainerMenu previous = player.containerMenu;
        player.containerMenu = context.menu();
        SERVER_CONTEXT.set(context);
        HANDLING.set(Boolean.TRUE);
        try {
            DesktopContainerSessions.withSessionTransition(
                player,
                context.sessionId(),
                () -> decodeAndHandle(context.data(), player.connection.connection, NetworkDirection.PLAY_TO_SERVER)
            );
            context.broadcastChanges();
        } finally {
            HANDLING.remove();
            SERVER_CONTEXT.remove();
            player.containerMenu = previous;
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static byte[] encode(PacketHandler handler, Object message) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            Object type = ((Map) get(BY_TYPE, handler)).get(message.getClass());
            if (type == null) {
                throw new IllegalArgumentException("Unregistered Sophisticated message " + message.getClass().getName());
            }
            String handlerName = handler.getClass().getName();
            validateHandlerName(handlerName);
            buffer.writeUtf(handlerName, MAX_HANDLER_NAME);
            buffer.writeVarInt((Integer) invoke(type, "id"));
            BiConsumer encoder = (BiConsumer) invoke(type, "encoder");
            encoder.accept(message, buffer);
            int length = buffer.readableBytes();
            if (length <= 0 || length > DesktopProtocol.MAX_CUSTOM_DATA_BYTES) {
                throw new IllegalArgumentException("Sophisticated message is too large: " + length);
            }
            byte[] result = new byte[length];
            buffer.getBytes(buffer.readerIndex(), result);
            return result;
        } finally {
            buffer.release();
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static void decodeAndHandle(byte[] data, Connection connection, NetworkDirection direction) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        try {
            String handlerName = buffer.readUtf(MAX_HANDLER_NAME);
            validateHandlerName(handlerName);
            PacketHandler handler = handler(handlerName);
            int id = buffer.readVarInt();
            Object type = ((Map) get(BY_ID, handler)).get(id);
            if (type == null) {
                throw new IllegalArgumentException("Unknown Sophisticated message id " + id);
            }
            Function decoder = (Function) invoke(type, "decoder");
            Object message = decoder.apply(buffer);
            if (buffer.isReadable()) {
                throw new IllegalArgumentException("Trailing Sophisticated message bytes: " + buffer.readableBytes());
            }
            BiConsumer handlerFunction = (BiConsumer) invoke(type, "handler");
            NetworkEvent.Context networkContext = context(connection, direction);
            handlerFunction.accept(message, (Supplier<NetworkEvent.Context>) () -> networkContext);
        } finally {
            buffer.release();
        }
    }

    private static PacketHandler handler(String name) {
        try {
            Class<?> type = Class.forName(name, false, SophisticatedLegacyMessageBridge.class.getClassLoader());
            Field instance = type.getField("INSTANCE");
            return (PacketHandler) instance.get(null);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to resolve Sophisticated packet handler " + name, exception);
        }
    }

    private static void validateHandlerName(String name) {
        if (!(name.equals("net.p3pp3rf1y.sophisticatedcore.network.PacketHandler")
            || name.equals("net.p3pp3rf1y.sophisticatedbackpacks.network.SBPPacketHandler")
            || name.equals("net.p3pp3rf1y.sophisticatedstorage.network.StoragePacketHandler"))) {
            throw new IllegalArgumentException("Unsupported Sophisticated packet handler " + name);
        }
    }

    private static NetworkEvent.Context context(Connection connection, NetworkDirection direction) {
        try {
            Constructor<NetworkEvent.Context> constructor = NetworkEvent.Context.class.getDeclaredConstructor(Connection.class, NetworkDirection.class, int.class);
            constructor.setAccessible(true);
            return constructor.newInstance(connection, direction, 0);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to create Sophisticated network context", exception);
        }
    }

    private static Field field(String name) {
        try {
            Field field = PacketHandler.class.getDeclaredField(name);
            field.setAccessible(true);
            return field;
        } catch (ReflectiveOperationException exception) {
            throw new ExceptionInInitializerError(exception);
        }
    }

    private static Object get(Field field, Object owner) {
        try {
            return field.get(owner);
        } catch (IllegalAccessException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private static Object invoke(Object owner, String methodName) {
        try {
            Method method = owner.getClass().getMethod(methodName);
            method.setAccessible(true);
            return method.invoke(owner);
        } catch (ReflectiveOperationException exception) {
            throw new IllegalStateException("Unable to access Sophisticated message " + methodName, exception);
        }
    }
}
