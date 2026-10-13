package com.salts_inventory_update.compat.ae2.menu;

import java.util.UUID;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;

/** AE-owned synchronization addressed to a particular detached menu lifetime. */
public record DesktopMenuUpdatePacket(UUID owner, int containerId, Identifier channel, byte[] data)
        implements
            ClientboundPacket {

    public static final Type<DesktopMenuUpdatePacket> TYPE = CustomAppEngPayload.createType("desktop_menu_update");
    public static final StreamCodec<RegistryFriendlyByteBuf, DesktopMenuUpdatePacket> STREAM_CODEC = StreamCodec
            .ofMember(DesktopMenuUpdatePacket::write, DesktopMenuUpdatePacket::decode);

    public DesktopMenuUpdatePacket {
        data = data.clone();
        if (data.length > 1048576) {
            throw new IllegalArgumentException("Desktop update is too large");
        }
    }

    @Override
    public Type<DesktopMenuUpdatePacket> type() {
        return TYPE;
    }

    public static DesktopMenuUpdatePacket decode(RegistryFriendlyByteBuf buffer) {
        return new DesktopMenuUpdatePacket(buffer.readUUID(), buffer.readVarInt(), buffer.readIdentifier(),
                buffer.readByteArray(1048576));
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeUUID(owner);
        buffer.writeVarInt(containerId);
        buffer.writeIdentifier(channel);
        buffer.writeByteArray(data);
    }
}
