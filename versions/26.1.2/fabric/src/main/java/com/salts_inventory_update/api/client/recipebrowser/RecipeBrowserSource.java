package com.salts_inventory_update.api.client.recipebrowser;

import java.util.Objects;

/** Stable provider identity and display priority; external browsers may define their own. */
public record RecipeBrowserSource(String id, String displayName, int priority) {
    public RecipeBrowserSource {
        Objects.requireNonNull(id, "id");
        Objects.requireNonNull(displayName, "displayName");
        if (id.isBlank()) throw new IllegalArgumentException("Browser provider id is blank");
    }
}
