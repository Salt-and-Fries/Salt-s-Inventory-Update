package com.salts_inventory_update.controlify.model;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/** Immutable frame; the tuck placement positions the center of the reserved action indicator. */
public record WorkspaceLayout<K>(Map<K, WindowPlacement> windows, WindowPlacement tuck) {
    public WorkspaceLayout {
        windows = Collections.unmodifiableMap(new LinkedHashMap<>(Objects.requireNonNull(windows, "windows")));
        Objects.requireNonNull(tuck, "tuck");
    }
}
