package com.salts_inventory_update.compat.ssn.mixin;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.salts_inventory_update.compat.ssn.SsnClient;
import org.spongepowered.asm.mixin.*;
/** Integer boxes send their own save packets, including on focus changes. */
@Pseudo @Mixin(targets="com.lothrazar.storagenetwork.gui.components.TextboxInteger",remap=false)
public abstract class SsnIntegerInputMixin {
    @WrapMethod(method={"keyPressed","m_7933_"},remap=false,require=0)
    private boolean salts$nativeKey(net.minecraft.client.input.KeyEvent event,Operation<Boolean> original){return SsnClient.nativeWidgetCall(this,()->original.call(event));}
    @WrapMethod(method={"charTyped","m_5534_"},remap=false,require=0)
    private boolean salts$nativeText(net.minecraft.client.input.CharacterEvent event,Operation<Boolean> original){return SsnClient.nativeWidgetCall(this,()->original.call(event));}
    @WrapMethod(method={"setFocused","m_93692_"},remap=false,require=0)
    private void salts$nativeFocus(boolean focused,Operation<Void> original){SsnClient.nativeWidgetCall(this,()->{original.call(focused);return null;});}
}
