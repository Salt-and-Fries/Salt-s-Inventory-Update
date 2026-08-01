package com.salts_inventory_update.compat.recipebrowser;

import java.util.List;

public record RecipeBrowserTransferPlan(RecipeBrowserTransferRect button, List<Integer> recipeSlotIds, List<RecipeBrowserTransferSlot> requirements) {
    public RecipeBrowserTransferPlan {
        recipeSlotIds = List.copyOf(recipeSlotIds);
        requirements = List.copyOf(requirements);
    }
}
