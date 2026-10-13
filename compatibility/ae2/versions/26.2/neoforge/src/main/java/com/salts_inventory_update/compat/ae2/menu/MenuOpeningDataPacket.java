package com.salts_inventory_update.compat.ae2.menu;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;

import appeng.core.network.ClientboundPacket;
import appeng.core.network.CustomAppEngPayload;

public record MenuOpeningDataPacket(int containerId, Identifier menuType, byte[] data) implements ClientboundPacket {

    public static final Type<MenuOpeningDataPacket> TYPE = CustomAppEngPayload.createType("menu_opening_data");
    public static final StreamCodec<RegistryFriendlyByteBuf, MenuOpeningDataPacket> STREAM_CODEC = StreamCodec
            .ofMember(MenuOpeningDataPacket::write, MenuOpeningDataPacket::decode);

    public MenuOpeningDataPacket {
        if (data.length > 32768) {
            throw new IllegalArgumentException("AE menu opening data exceeds 32 KiB");
        }
        data = data.clone();
    }

    @Override
    public Type<MenuOpeningDataPacket> type() {
        return TYPE;
    }

    public static MenuOpeningDataPacket decode(RegistryFriendlyByteBuf buffer) {
        return new MenuOpeningDataPacket(buffer.readVarInt(), buffer.readIdentifier(), buffer.readByteArray(32768));
    }

    public void write(RegistryFriendlyByteBuf buffer) {
        buffer.writeVarInt(containerId);
        buffer.writeIdentifier(menuType);
        buffer.writeByteArray(data);
    }
}
