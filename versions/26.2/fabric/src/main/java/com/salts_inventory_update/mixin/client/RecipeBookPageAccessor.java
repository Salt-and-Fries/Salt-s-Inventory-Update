package com.salts_inventory_update.mixin.client;

import java.util.function.Consumer;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.ImageButton;
import net.minecraft.client.gui.screens.recipebook.OverlayRecipeComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeBookPage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(RecipeBookPage.class)
public interface RecipeBookPageAccessor {
    @Invoker("listButtons")
    void salts_inventory_update$listButtons(Consumer<AbstractWidget> visitor);

    @Accessor("overlay")
    OverlayRecipeComponent salts_inventory_update$getOverlay();

    @Accessor("forwardButton")
    ImageButton salts_inventory_update$getForwardButton();

    @Accessor("backButton")
    ImageButton salts_inventory_update$getBackButton();
}
