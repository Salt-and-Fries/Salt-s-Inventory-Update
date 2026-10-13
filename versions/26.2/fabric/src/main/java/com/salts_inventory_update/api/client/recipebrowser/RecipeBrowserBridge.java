package com.salts_inventory_update.api.client.recipebrowser;

import java.util.LinkedHashMap;
import java.util.Map;

public final class RecipeBrowserBridge {
    private static final RecipeBrowserAccess NOOP = new NoopRecipeBrowserAccess();
    private static final Map<RecipeBrowserSource, RecipeBrowserAccess> ACCESSES = new LinkedHashMap<>();
    private static volatile RecipeBrowserAccess access = NOOP;

    private RecipeBrowserBridge() {
    }

    public static synchronized RecipeBrowserAccess access() {
        refresh();
        return access;
    }

    public static synchronized void install(RecipeBrowserSource source, RecipeBrowserAccess nextAccess) {
        RecipeBrowserAccess previous = ACCESSES.get(source);
        com.salts_inventory_update.integration.DesktopRegistrationScope.onRollback(() -> {
            synchronized (RecipeBrowserBridge.class) {
                if (ACCESSES.get(source) != nextAccess) return;
                if (previous == null) ACCESSES.remove(source);
                else ACCESSES.put(source, previous);
                refresh();
            }
        });
        if (nextAccess == null) {
            ACCESSES.remove(source);
        } else {
            ACCESSES.put(source, nextAccess);
        }
        refresh();
    }

    public static synchronized void clear(RecipeBrowserSource source, RecipeBrowserAccess currentAccess) {
        if (ACCESSES.get(source) == currentAccess) {
            ACCESSES.remove(source);
            refresh();
        }
    }

    public static synchronized void clear(RecipeBrowserSource source) {
        ACCESSES.remove(source);
        refresh();
    }

    public static synchronized void clear() {
        ACCESSES.clear();
        access = NOOP;
    }

    private static void refresh() {
        RecipeBrowserAccess selected = null;
        for (RecipeBrowserAccess candidate : ACCESSES.values()) {
            if (candidate == null) continue;
            try {
                if (!candidate.isAvailable()) continue;
            } catch (RuntimeException | LinkageError failure) {
                com.salts_inventory_update.api.integration.DesktopDiagnostics.report(
                    com.salts_inventory_update.api.integration.DesktopDiagnostics.Level.WARNING,
                    "recipe-browser", "availability", "Provider unavailable; trying another provider", failure);
                continue;
            }
            if (selected == null || candidate.source().priority() > selected.source().priority()) {
                selected = candidate;
            }
        }
        access = selected == null ? NOOP : selected;
    }
}
