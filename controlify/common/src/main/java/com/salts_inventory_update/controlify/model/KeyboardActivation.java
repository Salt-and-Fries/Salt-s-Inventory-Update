package com.salts_inventory_update.controlify.model;

/** Retained or automatic editor focus must not turn an unrelated item click into a keyboard action. */
public final class KeyboardActivation {
    private KeyboardActivation() { }
    public static boolean shouldOpen(boolean wasEditing,boolean editing,boolean hasGeometry,boolean fieldHit) {
        return editing && (hasGeometry ? fieldHit : !wasEditing);
    }
}
