package com.salts_inventory_update.compat.modmenu;

import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;

public final class SaltsInventoryModMenu implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        return DesktopClientApi::createConfigScreen;
    }

    @Override
    public java.util.Map<String, ConfigScreenFactory<?>> getProvidedConfigScreenFactories() {
        return java.util.Map.of("salts_inventory_update", DesktopClientApi::createConfigScreen);
    }
}
