package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.aircraft.AircraftWindows;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo @Mixin(targets="immersive_aircraft.entity.InventoryVehicleEntity",remap=false)
public abstract class AircraftOpeningMixin {
    @Inject(method="openInventory",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$aircraftWindows(ServerPlayer player,CallbackInfo ci){if(AircraftWindows.open((Entity)(Object)this,player))ci.cancel();}
}
