package com.salts_inventory_update.client;

import com.salts_inventory_update.network.NativeTemplateCapabilities;
import com.salts_inventory_update.protocol.DesktopProtocol;
import io.netty.buffer.Unpooled;
import java.util.Collections;
import java.util.List;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** The additive channel must retain its nonce and reject unbounded wire lists. */
class NativeTemplateCapabilitiesTest {
    @Test void roundTripRetainsNonceAndDeclaredFamilies() {
        var packet = new NativeTemplateCapabilities(1234567L, List.of("example:machine", "example:storage"));
        var buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            packet.write(buffer);
            assertEquals(packet, new NativeTemplateCapabilities(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally {
            buffer.release();
        }
    }

    @Test void rejectsNegativeAndExcessiveWireCountsBeforeAllocation() {
        for (int count : new int[]{-1, DesktopProtocol.MAX_SUPPORTED_MENU_IDS + 1}) {
            var buffer = new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeLong(1);
                buffer.writeVarInt(count);
                assertThrows(IllegalArgumentException.class, () -> new NativeTemplateCapabilities(buffer));
            } finally {
                buffer.release();
            }
        }
    }

    @Test void constructorCopiesAndBoundsDeclarations() {
        var ids = new java.util.ArrayList<>(List.of("example:machine"));
        var packet = new NativeTemplateCapabilities(1, ids);
        ids.clear();
        assertEquals(List.of("example:machine"), packet.menuIds());
        assertThrows(IllegalArgumentException.class, () -> new NativeTemplateCapabilities(1,
            Collections.nCopies(DesktopProtocol.MAX_SUPPORTED_MENU_IDS + 1, "example:machine")));
    }
}
