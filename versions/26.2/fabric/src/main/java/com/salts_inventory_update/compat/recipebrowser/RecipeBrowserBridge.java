package com.salts_inventory_update.compat.recipebrowser;

import java.util.EnumMap;
import java.util.Map;

public final class RecipeBrowserBridge {
    private static final RecipeBrowserAccess NOOP = new NoopRecipeBrowserAccess();
    private static final Map<RecipeBrowserSource, RecipeBrowserAccess> ACCESSES = new EnumMap<>(RecipeBrowserSource.class);
    private static volatile RecipeBrowserAccess access = NOOP;

    private RecipeBrowserBridge() {
    }

    public static RecipeBrowserAccess access() {
        return access;
    }

    public static void install(RecipeBrowserSource source, RecipeBrowserAccess nextAccess) {
        if (nextAccess == null) {
            ACCESSES.remove(source);
        } else {
            ACCESSES.put(source, nextAccess);
        }
        refresh();
    }

    public static void clear(RecipeBrowserSource source, RecipeBrowserAccess currentAccess) {
        if (ACCESSES.get(source) == currentAccess) {
            ACCESSES.remove(source);
            refresh();
        }
    }

    public static void clear(RecipeBrowserSource source) {
        ACCESSES.remove(source);
        refresh();
    }

    public static void clear() {
        ACCESSES.clear();
        access = NOOP;
    }

    private static void refresh() {
        RecipeBrowserAccess selected = null;
        for (RecipeBrowserAccess candidate : ACCESSES.values()) {
            if (candidate == null || !candidate.isAvailable()) {
                continue;
            }
            if (selected == null || candidate.source().priority() > selected.source().priority()) {
                selected = candidate;
            }
        }
        access = selected == null ? NOOP : selected;
    }
}
