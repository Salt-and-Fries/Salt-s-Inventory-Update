package com.salts_inventory_update.compat.emi;

import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserBridge;
import com.salts_inventory_update.compat.recipebrowser.RecipeBrowserSource;
import dev.emi.emi.api.EmiPlugin;
import dev.emi.emi.api.EmiRegistry;

public final class SaltsEmiClientPlugin implements EmiPlugin {
    private final RuntimeEmiRecipeBrowserAccess access = new RuntimeEmiRecipeBrowserAccess();

    public SaltsEmiClientPlugin() {
        RecipeBrowserBridge.install(RecipeBrowserSource.EMI, this.access);
    }

    @Override
    public void register(EmiRegistry registry) {
        this.access.invalidateCaches();
        RecipeBrowserBridge.install(RecipeBrowserSource.EMI, this.access);
    }
}
