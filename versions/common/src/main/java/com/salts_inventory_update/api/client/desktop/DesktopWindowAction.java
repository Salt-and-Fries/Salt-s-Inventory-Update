package com.salts_inventory_update.api.client.desktop;

/** Requests made by user-facing mod controls; permissions are checked by the manager. */
public enum DesktopWindowAction {
    FOCUS, USER_CLOSE, PIN, UNPIN, MOVE, RESIZE, LOCK, UNLOCK,
    MANAGEMENT_MENU
}
