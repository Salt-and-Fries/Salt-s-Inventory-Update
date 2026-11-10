package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.alexsmobs.TransmutationWindows;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo
@Mixin(targets="com.github.alexthe666.alexsmobs.inventory.MenuTransmutationTable",remap=false)
public abstract class AlexTransmutationChoiceMixin {
    @Inject(method={"clickMenuButton","m_6366_"},at=@At("HEAD"),cancellable=true,require=1,remap=false)
    private void salts$owningChoice(Player p,int choice,CallbackInfoReturnable<Boolean> ci){if(TransmutationWindows.choose((AbstractContainerMenu)(Object)this,choice))ci.setReturnValue(true);}
}
