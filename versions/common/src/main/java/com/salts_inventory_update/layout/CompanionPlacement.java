package com.salts_inventory_update.layout;

import com.salts_inventory_update.api.client.desktop.DesktopWindowBounds;

/** Initial adjacency only. Ownership changes must never use this to reset a dragged companion. */
public final class CompanionPlacement {
    public static DesktopWindowBounds beside(DesktopWindowBounds owner, int width, int height, int viewportWidth, int viewportHeight) {
        long[][] positions = {
            {(long) owner.x() + owner.width() + 4, owner.y()},
            {(long) owner.x() - width - 4, owner.y()},
            {owner.x(), (long) owner.y() + owner.height() + 4},
            {owner.x(), (long) owner.y() - height - 4}
        };
        for (long[] position : positions) {
            if (position[0] >= 0 && position[1] >= 0 && (long) position[0] + width <= viewportWidth
                && (long) position[1] + height <= viewportHeight) {
                return new DesktopWindowBounds(Math.toIntExact(position[0]), Math.toIntExact(position[1]), width, height);
            }
        }
        return new DesktopWindowBounds(Math.toIntExact(Math.max(0, Math.min(positions[0][0], (long) viewportWidth - width))),
            Math.max(0, Math.min(owner.y(), viewportHeight - height)), width, height);
    }
    private CompanionPlacement() { }
}
