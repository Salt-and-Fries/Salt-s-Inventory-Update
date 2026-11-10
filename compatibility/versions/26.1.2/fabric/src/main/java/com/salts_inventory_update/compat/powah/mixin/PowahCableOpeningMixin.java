package com.salts_inventory_update.compat.powah.mixin;

import com.salts_inventory_update.compat.powah.PowahWindows;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.phys.BlockHitResult;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Pseudo @Mixin(targets="owmii.powah.block.cable.CableBlock",remap=false)
public abstract class PowahCableOpeningMixin {
    @Inject(method="getContainer",at=@At("RETURN"),remap=false)
    private void salts$rememberFace(int id,Inventory inventory,@Coerce Object tile,BlockHitResult hit,CallbackInfoReturnable<Object> cir) {
        PowahWindows.rememberCableFace(cir.getReturnValue(),hit);
    }
}
