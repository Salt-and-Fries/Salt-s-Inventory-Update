package com.salts_inventory_update.compat.recipebrowser;

import com.salts_inventory_update.api.client.recipebrowser.RecipeBrowserSource;

public final class RecipeBrowserProviders {
    public static final RecipeBrowserSource JEI = new RecipeBrowserSource("jei", "JEI", 10);
    public static final RecipeBrowserSource REI = new RecipeBrowserSource("roughlyenoughitems", "REI", 20);
    public static final RecipeBrowserSource EMI = new RecipeBrowserSource("emi", "EMI", 30);
    private RecipeBrowserProviders() { }
}
