package com.salts_inventory_update.mixin.client;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.world.item.crafting.RecipeHolder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import com.salts_inventory_update.client.InventoryDesktopScreen;

@Mixin(MultiPlayerGameMode.class)
public abstract class MultiPlayerGameModeMixin {
    @Inject(method = "handleInventoryMouseClick", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$fullNativeClick(int menu, int slot, int button,
            net.minecraft.world.inventory.ClickType input, net.minecraft.world.entity.player.Player player, CallbackInfo ci) {
        if (com.salts_inventory_update.client.NativeContainerView.click(Minecraft.getInstance(), menu, slot, button, input)) ci.cancel();
    }

    @Inject(method = "handleInventoryButtonClick", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$nativeButton(int menu, int button, CallbackInfo ci) {
        var scope = com.salts_inventory_update.client.NativeContainerView.input();
        if (scope != null && scope.screen().getMenu().containerId == menu) {
            if (scope.context().inventoryAvailable())
                com.salts_inventory_update.client.DesktopContainerClient.clickButton(scope.sessionId(), button);
            ci.cancel();
        }
    }

    @Inject(method = "handlePlaceRecipe", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$placeDesktopRecipe(int containerId, RecipeHolder<?> recipe, boolean useMaxItems, CallbackInfo ci) {
        var scope = com.salts_inventory_update.client.NativeContainerView.input();
        if (com.salts_inventory_update.client.NativeContainerView.isActive(Minecraft.getInstance()) && scope != null
                && scope.screen().getMenu().containerId == containerId) {
            com.salts_inventory_update.client.DesktopContainerClient.placeRecipe(scope.sessionId(), recipe.id(), useMaxItems);
            ci.cancel(); return;
        }
        if (InventoryDesktopScreen.interceptRecipeBookPlacement(Minecraft.getInstance(), containerId, recipe.id(), useMaxItems)) {
            ci.cancel();
        }
    }
}
