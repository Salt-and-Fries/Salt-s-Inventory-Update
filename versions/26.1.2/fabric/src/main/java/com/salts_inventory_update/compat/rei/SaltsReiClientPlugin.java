package com.salts_inventory_update.compat.rei;

import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserBridge;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserSource;
import me.shedaniel.rei.api.client.plugins.REIClientPlugin;
import me.shedaniel.rei.api.client.registry.entry.EntryRegistry;

public final class SaltsReiClientPlugin implements REIClientPlugin {
    private final RuntimeReiRecipeBrowserAccess access = new RuntimeReiRecipeBrowserAccess();

    public SaltsReiClientPlugin() {
        RecipeBrowserBridge.install(RecipeBrowserSource.REI, this.access);
    }

    @Override
    public void registerEntries(EntryRegistry registry) {
        RecipeBrowserBridge.install(RecipeBrowserSource.REI, this.access);
    }
}
