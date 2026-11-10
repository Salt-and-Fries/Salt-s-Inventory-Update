package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.modernindustrialization.ModernIndustrializationClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="aztech.modern_industrialization.inventory.ConfigurableInventoryS2CPacketHandlers",remap=false)
public abstract class ModernInventoryPacketMixin {
    @Inject(method="lambda$static$0",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void salts$route0(Minecraft client,int sync,int index,CompoundTag data,CallbackInfo ci){
        
        if(ModernIndustrializationClient.route(sync,()->ModernIndustrializationClient.invoke("aztech.modern_industrialization.inventory.ConfigurableInventoryS2CPacketHandlers","lambda$static$0",client,sync,index,data)))ci.cancel();
    }
    @Inject(method="lambda$static$2",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void salts$route1(Minecraft client,int sync,int index,CompoundTag data,CallbackInfo ci){
        
        if(ModernIndustrializationClient.route(sync,()->ModernIndustrializationClient.invoke("aztech.modern_industrialization.inventory.ConfigurableInventoryS2CPacketHandlers","lambda$static$2",client,sync,index,data)))ci.cancel();
    }
}
