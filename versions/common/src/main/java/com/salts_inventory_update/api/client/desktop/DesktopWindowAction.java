package com.salts_inventory_update.api.client.desktop;

/** Requests made by user-facing mod controls; permissions are checked by the manager. */
public enum DesktopWindowAction {
    FOCUS, USER_CLOSE, MINIMIZE, RESTORE, PIN, UNPIN, GHOST, UNGHOST, MOVE, RESIZE, LOCK, UNLOCK,
    MANAGEMENT_MENU
}
