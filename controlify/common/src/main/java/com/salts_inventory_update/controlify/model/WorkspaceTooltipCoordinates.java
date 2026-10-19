package com.salts_inventory_update.controlify.model;

/** Native tooltips are deferred until the window pose has been popped. Capture their screen coordinates. */
public final class WorkspaceTooltipCoordinates {
    private record Transform(double screenX, double screenY, double nativeX, double nativeY, double scale) { }
    private static final ThreadLocal<Transform> CURRENT = new ThreadLocal<>();

    private WorkspaceTooltipCoordinates() { }

    public static Scope enter(double screenX, double screenY, double nativeX, double nativeY, double scale) {
        Transform previous = CURRENT.get();
        CURRENT.set(new Transform(screenX, screenY, nativeX, nativeY, scale));
        return new Scope(previous);
    }

    public static int screenX(int nativeX) {
        Transform transform = CURRENT.get();
        return transform == null ? nativeX : (int) Math.round(transform.screenX + (nativeX - transform.nativeX) * transform.scale);
    }

    public static int screenY(int nativeY) {
        Transform transform = CURRENT.get();
        return transform == null ? nativeY : (int) Math.round(transform.screenY + (nativeY - transform.nativeY) * transform.scale);
    }

    public static final class Scope implements AutoCloseable {
        private final Transform previous;
        private boolean closed;
        private Scope(Transform previous) { this.previous = previous; }
        @Override public void close() {
            if (closed) return;
            closed = true;
            if (previous == null) CURRENT.remove();
            else CURRENT.set(previous);
        }
    }
}
