package com.salts_inventory_update.client;

import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;

/** Native geometry shared by rendering, hit testing and external presentation fitting. */
final class WorkspaceSurfaceGeometry {
    private WorkspaceSurfaceGeometry() { }

    static int compactInventoryHeight(int slotCount, int columns, int maxRows,
                                      int titleHeight, int padding, int slotSize, int hotbarReservedHeight) {
        int rows = Math.max(1, Math.min(maxRows, (slotCount + Math.max(1, columns) - 1) / Math.max(1, columns)));
        return titleHeight + padding * 2 + rows * slotSize + hotbarReservedHeight;
    }

    static DesktopWindowBounds union(DesktopWindowBounds first, DesktopWindowBounds second) {
        int left = Math.min(first.x(), second.x());
        int top = Math.min(first.y(), second.y());
        int right = Math.max(Math.addExact(first.x(), first.width()), Math.addExact(second.x(), second.width()));
        int bottom = Math.max(Math.addExact(first.y(), first.height()), Math.addExact(second.y(), second.height()));
        return new DesktopWindowBounds(left, top, Math.subtractExact(right, left), Math.subtractExact(bottom, top));
    }
}
