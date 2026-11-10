package com.salts_inventory_update.compat.nativewindows.mixin;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The 1.8.x fluid selector uses a null label despite being an AbstractWidget. */
@Pseudo @Mixin(targets="aztech.modern_industrialization.pipes.fluid.FluidPipeScreen",remap=false)
public abstract class ModernFluidScreenMixin {
    @Inject(method={"init","method_25426","m_7856_"},at=@At("RETURN"),require=0,remap=false)
    private void salts$emptyNarration(CallbackInfo ci){
        for(var child:((Screen)(Object)this).children())
            if(child instanceof AbstractWidget widget&&widget.getMessage()==null)widget.setMessage(Component.empty());
    }
}
