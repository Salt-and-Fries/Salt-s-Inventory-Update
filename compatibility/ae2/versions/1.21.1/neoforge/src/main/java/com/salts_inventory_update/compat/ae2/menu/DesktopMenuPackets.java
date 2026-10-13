package com.salts_inventory_update.compat.ae2.menu;

import java.util.LinkedHashMap;
import java.util.Map;

import io.netty.buffer.Unpooled;

import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.connection.ConnectionType;

import appeng.core.network.ClientboundPacket;
import appeng.core.network.ServerboundPacket;
import appeng.core.network.bidirectional.ConfigValuePacket;
import appeng.core.network.clientbound.ClearPatternAccessTerminalPacket;
import appeng.core.network.clientbound.CraftConfirmPlanPacket;
import appeng.core.network.clientbound.CraftingStatusPacket;
import appeng.core.network.clientbound.GuiDataSyncPacket;
import appeng.core.network.clientbound.MEInventoryUpdatePacket;
import appeng.core.network.clientbound.NetworkStatusPacket;
import appeng.core.network.clientbound.PatternAccessTerminalPacket;
import appeng.core.network.clientbound.SetLinkStatusPacket;
import appeng.core.network.serverbound.ConfigButtonPacket;
import appeng.core.network.serverbound.ConfirmAutoCraftPacket;
import appeng.core.network.serverbound.FillCraftingGridFromRecipePacket;
import appeng.core.network.serverbound.GuiActionPacket;
import appeng.core.network.serverbound.InventoryActionPacket;
import appeng.core.network.serverbound.MEInteractionPacket;
import appeng.core.network.serverbound.QuickMovePatternPacket;
import appeng.core.network.serverbound.SelectKeyTypePacket;
import appeng.core.network.serverbound.SwapSlotsPacket;
import appeng.core.network.serverbound.SwitchGuisPacket;
import appeng.core.network.serverbound.UpdateHoldingCtrlPacket;

/** Fork menu allowlists using only upstream AE2 codecs and handlers. World/tool packets are never desktop actions. */
public final class DesktopMenuPackets {
    private static final Map<ResourceLocation, StreamCodec<RegistryFriendlyByteBuf, ?>> ACTIONS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, StreamCodec<RegistryFriendlyByteBuf, ?>> UPDATES = new LinkedHashMap<>();

    static {
        action(ConfigButtonPacket.TYPE, ConfigButtonPacket.STREAM_CODEC);
        action(ConfirmAutoCraftPacket.TYPE, ConfirmAutoCraftPacket.STREAM_CODEC);
        action(FillCraftingGridFromRecipePacket.TYPE, FillCraftingGridFromRecipePacket.STREAM_CODEC);
        action(GuiActionPacket.TYPE, GuiActionPacket.STREAM_CODEC);
        action(InventoryActionPacket.TYPE, InventoryActionPacket.STREAM_CODEC);
        action(MEInteractionPacket.TYPE, MEInteractionPacket.STREAM_CODEC);
        action(QuickMovePatternPacket.TYPE, QuickMovePatternPacket.STREAM_CODEC);
        action(SelectKeyTypePacket.TYPE, SelectKeyTypePacket.STREAM_CODEC);
        action(SwapSlotsPacket.TYPE, SwapSlotsPacket.STREAM_CODEC);
        action(SwitchGuisPacket.TYPE, SwitchGuisPacket.STREAM_CODEC);
        action(UpdateHoldingCtrlPacket.TYPE, UpdateHoldingCtrlPacket.STREAM_CODEC);
        action(ConfigValuePacket.TYPE, ConfigValuePacket.STREAM_CODEC);
        update(ClearPatternAccessTerminalPacket.TYPE, ClearPatternAccessTerminalPacket.STREAM_CODEC);
        update(CraftConfirmPlanPacket.TYPE, CraftConfirmPlanPacket.STREAM_CODEC);
        update(CraftingStatusPacket.TYPE, CraftingStatusPacket.STREAM_CODEC);
        update(GuiDataSyncPacket.TYPE, GuiDataSyncPacket.STREAM_CODEC);
        update(MEInventoryUpdatePacket.TYPE, MEInventoryUpdatePacket.STREAM_CODEC);
        update(NetworkStatusPacket.TYPE, NetworkStatusPacket.STREAM_CODEC);
        update(PatternAccessTerminalPacket.TYPE, PatternAccessTerminalPacket.STREAM_CODEC);
        update(SetLinkStatusPacket.TYPE, SetLinkStatusPacket.STREAM_CODEC);
        update(ConfigValuePacket.TYPE, ConfigValuePacket.STREAM_CODEC);
    }

