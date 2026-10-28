package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.client.automatic.AutomaticNativeScope;
import net.minecraft.client.gui.screens.recipebook.RecipeBookComponent;
import net.minecraft.client.gui.screens.recipebook.RecipeUpdateListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(RecipeBookComponent.class)
public abstract class AutomaticRecipeBookMixin {
    private boolean salts_inventory_update$automaticBook() {
        var scope = AutomaticNativeScope.current();
        return scope != null && scope.screen() instanceof com.salts_inventory_update.mixin.client.AutomaticRecipeScreenAccessor provider && provider.salts_inventory_update$recipeBook() == (Object)this;
    }
    @Inject(method = {"extractRenderState", "extractTooltip"}, at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$oneRecipePresentation(CallbackInfo ci) {
        if (salts_inventory_update$automaticBook()) ci.cancel();
    }
    @Inject(method = "isVisible", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$keepMachineVisible(CallbackInfoReturnable<Boolean> cir) {
        var scope = AutomaticNativeScope.current();
        if (scope != null && scope.rendering() != null && salts_inventory_update$automaticBook()) cir.setReturnValue(false);
    }
    @Inject(method = "mouseClicked", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$oneRecipeInput(CallbackInfoReturnable<Boolean> cir) {
        var scope = AutomaticNativeScope.current();
        if (scope != null && scope.input() != null && salts_inventory_update$automaticBook()) cir.setReturnValue(false);
    }
    @Inject(method = "tick", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$oneRecipeTick(CallbackInfo ci) {
        var scope = AutomaticNativeScope.current();
        if (scope != null && scope.owner().recipeManaged && salts_inventory_update$automaticBook()) ci.cancel();
    }
}
