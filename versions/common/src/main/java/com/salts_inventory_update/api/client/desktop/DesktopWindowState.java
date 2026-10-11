package com.salts_inventory_update.api.client.desktop;

import java.util.UUID;

/** Detached immutable state snapshot, safe to retain. No live menu or widget references. */
public record DesktopWindowState(UUID instanceId, boolean open, int x, int y, int width, int height,
                                 boolean minimized, boolean pinned, boolean ghosted, boolean locked,
                                 boolean keyboardFocused, boolean inventoryFocused, boolean visible) { }
