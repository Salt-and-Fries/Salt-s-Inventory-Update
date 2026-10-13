package com.salts_inventory_update.api.client.desktop;

/** An existing window lifetime exposed to an external workspace presentation. */
public record DesktopWorkspaceWindow(DesktopWindowHandle handle, Kind kind, String title,
                                     boolean closable) {
    public enum Kind { PLAYER_INVENTORY, CHARACTER, OTHER }
}
