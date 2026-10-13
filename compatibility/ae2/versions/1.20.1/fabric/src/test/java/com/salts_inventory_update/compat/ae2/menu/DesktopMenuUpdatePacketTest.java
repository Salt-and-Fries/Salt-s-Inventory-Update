package com.salts_inventory_update.compat.ae2.menu;

import static org.junit.jupiter.api.Assertions.*;

import io.netty.buffer.Unpooled;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

class DesktopMenuUpdatePacketTest {
    @Test void nativeUpdateEnvelopePreservesOwnerChannelAndDefensivePayloadCopies() {
        UUID owner = UUID.randomUUID();
        byte[] bytes = { 1, 2, 3 };
        var packet = new DesktopMenuUpdatePacket(owner, new ResourceLocation("ae2", "desktop/gui_data_sync"), bytes);
        bytes[0] = 9;
        packet.data()[1] = 9;
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.write(buffer);
            var received = DesktopMenuUpdatePacket.read(buffer);
            assertEquals(owner, received.owner());
            assertEquals(packet.channel(), received.channel());
            assertArrayEquals(new byte[] { 1, 2, 3 }, received.data());
        } finally { buffer.release(); }
    }

    @Test void largeNativeUpdatesFitTheLoaderPayloadAndMalformedTrailingDataIsRejected() {
        var packet = new DesktopMenuUpdatePacket(UUID.randomUUID(),
                new ResourceLocation("ae2", "desktop/me_inventory_update"),
                new byte[DesktopMenuUpdatePacket.MAX_DATA_BYTES]);
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.write(buffer);
            assertTrue(buffer.readableBytes() <= 1048576);
            buffer.writeByte(1);
            assertThrows(IllegalArgumentException.class, () -> DesktopMenuUpdatePacket.read(buffer));
            assertThrows(IllegalArgumentException.class, () -> new DesktopMenuUpdatePacket(packet.owner(), packet.channel(),
                    new byte[DesktopMenuUpdatePacket.MAX_DATA_BYTES + 1]));
        } finally { buffer.release(); }
    }
}
