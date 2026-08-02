package com.salts_inventory_update.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DesktopConnectionStateTest {
    @Test
    void negotiatesAndSequencesUiMode() {
        DesktopConnectionState state = new DesktopConnectionState();
        state.begin(41L, 1_000L);

        assertTrue(state.acknowledge(
            DesktopProtocol.VERSION,
            41L,
            99L,
            DesktopProtocol.CAP_INVENTORY_TOPOLOGY | DesktopProtocol.CAP_LINK_GRAPH,
            false
        ));
        assertEquals(DesktopConnectionState.Phase.UI_DISABLED, state.phase());
        assertTrue(state.authorizes(99L, DesktopProtocol.CAP_INVENTORY_TOPOLOGY, false));
        assertFalse(state.authorizes(99L, DesktopProtocol.CAP_INVENTORY_TOPOLOGY, true));

        assertTrue(state.updateMode(99L, 1L, true));
        assertEquals(DesktopConnectionState.Phase.ACTIVE, state.phase());
        assertFalse(state.updateMode(99L, 1L, false));
        assertFalse(state.updateMode(100L, 2L, false));
        assertEquals(DesktopConnectionState.Phase.ACTIVE, state.phase());
    }

    @Test
    void rejectsProtocolAndNonceMismatch() {
        DesktopConnectionState state = new DesktopConnectionState();
        assertFalse(state.acknowledge(DesktopProtocol.VERSION, 0L, 7L, 0L, true));
        assertEquals(DesktopConnectionState.Phase.INCOMPATIBLE, state.phase());

        state.begin(5L, 0L);
        assertFalse(state.acknowledge(1, 5L, 7L, 0L, true));
        assertEquals(DesktopConnectionState.Phase.INCOMPATIBLE, state.phase());

        state.begin(6L, 0L);
        assertFalse(state.acknowledge(DesktopProtocol.VERSION, 5L, 7L, 0L, true));
        assertEquals(DesktopConnectionState.Phase.INCOMPATIBLE, state.phase());
    }

    @Test
    void timeoutDistinguishesPresentFromAbsentPeer() {
        DesktopConnectionState state = new DesktopConnectionState();
        state.begin(1L, 10L);
        assertFalse(state.expireIfNecessary(109L, true));
        assertTrue(state.expireIfNecessary(110L, true));
        assertEquals(DesktopConnectionState.Phase.INCOMPATIBLE, state.phase());

        state.begin(2L, 10L);
        assertTrue(state.expireIfNecessary(110L, false));
        assertEquals(DesktopConnectionState.Phase.UNNEGOTIATED, state.phase());
        assertFalse(state.expireIfNecessary(111L, false));
        assertFalse(state.acknowledge(DesktopProtocol.VERSION, 2L, 3L, 0L, true));
    }

    @Test
    void resetDropsConnectionAuthority() {
        DesktopConnectionState state = new DesktopConnectionState();
        state.begin(1L, 0L);
        assertTrue(state.acknowledge(DesktopProtocol.VERSION, 1L, 2L, DesktopProtocol.KNOWN_CAPABILITIES, true));
        state.reset();
        assertFalse(state.authorizes(2L, 0L, false));
        assertEquals(0L, state.connectionNonce());
    }

    @Test
    void failedBeginLeavesNoHandshakeOrPriorAuthority() {
        DesktopConnectionState state = new DesktopConnectionState();
        state.begin(1L, 0L);
        assertTrue(state.acknowledge(
            DesktopProtocol.VERSION,
            1L,
            2L,
            DesktopProtocol.KNOWN_CAPABILITIES,
            true
        ));

        assertThrows(
            ArithmeticException.class,
            () -> state.begin(3L, Long.MAX_VALUE - DesktopProtocol.HELLO_TIMEOUT_TICKS + 1L)
        );
        assertEquals(DesktopConnectionState.Phase.UNNEGOTIATED, state.phase());
        assertEquals(0L, state.connectionNonce());
        assertEquals(0L, state.capabilities());
        assertFalse(state.authorizes(2L, 0L, false));
        assertFalse(state.acknowledge(DesktopProtocol.VERSION, 3L, 4L, 0L, true));

        state.begin(5L, 0L);
        assertTrue(state.acknowledge(DesktopProtocol.VERSION, 5L, 6L, 0L, true));
        assertThrows(IllegalArgumentException.class, () -> state.begin(0L, 0L));
        assertEquals(DesktopConnectionState.Phase.UNNEGOTIATED, state.phase());
        assertEquals(0L, state.connectionNonce());
        assertFalse(state.authorizes(6L, 0L, false));
    }

    @Test
    void rejectedAcknowledgementClearsConnectionKeyAndCapabilities() {
        DesktopConnectionState state = new DesktopConnectionState();
        state.begin(1L, 0L);
        assertTrue(state.acknowledge(
            DesktopProtocol.VERSION,
            1L,
            2L,
            DesktopProtocol.KNOWN_CAPABILITIES,
            true
        ));

        assertFalse(state.acknowledge(DesktopProtocol.VERSION, 1L, 3L, 0L, true));
        assertEquals(DesktopConnectionState.Phase.INCOMPATIBLE, state.phase());
        assertEquals(0L, state.connectionNonce());
        assertEquals(0L, state.capabilities());
        assertFalse(state.authorizes(2L, 0L, false));

        state.begin(4L, 0L);
        assertFalse(state.acknowledge(DesktopProtocol.VERSION, 4L, 0L, 0L, true));
        assertEquals(DesktopConnectionState.Phase.INCOMPATIBLE, state.phase());

        state.begin(5L, 0L);
        long unknownCapability = Long.highestOneBit(~DesktopProtocol.KNOWN_CAPABILITIES);
        assertFalse(state.acknowledge(DesktopProtocol.VERSION, 5L, 6L, unknownCapability, true));
        assertEquals(DesktopConnectionState.Phase.INCOMPATIBLE, state.phase());
        assertEquals(0L, state.connectionNonce());
        assertEquals(0L, state.capabilities());
    }
}