    private DesktopMenuPackets() {
    }

    private static <T extends ServerboundPacket> void action(CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        ACTIONS.put(type.id(), codec);
    }

    private static <T extends ClientboundPacket> void update(CustomPacketPayload.Type<T> type,
            StreamCodec<RegistryFriendlyByteBuf, T> codec) {
        UPDATES.put(type.id(), codec);
    }

    public static Iterable<ResourceLocation> actionChannels() {
        return ACTIONS.keySet();
    }

    public static byte[] encodeAction(ServerboundPacket packet, RegistryAccess registries) {
        return encode(ACTIONS, packet, registries, 32768);
    }

    public static ServerboundPacket decodeAction(ResourceLocation channel, byte[] data, RegistryAccess registries) {
        return (ServerboundPacket) decode(ACTIONS, channel, data, registries, 32768);
    }

    /** Embedded vanilla menu IDs are transport details; the authenticated desktop session selects the menu. */
    public static ServerboundPacket forMenu(ServerboundPacket packet, int containerId) {
        if (packet instanceof GuiActionPacket action) {
            return new GuiActionPacket(containerId, action.actionName(), action.jsonPayload());
        }
        if (packet instanceof MEInteractionPacket action) {
            return new MEInteractionPacket(containerId, action.serial(), action.action());
        }
        if (packet instanceof QuickMovePatternPacket action) {
            return new QuickMovePatternPacket(containerId, action.clickedSlot(), action.allowedPatternContainers());
        }
        return packet;
    }

    public static byte[] encodeUpdate(ClientboundPacket packet, RegistryAccess registries) {
        return encode(UPDATES, packet, registries, 1048576);
    }

    public static ClientboundPacket decodeUpdate(ResourceLocation channel, byte[] data, RegistryAccess registries) {
        return (ClientboundPacket) decode(UPDATES, channel, data, registries, 1048576);
    }

    @SuppressWarnings("unchecked")
    private static byte[] encode(Map<ResourceLocation, StreamCodec<RegistryFriendlyByteBuf, ?>> codecs,
            CustomPacketPayload packet, RegistryAccess registries, int limit) {
        var codec = (StreamCodec<RegistryFriendlyByteBuf, CustomPacketPayload>) codecs.get(packet.type().id());
        if (codec == null) {
            throw new IllegalArgumentException("Unsupported desktop packet: " + packet.type().id());
        }
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registries, ConnectionType.NEOFORGE);
        try {
            codec.encode(buffer, packet);
            if (buffer.readableBytes() > limit) {
                throw new IllegalArgumentException("Desktop packet exceeds " + limit + " bytes");
            }
            byte[] data = new byte[buffer.readableBytes()];
            buffer.readBytes(data);
            return data;
        } finally {
            buffer.release();
        }
    }

    private static Object decode(Map<ResourceLocation, StreamCodec<RegistryFriendlyByteBuf, ?>> codecs,
            ResourceLocation channel, byte[] data, RegistryAccess registries, int limit) {
        var codec = codecs.get(channel);
        if (codec == null || data.length > limit) {
            throw new IllegalArgumentException("Invalid desktop packet: " + channel);
        }
        var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), registries, ConnectionType.NEOFORGE);
        try {
            var result = codec.decode(buffer);
            if (buffer.isReadable()) {
                throw new IllegalArgumentException("Trailing desktop packet data");
            }
            return result;
        } finally {
            buffer.release();
        }
    }
}
