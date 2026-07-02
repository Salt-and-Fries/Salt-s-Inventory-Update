package com.salts_inventory_update.compat.jei;

public final class JeiDesktopBridge {
    private static final JeiDesktopAccess NOOP = new NoopJeiDesktopAccess();
    private static volatile JeiDesktopAccess access = NOOP;

    private JeiDesktopBridge() {
    }

    public static JeiDesktopAccess access() {
        return access;
    }

    public static void install(JeiDesktopAccess nextAccess) {
        access = nextAccess == null ? NOOP : nextAccess;
    }

    public static void clear(JeiDesktopAccess currentAccess) {
        if (access == currentAccess) {
            access = NOOP;
        }
    }

    public static void clear() {
        access = NOOP;
    }
}
