package com.salts_inventory_update.compat.rei;

import com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserBridge;
import com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserSource;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;
import me.shedaniel.rei.forge.REIPluginClient;

@REIPluginClient
public final class SaltsReiForgeClientPlugin implements REIClientPlugin {
    private final RuntimeReiRecipeBrowserAccess access = new RuntimeReiRecipeBrowserAccess();

    public SaltsReiForgeClientPlugin() {
        RecipeBrowserBridge.install(com.salts_inventory_update.compat.recipebrowser.RecipeBrowserProviders.REI, this.access);
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        RecipeBrowserBridge.install(com.salts_inventory_update.compat.recipebrowser.RecipeBrowserProviders.REI, this.access);
    }
}
