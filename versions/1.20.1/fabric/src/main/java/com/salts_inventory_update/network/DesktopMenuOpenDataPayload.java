package com.salts_inventory_update.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import com.salts_inventory_update.protocol.DesktopProtocol;

/** NeoForge 26.2 transport; deliberately not registered by the shared/Fabric packet bootstrap. */
public record DesktopMenuOpenDataPayload(
    long connectionNonce,
    int sessionId,
    long sessionToken,
    int menuTypeId,
    int replacesSessionId,
    byte[] data
) implements DesktopPackets.DesktopPacket {
    public static final ResourceLocation TYPE = DesktopPackets.id("desktop_menu_open_data");

    public DesktopMenuOpenDataPayload(FriendlyByteBuf buf) {
        this(
            buf.readLong(),
            buf.readVarInt(),
            buf.readLong(),
            buf.readVarInt(),
            buf.readVarInt() - 1,
            readData(buf)
        );
    }

    public DesktopMenuOpenDataPayload {
        if (connectionNonce == 0L
            || sessionId <= DesktopPackets.PLAYER_MENU_SESSION
            || sessionToken == 0L
            || menuTypeId < 0
            || DesktopPackets.menuTypeById(menuTypeId) == null
            || replacesSessionId < -1
            || replacesSessionId == DesktopPackets.PLAYER_MENU_SESSION
            || replacesSessionId == sessionId
            || data == null
            || data.length > DesktopProtocol.MAX_CUSTOM_DATA_BYTES) {
            throw new IllegalArgumentException("Invalid desktop menu opening data");
        }
        data = data.clone();
    }

    @Override
    public byte[] data() {
        return this.data.clone();
    }

    private static byte[] readData(FriendlyByteBuf buf) {
        int length = buf.readVarInt();
        DesktopProtocol.requireCount("desktop menu opening data", length, DesktopProtocol.MAX_CUSTOM_DATA_BYTES);
        byte[] data = new byte[length];
        buf.readBytes(data);
        return data;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeLong(this.connectionNonce);
        buf.writeVarInt(this.sessionId);
        buf.writeLong(this.sessionToken);
        buf.writeVarInt(this.menuTypeId);
        buf.writeVarInt(this.replacesSessionId + 1);
        buf.writeVarInt(this.data.length);
        buf.writeBytes(this.data);
    }

    @Override
    public ResourceLocation id() {
        return TYPE;
    }
}
