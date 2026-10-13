package com.salts_inventory_update.controlify.render;

/** Limits native GUI clipping adaptation to transformed controller window rendering. */
public final class WorkspaceRenderTransform {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);

    private WorkspaceRenderTransform() {}

    public static Scope enter() {
        DEPTH.set(DEPTH.get() + 1);
        return new Scope();
    }

    public static boolean active() {
        return DEPTH.get() > 0;
    }

    public static final class Scope implements AutoCloseable {
        private boolean closed;

        private Scope() {}

        @Override
        public void close() {
            if (closed) return;
            closed = true;
            int depth = DEPTH.get() - 1;
            if (depth == 0) DEPTH.remove();
            else DEPTH.set(depth);
        }
    }
}
