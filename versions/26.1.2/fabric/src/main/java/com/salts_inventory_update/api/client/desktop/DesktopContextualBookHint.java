package com.salts_inventory_update.api.client.desktop;

/** Cheap native bounds for a host-rendered contextual book hint; no recipe catalog is built. */
public record DesktopContextualBookHint(DesktopContextualMenu.Kind kind, DesktopWindowBounds activationBounds, DesktopWindowBounds iconBounds) { }
