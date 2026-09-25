package com.salts_inventory_update.compat.sophisticated.server;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import io.netty.buffer.Unpooled;

import com.salts_inventory_update.server.DesktopContainerSessions;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.p3pp3rf1y.sophisticatedcore.network.SyncAdditionalSlotInfoPayload;
import net.p3pp3rf1y.sophisticatedcore.network.SyncEmptySlotIconsPayload;
import net.p3pp3rf1y.sophisticatedcore.network.SyncSlotChangeErrorPayload;

/** Redirects menu-specific Sophisticated visual state to its authenticated detached Salt session. */
public final class SophisticatedServerPayloadBridge {
    private SophisticatedServerPayloadBridge() {
    }

    public static boolean capture(ServerPlayer player, CustomPacketPayload first, CustomPacketPayload[] additional) {
        int sessionId = DesktopContainerSessions.activeNetworkSessionId(player);
        if (sessionId < 0 || !canCapture(first)) {
            return false;
        }
        for (CustomPacketPayload payload : additional) {
            if (!canCapture(payload)) {
                return false;
            }
        }
        captureOne(player, sessionId, first);
        for (CustomPacketPayload payload : additional) {
            captureOne(player, sessionId, payload);
        }
        return true;
    }

    private static boolean canCapture(CustomPacketPayload payload) {
        String id = payload.type().id().toString();
        return id.equals("sophisticatedcore:sync_empty_slot_icons")
            || id.equals("sophisticatedcore:sync_additional_slot_info")
            || id.equals("sophisticatedcore:sync_slot_change_error")
            || id.equals("sophisticatedbackpacks:sync_client_info");
    }

    private static void captureOne(ServerPlayer player, int sessionId, CustomPacketPayload payload) {
        byte[] data;
        if (payload instanceof SyncEmptySlotIconsPayload value) {
            data = encode(player, value, SyncEmptySlotIconsPayload.STREAM_CODEC);
        } else if (payload instanceof SyncAdditionalSlotInfoPayload value) {
            data = encode(player, value, SyncAdditionalSlotInfoPayload.STREAM_CODEC);
        } else if (payload instanceof SyncSlotChangeErrorPayload value) {
            data = encode(player, value, SyncSlotChangeErrorPayload.STREAM_CODEC);
        } else {
            data = captureOptionalBackpack(player, payload);
        }
        if (!DesktopContainerSessions.sendSessionPayload(player, sessionId, payload.type().id(), data)) {
            throw new IllegalStateException("Detached Sophisticated session disappeared while sending " + payload.type().id());
        }
    }

    private static byte[] captureOptionalBackpack(ServerPlayer player, CustomPacketPayload payload) {
        try {
            Class<?> type = Class.forName("com.salts_inventory_update.compat.sophisticated.server.SophisticatedBackpackServerPayloads");
            Method method = type.getMethod("encode", ServerPlayer.class, CustomPacketPayload.class);
            return (byte[]) method.invoke(null, player, payload);
        } catch (ReflectiveOperationException exception) {
            Throwable cause = exception instanceof InvocationTargetException invocation && invocation.getCause() != null
                ? invocation.getCause()
                : exception;
            throw new IllegalStateException("Failed to encode optional backpack payload " + payload.type().id(), cause);
        }
    }

    public static <P> byte[] encode(ServerPlayer player, P payload, StreamCodec<? super RegistryFriendlyByteBuf, P> codec) {
        RegistryFriendlyByteBuf buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), player.registryAccess());
        try {
            codec.encode(buffer, payload);
            byte[] data = new byte[buffer.readableBytes()];
            buffer.getBytes(buffer.readerIndex(), data);
            return data;
        } finally {
            buffer.release();
        }
    }
}
