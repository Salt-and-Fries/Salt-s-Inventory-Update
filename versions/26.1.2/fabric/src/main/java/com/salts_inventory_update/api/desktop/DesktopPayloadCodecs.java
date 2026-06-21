package com.salts_inventory_update.api.desktop;

import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public final class DesktopPayloadCodecs {
    private DesktopPayloadCodecs() {
    }

    public static <P> byte[] encode(RegistryAccess registryAccess, StreamCodec<? super RegistryFriendlyByteBuf, P> codec, P payload) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess);
        codec.encode(buf, payload);
        byte[] data = new byte[buf.readableBytes()];
        buf.readBytes(data);
        return data;
    }

    public static <P> P decode(RegistryAccess registryAccess, StreamCodec<? super RegistryFriendlyByteBuf, P> codec, byte[] data) {
        RegistryFriendlyByteBuf buf = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(data), registryAccess);
        return codec.decode(buf);
    }
}
