package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.client.automatic.AutomaticNativeScope;
import net.minecraft.client.gui.components.Button;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Button.class)
public abstract class AutomaticRecipeButtonMixin {
    @Inject(method = "onPress", at = @At("HEAD"), cancellable = true)
    private void salts_inventory_update$desktopRecipeBook(CallbackInfo ci) {
        var scope = AutomaticNativeScope.current();
        if (scope != null && scope.input() != null && scope.owner().recipeButton == (Object)this) {
            scope.input().toggleRecipeBook(); ci.cancel();
        }
    }
}
