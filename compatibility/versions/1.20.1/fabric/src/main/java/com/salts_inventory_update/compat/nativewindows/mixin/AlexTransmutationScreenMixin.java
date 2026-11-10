package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.alexsmobs.TransmutationWindows;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo
@Mixin(targets="com.github.alexthe666.alexsmobs.client.gui.GUITransmutationTable",remap=false)
public abstract class AlexTransmutationScreenMixin {
    @Inject(method="<init>",at=@At("RETURN"),remap=false)
    private void salts$remember(CallbackInfo ci){TransmutationWindows.remember((AbstractContainerScreen<?>)(Object)this);}
}
