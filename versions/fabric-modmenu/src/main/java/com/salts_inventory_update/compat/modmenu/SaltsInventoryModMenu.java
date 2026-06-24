package com.salts_inventory_update.compat.modmenu;

import com.salts_inventory_update.client.WindowedInventoryClient;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class SaltsInventoryModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return WindowedInventoryClient::createConfigScreen;
    }
}
