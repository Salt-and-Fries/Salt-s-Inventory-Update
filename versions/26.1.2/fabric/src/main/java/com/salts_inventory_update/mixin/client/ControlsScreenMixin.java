package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.client.WindowMovementControls;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.client.gui.screens.options.controls.ControlsScreen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ControlsScreen.class)
public abstract class ControlsScreenMixin extends OptionsSubScreen {
    protected ControlsScreenMixin(Screen previous, Options options, Component title) {
        super(previous, options, title);
    }

    @Inject(method = "addOptions", at = @At("TAIL"))
    private void salts_inventory_update$movementControls(CallbackInfo ci) {
        this.list.addSmall(java.util.List.of(WindowMovementControls.modeButton()));
    }
}
