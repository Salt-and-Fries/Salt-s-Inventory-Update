package com.salts_inventory_update.compat.jei;

public record JeiDesktopEntry(String typeUid, Object type, Object ingredient, Object opaque) {
    public JeiDesktopEntry(String typeUid, Object type, Object ingredient) {
        this(typeUid, type, ingredient, null);
    }
}
