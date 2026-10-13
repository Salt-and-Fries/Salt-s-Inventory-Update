package com.salts_inventory_update.compat.ae2.menu;

import com.salts_inventory_update.compat.ae2.nativeview.NativeContentRegistry;
import java.util.Objects;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

/** The fork's AE-owned, lifetime-addressed update envelope over each loader's native network. */
public record DesktopMenuUpdatePacket(UUID owner, ResourceLocation channel, byte[] data) {
    public static final ResourceLocation CHANNEL = new ResourceLocation("salts_inventory_compat", "ae2_desktop_update");
    public static final int MAX_DATA_BYTES = 1048576 - 256;

    public DesktopMenuUpdatePacket {
        Objects.requireNonNull(owner, "owner");
        Objects.requireNonNull(channel, "channel");
        data = data.clone();
        if (data.length > MAX_DATA_BYTES) throw new IllegalArgumentException("AE desktop update is too large");
    }

    @Override public byte[] data() { return data.clone(); }

    public void write(FriendlyByteBuf buffer) {
        buffer.writeUUID(owner);
        buffer.writeResourceLocation(channel);
        buffer.writeByteArray(data);
    }

    public static DesktopMenuUpdatePacket read(FriendlyByteBuf buffer) {
        var packet = new DesktopMenuUpdatePacket(buffer.readUUID(), buffer.readResourceLocation(),
                buffer.readByteArray(MAX_DATA_BYTES));
        if (buffer.isReadable()) throw new IllegalArgumentException("Trailing AE desktop update data");
        return packet;
    }

    public void receive() {
        var packet = DesktopMenuPackets.decodeUpdate(channel, data, null);
        if (!NativeContentRegistry.receive(owner, packet)) NativeContentRegistry.awaitOpening(owner, packet, data.length);
    }
}
