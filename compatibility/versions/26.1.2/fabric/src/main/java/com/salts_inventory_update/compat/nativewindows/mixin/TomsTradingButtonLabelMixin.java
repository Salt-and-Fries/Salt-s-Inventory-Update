package com.salts_inventory_update.compat.nativewindows.mixin;

import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Native icon buttons have no text; provide an empty label for geometry/narration. */
@Pseudo
@Mixin(targets="com.tom.trading.gui.GuiButton",remap=false)
public abstract class TomsTradingButtonLabelMixin {
    @Inject(method="<init>",at=@At("RETURN"),remap=false)
    private void salts$emptyLabel(CallbackInfo ci){var widget=(AbstractWidget)(Object)this;if(widget.getMessage()==null)widget.setMessage(Component.empty());}
}
