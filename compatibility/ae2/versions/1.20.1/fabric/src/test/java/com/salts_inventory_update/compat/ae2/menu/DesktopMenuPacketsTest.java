package com.salts_inventory_update.compat.ae2.menu;

import static org.junit.jupiter.api.Assertions.*;

import appeng.core.sync.BasePacketHandler.PacketTypes;
import appeng.core.sync.packets.ClearPatternAccessTerminalPacket;
import appeng.core.sync.packets.GuiDataSyncPacket;
import appeng.core.sync.packets.MEInteractionPacket;
import appeng.core.sync.packets.SwapSlotsPacket;
import appeng.helpers.InventoryAction;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DesktopMenuPacketsTest {
    @BeforeAll
    static void initializeNativeConfiguration(@TempDir java.nio.file.Path configDirectory) {
        // Native AE2 configuration queries the loader side even in these
        // headless codec tests. Supply a server-side test launcher only.
        if (net.fabricmc.loader.impl.launch.FabricLauncherBase.getLauncher() == null) {
            var launcher = (net.fabricmc.loader.impl.launch.FabricLauncher) java.lang.reflect.Proxy.newProxyInstance(
                    DesktopMenuPacketsTest.class.getClassLoader(),
                    new Class<?>[] { net.fabricmc.loader.impl.launch.FabricLauncher.class },
                    (proxy, method, arguments) -> {
                        if (method.getName().equals("getEnvironmentType")) return net.fabricmc.api.EnvType.SERVER;
                        if (method.getName().equals("isDevelopment")) return true;
                        throw new UnsupportedOperationException("Unexpected headless loader query: " + method);
                    });
            net.fabricmc.loader.impl.launch.FabricLauncherBase.setLauncher(launcher);
        }
        if (appeng.core.AEConfig.instance() == null) appeng.core.AEConfig.load(configDirectory);
    }

    @Test
    void nativeActionCodecPreservesPayloadAndDoesNotConsumeItsBuffer() {
        var packet = new SwapSlotsPacket(4, 9);
        int readerIndex = packet.getPayload().readerIndex();
        var bytes = DesktopMenuPackets.encodeAction(packet, null);
        assertEquals(readerIndex, packet.getPayload().readerIndex());
        var decoded = DesktopMenuPackets.decodeAction(DesktopMenuPackets.channel(packet), bytes, null);
        assertInstanceOf(SwapSlotsPacket.class, decoded);
        assertEquals(packet.getPacketID(), decoded.getPacketID());
        assertArrayEquals(bytes, DesktopMenuPackets.encodeAction(decoded, null));
        assertArrayEquals(bytes, DesktopMenuPackets.encodeAction(packet, null));
    }

    @Test
    void directionChannelAndSizeBoundsAreEnforcedBeforeNativeDispatch() {
        var action = new SwapSlotsPacket(4, 9);
        var update = new ClearPatternAccessTerminalPacket();
        var actionBytes = DesktopMenuPackets.encodeAction(action, null);
        assertThrows(IllegalArgumentException.class, () -> DesktopMenuPackets.encodeAction(update, null));
        assertThrows(IllegalArgumentException.class, () -> DesktopMenuPackets.encodeUpdate(action, null));
        assertThrows(IllegalArgumentException.class, () -> DesktopMenuPackets.decodeAction(
                DesktopMenuPackets.channel(update), actionBytes, null));
        assertThrows(IllegalArgumentException.class, () -> DesktopMenuPackets.decodeAction(
                DesktopMenuPackets.channel(action), Arrays.copyOf(actionBytes, 32769), null));
        assertThrows(IllegalArgumentException.class, () -> DesktopMenuPackets.decodeAction(
                DesktopMenuPackets.channel(action), new byte[3], null));
        byte[] wrongId = actionBytes.clone();
        wrongId[3] = (byte) PacketTypes.COMPASS_REQUEST.getPacketId();
        assertThrows(IllegalArgumentException.class, () -> DesktopMenuPackets.decodeAction(
                DesktopMenuPackets.channel(action), wrongId, null));
        assertThrows(IllegalArgumentException.class, () -> DesktopMenuPackets.decodeAction(
                DesktopMenuPackets.channel(action), Arrays.copyOf(actionBytes, actionBytes.length + 1), null));
    }

    @Test
    void opaqueGuiSyncDataStaysAvailableToTheNativeMenu() {
        var packet = new GuiDataSyncPacket(7, buffer -> buffer.writeInt(123456));
        byte[] bytes = DesktopMenuPackets.encodeUpdate(packet, null);
        var decoded = (GuiDataSyncPacket) DesktopMenuPackets.decodeUpdate(
                DesktopMenuPackets.channel(packet), bytes, null);
        try {
            assertEquals(123456, decoded.getData().readInt());
            assertFalse(decoded.getData().isReadable());
        } finally {
            decoded.getData().release();
        }
    }

    @Test
    void authenticatedSessionReaddressesOnlyTheNativeMenuId() {
        var packet = new MEInteractionPacket(7, 456789L, InventoryAction.PICKUP_OR_SET_DOWN);
        byte[] before = DesktopMenuPackets.encodeAction(packet, null);
        var inbound = DesktopMenuPackets.decodeAction(DesktopMenuPackets.channel(packet), before, null);
        var addressed = DesktopMenuPackets.forMenu(inbound, 42);
        byte[] after = DesktopMenuPackets.encodeAction(addressed, null);
        assertArrayEquals(Arrays.copyOfRange(before, 8, before.length), Arrays.copyOfRange(after, 8, after.length));
        assertEquals(42, java.nio.ByteBuffer.wrap(after).getInt(4));
        assertEquals(7, java.nio.ByteBuffer.wrap(before).getInt(4));
    }

    @Test
    void opaqueClientActionsKeepTheirNativeEncodingWhenReaddressed() {
        var outbound = new GuiDataSyncPacket(7, buffer -> {
            buffer.writeUtf("encodePattern");
            buffer.writeUtf("{\"amount\":12}");
        });
        var channel = DesktopMenuPackets.channel(outbound);
        byte[] original = DesktopMenuPackets.encodeAction(outbound, null);
        var inbound = (GuiDataSyncPacket) DesktopMenuPackets.decodeAction(channel, original, null);
        var addressed = (GuiDataSyncPacket) DesktopMenuPackets.forMenu(inbound, 42000);
        try {
            byte[] rewritten = DesktopMenuPackets.encodeAction(addressed, null);
            var encoded = new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(rewritten));
            try {
                encoded.readInt();
                assertEquals(42000, encoded.readVarInt());
                byte[] body = new byte[encoded.readableBytes()];
                encoded.readBytes(body);
                assertArrayEquals(Arrays.copyOfRange(original, 5, original.length), body);
            } finally {
                encoded.release();
            }
            assertEquals("encodePattern", addressed.getData().readUtf());
            assertEquals("{\"amount\":12}", addressed.getData().readUtf());
        } finally {
            inbound.getData().release();
            addressed.getData().release();
        }
    }

    @Test
    void nativeSyncBuffersAreReleasedWhenQueuedUpdatesCloseOrAreDelivered() {
        com.salts_inventory_update.compat.ae2.nativeview.NativeContentRegistry.clear();
        var outbound = new GuiDataSyncPacket(7, buffer -> buffer.writeInt(123456));
        var channel = DesktopMenuPackets.channel(outbound);
        byte[] wire = DesktopMenuPackets.encodeUpdate(outbound, null);
        var closed = (GuiDataSyncPacket) DesktopMenuPackets.decodeUpdate(channel, wire, null);
        var owner = java.util.UUID.randomUUID();
        com.salts_inventory_update.compat.ae2.nativeview.NativeContentRegistry.awaitOpening(owner, closed, wire.length);
        com.salts_inventory_update.compat.ae2.nativeview.NativeContentRegistry.remove(owner);
        assertEquals(0, closed.getData().refCnt());
        DesktopMenuPackets.release(closed); // Disposal is idempotent for adapter-owned packets.
        var delivered = (GuiDataSyncPacket) DesktopMenuPackets.decodeUpdate(channel, wire, null);
        var other = java.util.UUID.randomUUID();
        try {
            com.salts_inventory_update.compat.ae2.nativeview.NativeContentRegistry.register(other,
                    packet -> assertEquals(123456, ((GuiDataSyncPacket) packet).getData().readInt()));
            assertTrue(com.salts_inventory_update.compat.ae2.nativeview.NativeContentRegistry.receive(other, delivered));
            assertEquals(0, delivered.getData().refCnt());
        } finally {
            com.salts_inventory_update.compat.ae2.nativeview.NativeContentRegistry.clear();
        }
    }

    @Test
    void authenticatedActionScopeReleasesItsReaddressedBufferAfterHandlerFailure() {
        var outbound=new GuiDataSyncPacket(7,buffer -> buffer.writeUtf("encodePattern"));
        var channel=DesktopMenuPackets.channel(outbound);
        byte[] wire=DesktopMenuPackets.encodeAction(outbound,null);
        var handled=new java.util.concurrent.atomic.AtomicReference<GuiDataSyncPacket>();
        assertThrows(IllegalStateException.class, () -> DesktopMenuPackets.withMenuAction(channel,wire,42000,packet -> {
            var sync=(GuiDataSyncPacket)packet;handled.set(sync);
            assertEquals("encodePattern",sync.getData().readUtf());
            throw new IllegalStateException("native handler failed");
        }));
        assertNotNull(handled.get());assertEquals(0,handled.get().getData().refCnt());
        DesktopMenuPackets.release(handled.get());
    }
}
