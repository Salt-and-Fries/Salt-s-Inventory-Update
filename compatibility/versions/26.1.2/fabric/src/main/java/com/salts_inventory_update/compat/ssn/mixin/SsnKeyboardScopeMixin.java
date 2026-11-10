package com.salts_inventory_update.compat.ssn.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.salts_inventory_update.api.client.desktop.DesktopNativeTextInput;
import com.salts_inventory_update.compat.ssn.SsnClient;
import org.spongepowered.asm.mixin.*;

@Pseudo @Mixin(targets="com.lothrazar.storagenetwork.block.AbstractNetworkScreen",remap=false)
public abstract class SsnKeyboardScopeMixin implements DesktopNativeTextInput {
    @Override public boolean desktopNativeTextInputFocused(){return SsnClient.searchFocused(this);}
    @WrapMethod(method="keyPressed",remap=false,require=0)
    private boolean salts$nativeKey(net.minecraft.client.input.KeyEvent event,Operation<Boolean> original){return SsnClient.nativeCall(this,()->original.call(event));}
    @WrapMethod(method="charTyped",remap=false,require=0)
    private boolean salts$nativeText(net.minecraft.client.input.CharacterEvent event,Operation<Boolean> original){return SsnClient.nativeCall(this,()->original.call(event));}
}
