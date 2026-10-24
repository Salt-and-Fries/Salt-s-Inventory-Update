package com.salts_inventory_update.client;

/** Pure geometry for a window and its upper attachment. No inventory/model state is changed. */
public final class AttachedWindowLayout {
    private AttachedWindowLayout() { }
    public static Layout above(int x, int y, int width, int height, int preferredHeight,
                               int protrusion, int gap, int viewportWidth, int viewportHeight) {
        int body = Math.max(1, Math.min(preferredHeight, viewportHeight - height - protrusion - gap));
        int left = Math.max(0, Math.min(x, Math.max(0, viewportWidth - width)));
        int minY = body + protrusion + gap;
        int top = Math.max(minY, Math.min(y, Math.max(minY, viewportHeight - height)));
        return new Layout(left, top, left, top - gap - body, width, body);
    }
    public record Layout(int ownerX, int ownerY, int childX, int childY, int width, int childHeight) { }
}
