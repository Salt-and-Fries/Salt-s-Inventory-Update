package com.salts_inventory_update.mixin.client;

import java.util.List;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(OverlayRecipeComponent.class)
public interface OverlayRecipeComponentAccessor {
    @Accessor("recipeButtons")
    List<AbstractWidget> salts_inventory_update$getRecipeButtons();

    @Accessor("x")
    int salts_inventory_update$getX();

    @Accessor("y")
    int salts_inventory_update$getY();
}
