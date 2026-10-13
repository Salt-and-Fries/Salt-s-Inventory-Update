package com.salts_inventory_update.api.client.recipebrowser;

public record RecipeBrowserEntry(String typeUid, Object type, Object ingredient, Object opaque) {
    public RecipeBrowserEntry(String typeUid, Object type, Object ingredient) {
        this(typeUid, type, ingredient, null);
    }
}
