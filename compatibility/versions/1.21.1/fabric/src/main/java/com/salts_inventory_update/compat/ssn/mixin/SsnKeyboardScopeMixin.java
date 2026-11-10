package com.salts_inventory_update.compat.ssn.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.salts_inventory_update.api.client.desktop.DesktopNativeTextInput;
import com.salts_inventory_update.compat.ssn.SsnClient;
import org.spongepowered.asm.mixin.*;

@Pseudo @Mixin(targets="com.lothrazar.storagenetwork.block.AbstractNetworkScreen",remap=false)
public abstract class SsnKeyboardScopeMixin implements DesktopNativeTextInput {
    @Override public boolean desktopNativeTextInputFocused(){return SsnClient.searchFocused(this);}
    @WrapMethod(method={"keyPressed","m_7933_"},remap=false,require=0)
    private boolean salts$nativeKey(int key,int scan,int mods,Operation<Boolean> original){return SsnClient.nativeCall(this,()->original.call(key,scan,mods));}
    @WrapMethod(method={"charTyped","m_5534_"},remap=false,require=0)
    private boolean salts$nativeText(char ch,int mods,Operation<Boolean> original){return SsnClient.nativeCall(this,()->original.call(ch,mods));}
}
