package com.salts_inventory_update.compat.ae2;

import com.salts_inventory_update.compat.ae2.menu.DesktopMenuState;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

final class DesktopMenuActionTest {
    @Test void unmanagedMenuKeepsItsNativeTransport() {
        var state = new DesktopMenuState<String>();
        var nativePackets = new ArrayList<String>();
        state.send("config", nativePackets::add);
        state.flush(packet -> packet, packet -> new byte[0], (channel, data) -> fail("unmanaged queue"));
        assertEquals(List.of("config"), nativePackets);
    }

    @Test void managedMenuQueuesNativePacketsUntilAuthenticatedTransportIsAvailable() {
        var state = new DesktopMenuState<String>();
        state.identity(UUID.randomUUID());
        state.send("first", packet -> fail("native transport bypassed the window"));
        state.send("second", packet -> fail("native transport bypassed the window"));
        var sent = new ArrayList<String>();
        state.flush(packet -> "ae2:" + packet, packet -> packet.getBytes(StandardCharsets.UTF_8),
                (channel, data) -> sent.add(channel + "=" + new String(data, StandardCharsets.UTF_8)));
        state.flush(packet -> packet, packet -> new byte[0], (channel, data) -> fail("resent action"));
        assertEquals(List.of("ae2:first=first", "ae2:second=second"), sent);
    }

    @Test void closingMenuDiscardsUnsentActionsWithoutChangingAnotherMenusQueue() {
        var closed = new DesktopMenuState<String>();
        var live = new DesktopMenuState<String>();
        closed.identity(UUID.randomUUID());
        live.identity(UUID.randomUUID());
        closed.send("stale", ignored -> fail());
        live.send("live", ignored -> fail());
        closed.discardActions();
        closed.flush(packet -> packet, packet -> new byte[0], (channel, data) -> fail("closed action"));
        var sent = new ArrayList<String>();
        live.flush(packet -> packet, packet -> new byte[0], (channel, data) -> sent.add(channel));
        assertEquals(List.of("live"), sent);
    }

    @Test void excessiveActionsFailBeforeTheExistingQueueIsCorrupted() {
        var state = new DesktopMenuState<Integer>();
        state.identity(UUID.randomUUID());
        for (int i = 0; i < 256; i++) state.send(i, ignored -> fail());
        assertThrows(IllegalStateException.class, () -> state.send(256, ignored -> fail()));
        var sent = new ArrayList<Integer>();
        state.flush(packet -> packet, packet -> new byte[0], (channel, data) -> sent.add(channel));
        assertEquals(256, sent.size());
        assertEquals(0, sent.get(0));
        assertEquals(255, sent.get(255));
    }
}
