package com.salts_inventory_update.compat.sophisticated;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;
import io.netty.buffer.Unpooled;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.PacketListener;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.desktop.DesktopPayloadLimits;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.api.server.desktop.DesktopServerPayloadContext;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import com.salts_inventory_update.compat.sophisticated.client.SophisticatedHostedScreenBridge;

/** Routes the Fabric port's existing GUI message codecs through authenticated desktop sessions. */
public final class SophisticatedLegacyMessageBridge {
    public static final ResourceLocation CHANNEL = new ResourceLocation("salts_inventory_compat", "sophisticated_fabric_message");
    private static final String CORE = "net.p3pp3rf1y.sophisticatedcore.";
    private static final String BACKPACKS = "net.p3pp3rf1y.sophisticatedbackpacks.network.";
    private static final String STORAGE = "net.p3pp3rf1y.sophisticatedstorage.network.";
    private static final Set<String> SERVER_MESSAGES = Set.of(
        CORE + "network.SyncContainerClientDataMessage", CORE + "network.TransferFullSlotMessage",
        CORE + "network.TransferItemsMessage", CORE + "upgrades.tank.TankClickMessage",
        CORE + "compat.jei.SetGhostSlotMessage", CORE + "compat.jei.SetMemorySlotMessage",
        CORE + "compat.emi.EmiSetGhostSlotMessage", CORE + "compat.emi.EmiSetMemorySlotMessage",
        CORE + "compat.rei.REISetGhostSlotMessage", CORE + "compat.rei.REISetMemorySlotMessage",
        BACKPACKS + "BackpackOpenMessage", BACKPACKS + "BackpackCloseMessage",
        STORAGE + "OpenStorageInventoryMessage");
    private static final Set<String> CLIENT_MESSAGES = Set.of(
        CORE + "network.SyncContainerStacksMessage", CORE + "network.SyncSlotStackMessage",
        CORE + "network.SyncPlayerSettingsMessage", CORE + "network.SyncTemplateSettingsMessage",
        CORE + "network.SyncAdditionalSlotInfoMessage", CORE + "network.SyncEmptySlotIconsMessage",
        CORE + "network.SyncSlotChangeErrorMessage", CORE + "network.SyncDatapackSettingsTemplateMessage",
        BACKPACKS + "SyncClientInfoMessage");
    private static final ThreadLocal<DesktopServerPayloadContext<AbstractContainerMenu>> SERVER_CONTEXT = new ThreadLocal<>();
    private SophisticatedLegacyMessageBridge() { }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public static void register(net.minecraft.world.inventory.MenuType<?> menuType) {
        DesktopServerApi.registerPayload((net.minecraft.world.inventory.MenuType) menuType,
            CHANNEL, SophisticatedLegacyMessageBridge::handleServer);
    }

    public static boolean sendToServer(Object message) {
        DesktopInputContext<?, ?> context = SophisticatedHostedScreenBridge.inputContext();
        return context != null && SERVER_MESSAGES.contains(message.getClass().getName())
            && context.sendPayload(CHANNEL, encode(message));
    }

    public static boolean sendToClient(ServerPlayer player, Object message) {
        if (!CLIENT_MESSAGES.contains(message.getClass().getName())) return false;
        DesktopServerPayloadContext<AbstractContainerMenu> context = SERVER_CONTEXT.get();
        if (context != null && context.player() == player) {
            context.sendToClient(CHANNEL, encode(message));
            return true;
        }
        int session = DesktopSessions.activeNetworkSessionId(player);
        return session > 0 && DesktopSessions.sendSessionPayload(player, session, CHANNEL, encode(message));
    }

    public static void handleClient(Minecraft minecraft, AbstractContainerMenu menu, ResourceLocation channel, byte[] data) {
        if (!CHANNEL.equals(channel) || minecraft.player == null || minecraft.getConnection() == null) return;
        AbstractContainerMenu previous = minecraft.player.containerMenu;
        minecraft.player.containerMenu = menu;
        try {
            decodeAndHandle(data, false, minecraft.getConnection(), null, minecraft.player);
        } finally {
            minecraft.player.containerMenu = previous;
        }
    }

    private static void handleServer(DesktopServerPayloadContext<AbstractContainerMenu> context) {
        DesktopServerPayloadContext<AbstractContainerMenu> previous = SERVER_CONTEXT.get();
        SERVER_CONTEXT.set(context);
        try {
            DesktopSessions.withSessionTransition(context.player(), context.sessionId(), () ->
                decodeAndHandle(context.data(), true, context.player().connection, context.player(), null));
            context.broadcastChanges();
        } finally {
            if (previous == null) SERVER_CONTEXT.remove(); else SERVER_CONTEXT.set(previous);
        }
    }

    private static byte[] encode(Object message) {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeUtf(message.getClass().getName(), 200);
            message.getClass().getMethod("write", FriendlyByteBuf.class).invoke(message, buffer);
            int size = buffer.readableBytes();
            if (size <= 0 || size > DesktopPayloadLimits.MAX_CUSTOM_DATA_BYTES)
                throw new IllegalArgumentException("Invalid Sophisticated GUI message size: " + size);
            byte[] data = new byte[size];
            buffer.getBytes(buffer.readerIndex(), data);
            return data;
        } catch (ReflectiveOperationException failure) {
            throw nativeFailure(failure);
        } finally { buffer.release(); }
    }

    private static void decodeAndHandle(byte[] data, boolean server, PacketListener listener, ServerPlayer sender, Player clientPlayer) {
        if (data.length == 0 || data.length > DesktopPayloadLimits.MAX_CUSTOM_DATA_BYTES)
            throw new IllegalArgumentException("Invalid Sophisticated GUI message size");
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        try {
            String name = buffer.readUtf(200);
            if (!(server ? SERVER_MESSAGES : CLIENT_MESSAGES).contains(name))
                throw new IllegalArgumentException("Unsupported Sophisticated GUI message: " + name);
            Class<?> type = Class.forName(name, false, SophisticatedLegacyMessageBridge.class.getClassLoader());
            Object message = type.getConstructor(FriendlyByteBuf.class).newInstance(buffer);
            if (buffer.isReadable()) throw new IllegalArgumentException("Trailing Sophisticated GUI message bytes");
            Class<?> contextType = Class.forName(CORE + "network.SimplePacketBase$Context");
            Object context = contextType.getConstructor(Executor.class, PacketListener.class, ServerPlayer.class, Player.class)
                .newInstance((Executor) Runnable::run, listener, sender, clientPlayer);
            type.getMethod("handle", contextType).invoke(message, context);
        } catch (ReflectiveOperationException failure) {
            throw nativeFailure(failure);
        } finally { buffer.release(); }
    }

    private static IllegalStateException nativeFailure(ReflectiveOperationException failure) {
        Throwable cause = failure instanceof InvocationTargetException invocation ? invocation.getCause() : failure;
        return new IllegalStateException("Native Sophisticated Fabric GUI message failed", cause);
    }
}
