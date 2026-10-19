package com.salts_inventory_update.controlify.model;

import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static com.salts_inventory_update.controlify.model.LogicalMenuNavigation.Direction.*;

final class LogicalMenuNavigationTest {
    private static LogicalMenuNavigation.Target target(String group, int row, int column, double x, double y) {
        return new LogicalMenuNavigation.Target(group, row, column, x, y, 16, 16);
    }

    @Test void brewingBottlesFollowLogicalRowEvenWhenMiddleIsLower() {
        var left = target("brewing", 1, 0, 56, 51);
        var middle = target("brewing", 1, 1, 79, 58);
        var right = target("brewing", 1, 2, 102, 51);
        var points = List.of(left, middle, right, target("brewing", 0, 1, 79, 17));
        assertSame(middle, LogicalMenuNavigation.next(points, 56, 51, RIGHT));
        assertSame(right, LogicalMenuNavigation.next(points, 79, 58, RIGHT));
        assertSame(middle, LogicalMenuNavigation.next(points, 102, 51, LEFT));
        assertSame(points.get(3), LogicalMenuNavigation.next(points, 79, 58, UP));
    }

    @Test void shortRowsPreferTheNearestLogicalColumn() {
        var middle = target("crafting", 1, 1, 50, 50);
        var output = target("crafting", 1, 3, 125, 43);
        var points = List.of(middle, output, target("crafting", 0, 1, 50, 32));
        assertSame(output, LogicalMenuNavigation.next(points, 50, 50, RIGHT));
        assertSame(middle, LogicalMenuNavigation.next(points, 125, 43, LEFT));
    }

    @Test void cursorInsideAWideControlOwnsThatControl() {
        var search = new LogicalMenuNavigation.Target("guide", 0, 2, 90, 10, 80, 16);
        var recipe = target("guide", 1, 2, 60, 40);
        assertSame(recipe, LogicalMenuNavigation.next(List.of(search, recipe), 60, 10, DOWN));
    }

    @Test void separatePanelsAndWindowsUseGeometricFallbackAtTheirEdges() {
        var catalog = target("creative/catalog", 0, 0, 10, 30);
        var tab = target("creative/top-tabs", 0, 0, 10, 2);
        var other = target("chest/slots", 0, 0, 100, 30);
        assertSame(tab, LogicalMenuNavigation.next(List.of(catalog, tab, other), 10, 30, UP));
        assertSame(other, LogicalMenuNavigation.next(List.of(catalog, tab, other), 10, 30, RIGHT));
        assertSame(tab, LogicalMenuNavigation.next(List.of(catalog, tab, other), 10, 2, UP));
        assertNull(LogicalMenuNavigation.next(List.of(), 1, 2, DOWN));
    }

    @Test void emptySpaceAcquiresTheDesiredTargetBeforeAdvancingItsRow() {
        var first = target("guide", 0, 0, 30, 10);
        var next = target("guide", 0, 1, 60, 10);
        assertSame(first, LogicalMenuNavigation.next(List.of(first, next), 19, 10, RIGHT));
        assertSame(next, LogicalMenuNavigation.next(List.of(first, next), 30, 10, RIGHT));
        assertSame(first, LogicalMenuNavigation.next(List.of(first, next), 37, 10, LEFT));
    }
}
