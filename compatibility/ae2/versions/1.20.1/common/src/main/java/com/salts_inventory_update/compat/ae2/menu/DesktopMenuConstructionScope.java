package com.salts_inventory_update.compat.ae2.menu;

/** A missing detached host must not close another native menu; ordinary opening stays native. */
public final class DesktopMenuConstructionScope {
    private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);
    private DesktopMenuConstructionScope() { }
    public static boolean detached() { return DEPTH.get() > 0; }
    public static int begin() {
        int previous = DEPTH.get();
        DEPTH.set(Math.addExact(previous, 1));
        return previous;
    }
    public static void restore(int previous) {
        if (previous < 0 || DEPTH.get() != previous + 1) throw new IllegalStateException("Unbalanced AE desktop construction scope");
        if (previous == 0) DEPTH.remove();
        else DEPTH.set(previous);
    }
}
