package com.salts_inventory_update.compat.ae2.menu;

import java.util.Arrays;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class DesktopCpuStateTest {
    @Test void allStatusFlagsAndNativeSerialValuesRoundTripThroughTheSessionPayload() {
        for (int flags=0;flags<8;flags++) for (int serial:new int[]{-1,0,42,Integer.MAX_VALUE}) {
            var value=new DesktopCpuState((flags&1)!=0,(flags&2)!=0,(flags&4)!=0,serial);
            assertEquals(value,DesktopCpuState.decode(value.encode()));
        }
    }
    @Test void malformedPayloadsCannotPartiallyApplyNativeStatusFields() {
        byte[] good=new DesktopCpuState(true,true,false,42).encode();
        assertThrows(IllegalArgumentException.class,() -> DesktopCpuState.decode(Arrays.copyOf(good,4)));
        assertThrows(IllegalArgumentException.class,() -> DesktopCpuState.decode(Arrays.copyOf(good,6)));
        good[0]=(byte)128;
        assertThrows(IllegalArgumentException.class,() -> DesktopCpuState.decode(good));
    }
}
