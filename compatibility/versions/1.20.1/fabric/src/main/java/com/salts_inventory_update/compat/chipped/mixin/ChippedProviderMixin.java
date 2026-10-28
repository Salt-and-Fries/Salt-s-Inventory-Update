package com.salts_inventory_update.compat.chipped.mixin;
import com.salts_inventory_update.compat.chipped.ChippedSources;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.*;

@Pseudo
@Mixin(targets="earth.terrarium.chipped.common.blocks.WorkbenchBlock$WorkbenchMenuProvider",remap=false)
abstract class ChippedProviderMixin {
    @Inject(method="<init>",at=@At("TAIL"),remap=false)
    private void salts$rememberSource(CallbackInfo ci){ChippedSources.provider(this);}
    @Inject(method={"createMenu","m_7208_"},at=@At("RETURN"),remap=false,require=1)
    private void salts$attachSource(CallbackInfoReturnable<AbstractContainerMenu> ci){ChippedSources.attachProvider(this,ci.getReturnValue());}
}
