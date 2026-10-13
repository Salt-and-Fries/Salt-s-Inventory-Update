package com.salts_inventory_update.compat.ae2.menu;

import appeng.items.contents.NetworkToolMenuHost;

/** The fork's toolbox host getter, kept outside the native AE2 implementation. */
public interface DesktopToolboxAccess {
    NetworkToolMenuHost<?> salts$toolboxHost();
}
