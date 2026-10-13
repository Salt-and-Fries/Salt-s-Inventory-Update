package com.salts_inventory_update.compat.ae2.menu;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Objects;

/** The fork's builder catalog, captured from AE2's original registration methods. */
public final class NativeMenuRegistry {
    private static final Map<Object, Object> BUILDERS = new IdentityHashMap<>();

    private NativeMenuRegistry() { }

    public static synchronized void capture(Object menuType, Object builder) {
        Objects.requireNonNull(menuType, "menuType");
        Objects.requireNonNull(builder, "builder");
        var previous = BUILDERS.putIfAbsent(menuType, builder);
        if (previous != null && previous != builder) {
            throw new IllegalStateException("AE2 menu type has multiple native builders");
        }
    }

    public static synchronized Map<Object, Object> builders() {
        return Collections.unmodifiableMap(new IdentityHashMap<>(BUILDERS));
    }
}
