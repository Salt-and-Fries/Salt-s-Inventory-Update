package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.TechRebornWindows;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerListener;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="reborncore.common.screen.BuiltScreenHandler",remap=false)
public abstract class RebornSyncMixin {
    @Inject(method="sendUpdatedValues",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$session(ContainerListener listener,AbstractContainerMenu menu,Int2ObjectMap<?> values,CallbackInfo ci){if(TechRebornWindows.send(listener,menu,values))ci.cancel();}
}
