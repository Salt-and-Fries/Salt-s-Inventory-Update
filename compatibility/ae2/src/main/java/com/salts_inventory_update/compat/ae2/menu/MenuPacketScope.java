package com.salts_inventory_update.compat.ae2.menu;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.BiConsumer;
import java.util.function.Supplier;

/**
 * Main-thread scope for AE2's reusable widgets. This is the fork's scoped packet
 * routing, with native menu and packet types supplied by the loader adapter.
 * The fallback always invokes AE2's own ordinary packet transport.
 */
public final class MenuPacketScope {
    private static final ThreadLocal<Object> CURRENT = new ThreadLocal<>();

    private MenuPacketScope() { }

    public static Object current() {
        return CURRENT.get();
    }

    public static <T> T call(Object menu, Supplier<T> callback) {
        Objects.requireNonNull(menu, "menu");
        var previous = CURRENT.get();
        CURRENT.set(menu);
        try {
            return callback.get();
        } finally {
            if (previous == null) {
                CURRENT.remove();
            } else {
                CURRENT.set(previous);
            }
        }
    }

    public static void run(Object menu, Runnable callback) {
        call(menu, () -> {
            callback.run();
            return null;
        });
    }

    public static <P> void send(P packet, BiConsumer<Object, P> scopedTransport, Consumer<P> nativeTransport) {
        var menu = CURRENT.get();
        if (menu == null) {
            nativeTransport.accept(packet);
        } else {
            scopedTransport.accept(menu, packet);
        }
    }
}
