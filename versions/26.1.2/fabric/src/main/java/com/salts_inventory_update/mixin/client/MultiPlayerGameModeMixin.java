package com.salts_inventory_update.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.crafting.display.RecipeDisplayId;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.client.InventoryDesktopScreen;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "handlePlaceRecipe", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$placeDesktopRecipe(int containerId, RecipeDisplayId recipeId, boolean useMaxItems, CallbackInfo ci) {
        if (InventoryDesktopScreen.interceptRecipeBookPlacement(Minecraft.getInstance(), containerId, recipeId, useMaxItems)) {
            ci.cancel();
        }
    }
}
