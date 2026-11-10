package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.api.client.desktop.DesktopNativeTextInput;
import com.salts_inventory_update.compat.create.CreateNativeAccess;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.simibubi.create.foundation.gui.menu.AbstractSimiContainerScreen",remap=false)
public abstract class CreateScreenBridgeMixin implements DesktopNativeTextInput {
    @Inject(method="<init>",at=@At("RETURN"),remap=false)
    private void salts$rememberNativeScreen(CallbackInfo ci) { CreateNativeAccess.register((AbstractContainerScreen<?>)(Object)this); }
    @Override public boolean desktopNativeTextInputFocused() { return CreateNativeAccess.textFocused((Screen)(Object)this); }
}
