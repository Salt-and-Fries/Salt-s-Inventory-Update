package com.salts_inventory_update.compat.ae2.menu;

import appeng.core.sync.BasePacket;
import appeng.core.sync.BasePacketHandler.PacketTypes;
import appeng.core.sync.packets.GuiDataSyncPacket;
import appeng.core.sync.packets.MEInteractionPacket;
import io.netty.buffer.Unpooled;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** Desktop transport for AE2 15. Native AE2 owns every packet encoder and decoder. */
public final class DesktopMenuPackets {
    private static final Map<ResourceLocation, PacketTypes> ACTIONS = new LinkedHashMap<>();
    private static final Map<ResourceLocation, PacketTypes> UPDATES = new LinkedHashMap<>();
    // Native inbound constructors populate handler state, not an outbound
    // buffer. Preserve our validated wire image without retaining the packet.
    private static final Map<BasePacket, byte[]> DECODED_PAYLOADS = java.util.Collections.synchronizedMap(new WeakHashMap<>());

    static {
        action(PacketTypes.CONFIG_BUTTON);
        action(PacketTypes.CONFIRM_AUTO_CRAFT);
        action(PacketTypes.FILL_CRAFTING_GRID_FROM_RECIPE);
        action(PacketTypes.INVENTORY_ACTION);
        action(PacketTypes.ME_INTERACTION);
        action(PacketTypes.SWAP_SLOTS);
        action(PacketTypes.SWITCH_GUIS);
        action(PacketTypes.VALUE_CONFIG);
        action(PacketTypes.GUI_DATA_SYNC);
        update(PacketTypes.PATTERN_PROVIDER_CLEAR);
        update(PacketTypes.CRAFT_CONFIRM_PLAN);
        update(PacketTypes.CRAFTING_STATUS);
        update(PacketTypes.GUI_DATA_SYNC);
        update(PacketTypes.ME_INVENTORY_UPDATE);
        update(PacketTypes.NETWORK_STATUS);
        update(PacketTypes.PATTERN_PROVIDER_UPDATE);
        update(PacketTypes.VALUE_CONFIG);
    }

    private DesktopMenuPackets() { }

    private static ResourceLocation channel(PacketTypes type) {
        return new ResourceLocation("ae2", "desktop/" + type.name().toLowerCase(Locale.ROOT));
    }

    private static void action(PacketTypes type) { ACTIONS.put(channel(type), type); }
    private static void update(PacketTypes type) { UPDATES.put(channel(type), type); }

    public static ResourceLocation channel(BasePacket packet) {
        return channel(packetType(packet.getPacketID()));
    }

    public static Iterable<ResourceLocation> actionChannels() { return ACTIONS.keySet(); }

    public static byte[] encodeAction(BasePacket packet, RegistryAccess registries) {
        return encode(ACTIONS, packet, 32768);
    }

    public static BasePacket decodeAction(ResourceLocation channel, byte[] data, RegistryAccess registries) {
        return decode(ACTIONS, channel, data, 32768);
    }

    public static byte[] encodeUpdate(BasePacket packet, RegistryAccess registries) {
        return encode(UPDATES, packet, 1048576);
    }

    public static BasePacket decodeUpdate(ResourceLocation channel, byte[] data, RegistryAccess registries) {
        return decode(UPDATES, channel, data, 1048576);
    }

    /** Releases only packets decoded by this adapter; native AE2 packets remain native-owned. */
    public static void release(BasePacket packet) {
        if (DECODED_PAYLOADS.remove(packet) != null && packet instanceof GuiDataSyncPacket sync) {
            sync.getData().release();
        }
    }

    /** Owns decoded action buffers across readdressing and handler failures. */
    public static void withMenuAction(ResourceLocation channel, byte[] data, int containerId,
            java.util.function.Consumer<BasePacket> action) {
        var inbound=decodeAction(channel,data,null);
        BasePacket addressed=null;
        try {
            addressed=forMenu(inbound,containerId);
            action.accept(addressed);
        } finally {
            if (addressed!=inbound) release(addressed);
            release(inbound);
        }
    }

    /** Native menu IDs are readdressed only after the public API selects the authenticated session. */
    public static BasePacket forMenu(BasePacket packet, int containerId) {
        if (!(packet instanceof MEInteractionPacket) && !(packet instanceof GuiDataSyncPacket)) return packet;
        byte[] data = encodeAction(packet, null);
        var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        try {
            if (packet instanceof GuiDataSyncPacket) {
                buffer.readInt();
                buffer.readVarInt();
                // GUI sync uses a VarInt menu ID. The native encoder rebuilds
                // that header while the action/sync stream remains opaque.
                var addressed = new GuiDataSyncPacket(containerId,
                        output -> output.writeBytes(buffer, buffer.readerIndex(), buffer.readableBytes()));
                try { return decodeAction(channel(packet), encodeAction(addressed, null), null); }
                finally { NativePacketBytes.payload(addressed).release(); }
            }
            if (data.length < 2 * Integer.BYTES) throw new IllegalArgumentException("Missing AE2 menu ID");
            buffer.setInt(Integer.BYTES, containerId);
            // Reuse the native inbound constructor so its private handler
            // fields are initialized, including AE2's opaque client actions.
            return decodeAction(channel(packet), data, null);
        } finally {
            buffer.release();
        }
    }

    private static PacketTypes packetType(int id) {
        if (id < 0 || id >= PacketTypes.values().length) {
            throw new IllegalArgumentException("Invalid AE2 packet ID: " + id);
        }
        return PacketTypes.getPacket(id);
    }

    private static byte[] encode(Map<ResourceLocation, PacketTypes> allowed, BasePacket packet, int limit) {
        var type = packetType(packet.getPacketID());
        if (allowed.get(channel(type)) != type) {
            throw new IllegalArgumentException("Unsupported desktop packet: " + type);
        }
        byte[] received = DECODED_PAYLOADS.get(packet);
        if (received != null) {
            if (received.length > limit) throw new IllegalArgumentException("AE2 desktop packet exceeds its direction bound");
            return received.clone();
        }
        var buffer = NativePacketBytes.payload(packet);
        int size = buffer.readableBytes();
        if (size < Integer.BYTES || size > limit || buffer.getInt(buffer.readerIndex()) != type.getPacketId()) {
            throw new IllegalArgumentException("Invalid AE2 desktop packet size or ID");
        }
        byte[] data = new byte[size];
        // This is AE2's retained buffer; never advance or release it here.
        buffer.getBytes(buffer.readerIndex(), data);
        return data;
    }

    private static BasePacket decode(Map<ResourceLocation, PacketTypes> allowed,
            ResourceLocation channel, byte[] data, int limit) {
        var type = allowed.get(channel);
        if (type == null || data.length < Integer.BYTES || data.length > limit) {
            throw new IllegalArgumentException("Invalid desktop packet: " + channel);
        }
        var buffer = new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
        try {
            if (buffer.readInt() != type.getPacketId()) {
                throw new IllegalArgumentException("AE2 desktop channel and packet ID disagree");
            }
            var packet = type.parsePacket(buffer);
            // AE2's GUI sync constructor deliberately copies the remaining
            // opaque sync stream. Its native menu consumes that copy later.
            if (!(packet instanceof GuiDataSyncPacket) && buffer.isReadable()) {
                throw new IllegalArgumentException("Trailing desktop packet data");
            }
            DECODED_PAYLOADS.put(packet, data.clone());
            return packet;
        } finally {
            buffer.release();
        }
    }
}
