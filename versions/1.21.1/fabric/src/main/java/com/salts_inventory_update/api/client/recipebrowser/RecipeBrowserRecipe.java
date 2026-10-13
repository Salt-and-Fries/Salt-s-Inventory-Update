package com.salts_inventory_update.api.client.recipebrowser;

import org.jspecify.annotations.Nullable;

public record RecipeBrowserRecipe(String uid, String categoryUid, @Nullable String recipeId, int width, int height, Object recipe, Object layout) {
}
