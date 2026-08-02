package com.salts_inventory_update.protocol;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/** Bounded undirected graph for server-authorized persistent window links. */
public final class BoundedLinkGraph<K> {
    private final int maximumNodes;
    private final int maximumEdges;
    private final LinkedHashMap<K, LinkedHashSet<K>> adjacency = new LinkedHashMap<>();
    private int edgeCount;

    public BoundedLinkGraph() {
        this(DesktopProtocol.MAX_LINK_NODES, DesktopProtocol.MAX_LINK_EDGES);
    }

    public BoundedLinkGraph(int maximumNodes, int maximumEdges) {
        if (maximumNodes < 2 || maximumEdges < 1) {
            throw new IllegalArgumentException("graph limits must be positive");
        }
        this.maximumNodes = maximumNodes;
        this.maximumEdges = maximumEdges;
    }

    public synchronized boolean link(K first, K second) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (first.equals(second)) {
            return false;
        }
        Set<K> firstEdges = adjacency.get(first);
        if (firstEdges != null && firstEdges.contains(second)) {
            return true;
        }
        int addedNodes = (adjacency.containsKey(first) ? 0 : 1) + (adjacency.containsKey(second) ? 0 : 1);
        if (adjacency.size() + addedNodes > maximumNodes || edgeCount >= maximumEdges) {
            return false;
        }
        adjacency.computeIfAbsent(first, ignored -> new LinkedHashSet<>()).add(second);
        adjacency.computeIfAbsent(second, ignored -> new LinkedHashSet<>()).add(first);
        edgeCount++;
        return true;
    }

    public synchronized boolean unlink(K first, K second) {
        Set<K> firstEdges = adjacency.get(first);
        Set<K> secondEdges = adjacency.get(second);
        if (firstEdges == null || secondEdges == null || !firstEdges.remove(second)) {
            return false;
        }
        secondEdges.remove(first);
        edgeCount--;
        removeIfIsolated(first);
        removeIfIsolated(second);
        return true;
    }

    public synchronized List<K> connectedComponent(K origin, int maximumResults) {
        if (maximumResults <= 0 || !adjacency.containsKey(origin)) {
            return List.of();
        }
        LinkedHashSet<K> visited = new LinkedHashSet<>();
        ArrayDeque<K> queue = new ArrayDeque<>();
        visited.add(origin);
        queue.add(origin);
        while (!queue.isEmpty() && visited.size() < maximumResults) {
            K current = queue.removeFirst();
            for (K neighbor : adjacency.getOrDefault(current, new LinkedHashSet<>())) {
                if (visited.add(neighbor)) {
                    queue.addLast(neighbor);
                    if (visited.size() == maximumResults) {
                        break;
                    }
                }
            }
        }
        return List.copyOf(visited);
    }

    public synchronized Map<K, Set<K>> snapshot() {
        LinkedHashMap<K, Set<K>> copy = new LinkedHashMap<>();
        adjacency.forEach((node, edges) -> copy.put(node, Collections.unmodifiableSet(new LinkedHashSet<>(edges))));
        return Collections.unmodifiableMap(copy);
    }

    public synchronized int nodeCount() {
        return adjacency.size();
    }

    public synchronized int edgeCount() {
        return edgeCount;
    }

    public synchronized void clear() {
        adjacency.clear();
        edgeCount = 0;
    }

    private void removeIfIsolated(K node) {
        Set<K> edges = adjacency.get(node);
        if (edges != null && edges.isEmpty()) {
            adjacency.remove(node);
        }
    }
}
