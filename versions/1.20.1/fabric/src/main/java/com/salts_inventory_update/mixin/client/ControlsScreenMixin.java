package com.salts_inventory_update.mixin.client;

import com.salts_inventory_update.client.WindowMovementControls;
import net.minecraft.client.Options;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.OptionsSubScreen;
import net.minecraft.client.gui.screens.controls.ControlsScreen;
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

    @Inject(method = "init", at = @At("TAIL"))
    private void salts_inventory_update$movementControls(CallbackInfo ci) {
        int rowY = this.height / 6 - 12 + 72;
        // Insert a row before Done, preserving the vanilla Controls layout.
        for (var child : this.children()) {
            if (child instanceof net.minecraft.client.gui.components.AbstractWidget widget && widget.getY() >= rowY) {
                widget.setY(widget.getY() + 24);
            }
        }
        var mode = WindowMovementControls.modeButton();
        mode.setPosition(this.width / 2 - 155, rowY);
        this.addRenderableWidget(mode);
    }
}
