package com.salts_inventory_update.protocol;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class BoundedLinkGraphTest {
    @Test
    void boundsNodesEdgesAndGroupTraversal() {
        BoundedLinkGraph<String> graph = new BoundedLinkGraph<>(3, 2);
        assertTrue(graph.link("player", "chest-a"));
        assertTrue(graph.link("chest-a", "chest-b"));
        assertFalse(graph.link("chest-b", "chest-c"));
        assertEquals(3, graph.nodeCount());
        assertEquals(2, graph.edgeCount());
        assertEquals(List.of("player", "chest-a", "chest-b"), graph.connectedComponent("player", 16));
        assertEquals(List.of("player", "chest-a"), graph.connectedComponent("player", 2));
    }

    @Test
    void unlinkPrunesIsolatedNodes() {
        BoundedLinkGraph<String> graph = new BoundedLinkGraph<>();
        graph.link("a", "b");
        assertTrue(graph.unlink("a", "b"));
        assertEquals(0, graph.nodeCount());
        assertEquals(0, graph.edgeCount());
    }

    @Test
    void duplicateLinksAreIdempotentAndReverseUnlinkCannotUnderflow() {
        BoundedLinkGraph<String> graph = new BoundedLinkGraph<>(2, 1);
        assertTrue(graph.link("a", "b"));
        assertTrue(graph.link("a", "b"));
        assertTrue(graph.link("b", "a"));
        assertEquals(2, graph.nodeCount());
        assertEquals(1, graph.edgeCount());

        assertTrue(graph.unlink("b", "a"));
        assertFalse(graph.unlink("a", "b"));
        assertFalse(graph.unlink("b", "a"));
        assertEquals(0, graph.nodeCount());
        assertEquals(0, graph.edgeCount());
    }

    @Test
    void snapshotIsDeeplyImmutableAndDetachedFromLaterChanges() {
        BoundedLinkGraph<String> graph = new BoundedLinkGraph<>();
        assertTrue(graph.link("a", "b"));
        Map<String, Set<String>> snapshot = graph.snapshot();

        assertThrows(UnsupportedOperationException.class, () -> snapshot.put("c", Set.of()));
        assertThrows(UnsupportedOperationException.class, () -> snapshot.get("a").add("c"));

        assertTrue(graph.link("b", "c"));
        assertEquals(Set.of("b"), snapshot.get("a"));
        assertEquals(Set.of("a"), snapshot.get("b"));
        assertFalse(snapshot.containsKey("c"));
    }
}
