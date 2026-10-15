package com.salts_inventory_update.controlify.render;

import java.util.IdentityHashMap;
import java.util.function.Consumer;

/** Captures a window's opacity before native queued rendering runs later in the frame. */
public final class WorkspaceOpacity {
    private static final ThreadLocal<Float> CURRENT = ThreadLocal.withInitial(() -> 1.0f);

    private WorkspaceOpacity() { }

    public static float current() { return CURRENT.get(); }

    /** Nested drawing opacity multiplies the enclosing window's opacity. */
    public static Scope enter(float opacity) { return restore(current() * normalize(opacity)); }

    private static Scope restore(float opacity) {
        float previous = current();
        CURRENT.set(opacity);
        return new Scope(previous);
    }

    public static int channel(int value, float opacity) {
        return Math.round(Math.max(0, Math.min(255, value)) * normalize(opacity));
    }

    public static int color(int argb, float opacity, boolean premultiplied) {
        int alpha = channel(argb >>> 24, opacity);
        int rgb = argb & 0x00ffffff;
        if (premultiplied) {
            rgb = channel((argb >>> 16) & 255, opacity) << 16
                    | channel((argb >>> 8) & 255, opacity) << 8 | channel(argb & 255, opacity);
        }
        return alpha << 24 | rgb;
    }

    private static float normalize(float opacity) {
        return Float.isFinite(opacity) ? Math.max(0, Math.min(1, opacity)) : 1;
    }

    public static final class Scope implements AutoCloseable {
        private final float previous;
        private boolean closed;
        private Scope(float previous) { this.previous = previous; }
        @Override public void close() {
            if (closed) return;
            closed = true;
            if (previous == 1) CURRENT.remove(); else CURRENT.set(previous);
        }
    }

    /** One store per native GUI render state; clear it when that frame is reset. */
    public static final class Capture {
        private final IdentityHashMap<Object, Float> values = new IdentityHashMap<>();

        public void record(Object state) {
            float opacity = current();
            if (opacity == 1) values.remove(state); else values.put(state, opacity);
        }

        public float opacity(Object state) { return values.getOrDefault(state, 1.0f); }

        /** Queued conversion restores the captured value, avoiding a second multiplication. */
        public <T> Consumer<T> wrap(Consumer<T> original) {
            return state -> {
                try (Scope ignored = restore(opacity(state))) { original.accept(state); }
            };
        }

        public void clear() { values.clear(); }
    }
}
