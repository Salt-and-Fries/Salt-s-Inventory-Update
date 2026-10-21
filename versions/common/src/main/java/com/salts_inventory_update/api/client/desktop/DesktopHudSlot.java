package com.salts_inventory_update.api.client.desktop;

import java.util.Objects;

/**
 * One detached player HUD item. Transform maps its local 16x16 item origin to GUI coordinates.
 * The target is [-1,17) on each local axis. Hidden items are excluded from pointer hit testing.
 * This adapts Salt's overlays and interactions, not Minecraft's underlying HUD renderer.
 */
public record DesktopHudSlot(Kind kind, int index, DesktopTransform transform, boolean visible, boolean interactive) {
    public enum Kind { HOTBAR, OFFHAND }

    public DesktopHudSlot {
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(transform, "transform");
        if (index < 0 || index >= (kind == Kind.HOTBAR ? 9 : 1)) {
            throw new IllegalArgumentException("Invalid HUD slot index");
        }
        // Native pose stacks use floats. Reject values that cannot be represented safely there.
        double scale = transform.scale();
        for (double edge : new double[] { transform.screenX(-4), transform.screenX(32),
                transform.screenY(-4), transform.screenY(32) }) {
            if (!Double.isFinite(edge) || Math.abs(edge) > Integer.MAX_VALUE / 2.0) {
                throw new IllegalArgumentException("HUD transform exceeds GUI coordinate range");
            }
        }
        if ((float) scale == 0 || !Float.isFinite((float) scale)) {
            throw new IllegalArgumentException("HUD scale exceeds native pose range");
        }
    }

    public boolean contains(double screenX, double screenY) {
        double x = transform.localX(screenX), y = transform.localY(screenY);
        return visible && interactive && x >= -1 && x < 17 && y >= -1 && y < 17;
    }

    public DesktopHudSlot withTransform(DesktopTransform next) {
        return new DesktopHudSlot(kind, index, next, visible, interactive);
    }

    public DesktopHudSlot withVisibility(boolean next) {
        return new DesktopHudSlot(kind, index, transform, next, interactive);
    }

    public DesktopHudSlot withInteraction(boolean next) {
        return new DesktopHudSlot(kind, index, transform, visible, next);
    }

    /** Composes a screen-space transform after this slot's local transform. */
    public DesktopHudSlot transformed(DesktopTransform outer) {
        return withTransform(new DesktopTransform(outer.screenX(transform.x()), outer.screenY(transform.y()),
            outer.scale() * transform.scale()));
    }
}
