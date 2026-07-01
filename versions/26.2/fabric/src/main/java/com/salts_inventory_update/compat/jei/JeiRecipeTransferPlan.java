package com.salts_inventory_update.compat.jei;

import java.util.List;

public record JeiRecipeTransferPlan(JeiRecipeTransferRect button, List<Integer> recipeSlotIds, List<JeiRecipeTransferSlot> requirements) {
    public JeiRecipeTransferPlan {
        recipeSlotIds = List.copyOf(recipeSlotIds);
        requirements = List.copyOf(requirements);
    }
}
