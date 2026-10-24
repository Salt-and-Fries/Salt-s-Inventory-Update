package com.salts_inventory_update.client.modmenu;

import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

/** Optional Fabric config entrypoint, owned by Core so it needs no extension. */
public final class SaltsInventoryModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return DesktopClientApi::createConfigScreen;
    }
}
