package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.adastra.AdAstraWindows;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Preserve native action validation when its menu lives in an SIU session. */
@Pseudo @Mixin(targets="earth.terrarium.adastra.common.utils.ModUtils",remap=false)
public abstract class AdAstraMachinePacketMixin {
    @Inject(method="getMachineFromMenuPacket",at=@At("RETURN"),cancellable=true,remap=false)
    private static void siu$machineSession(BlockPos pos,Player player,Level level,CallbackInfoReturnable<Optional<?>> result){
        if(result.getReturnValue().isEmpty())AdAstraWindows.machine(pos,player,level).ifPresent(machine->result.setReturnValue(Optional.of(machine)));
    }
}
