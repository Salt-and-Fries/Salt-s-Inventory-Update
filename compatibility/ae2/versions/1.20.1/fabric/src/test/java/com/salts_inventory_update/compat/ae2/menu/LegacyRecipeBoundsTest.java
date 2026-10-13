package com.salts_inventory_update.compat.ae2.menu;

import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LegacyRecipeBoundsTest {
    @Test void nativeIntegerEncodingAcceptsOnlyTheForkGridBounds() {
        for (int count:new int[]{0,1,9,-1,10,Integer.MAX_VALUE}) {
            var buffer=new FriendlyByteBuf(Unpooled.buffer());
            try {
                buffer.writeInt(count);buffer.writeByte(42);
                if (count>=0&&count<=9) assertEquals(count,LegacyRecipeBounds.readSize(buffer));
                else assertThrows(IllegalArgumentException.class,() -> LegacyRecipeBounds.readSize(buffer));
                assertEquals(42,buffer.readUnsignedByte());
            } finally { buffer.release(); }
        }
    }
}
