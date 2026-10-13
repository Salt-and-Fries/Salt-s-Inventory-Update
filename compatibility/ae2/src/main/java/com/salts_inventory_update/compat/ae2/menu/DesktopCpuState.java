package com.salts_inventory_update.compat.ae2.menu;

import java.nio.ByteBuffer;

/** Desktop-only status transport; AE2's ordinary GUI synchronization schema stays unchanged. */
public record DesktopCpuState(boolean ready, boolean available, boolean activeJob, int serial) {
    public byte[] encode() {
        int flags=(ready?1:0)|(available?2:0)|(activeJob?4:0);
        return ByteBuffer.allocate(5).put((byte)flags).putInt(serial).array();
    }
    public static DesktopCpuState decode(byte[] data) {
        if (data.length!=5) throw new IllegalArgumentException("Invalid desktop CPU state size");
        int flags=data[0]&255;
        if ((flags&~7)!=0) throw new IllegalArgumentException("Invalid desktop CPU state flags");
        return new DesktopCpuState((flags&1)!=0,(flags&2)!=0,(flags&4)!=0,ByteBuffer.wrap(data,1,4).getInt());
    }
}
