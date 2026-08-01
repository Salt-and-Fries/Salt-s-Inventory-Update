package com.salts_inventory_update.compat.recipebrowser;

public record RecipeBrowserEntry(String typeUid, Object type, Object ingredient, Object opaque) {
    public RecipeBrowserEntry(String typeUid, Object type, Object ingredient) {
        this(typeUid, type, ingredient, null);
    }
}
