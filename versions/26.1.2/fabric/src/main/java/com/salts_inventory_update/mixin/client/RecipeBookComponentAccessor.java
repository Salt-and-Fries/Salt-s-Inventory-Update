package com.salts_inventory_update.mixin.client;

import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import net.minecraft.client.gui.screens.recipebook.RecipeBookTabButton;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(RecipeBookComponent.class)
public interface RecipeBookComponentAccessor {
    @Accessor("searchBox")
    EditBox salts_inventory_update$getSearchBox();

    @Accessor("tabButtons")
    List<RecipeBookTabButton> salts_inventory_update$getTabButtons();

    @Accessor("filterButton")
    CycleButton<Boolean> salts_inventory_update$getFilterButton();

    @Accessor("recipeBookPage")
    RecipeBookPage salts_inventory_update$getRecipeBookPage();
}
