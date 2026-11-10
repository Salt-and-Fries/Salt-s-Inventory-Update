package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.modernindustrialization.ModernIndustrializationClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="aztech.modern_industrialization.pipes.impl.PipePackets",remap=false)
public abstract class ModernPipePacketMixin {
    @Inject(method="lambda$static$0",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void salts$route0(Player player,int sync,boolean value,CallbackInfo ci){
        if(player!=Minecraft.getInstance().player)return;
        if(ModernIndustrializationClient.route(sync,()->ModernIndustrializationClient.invoke("aztech.modern_industrialization.pipes.impl.PipePackets","lambda$static$0",player,sync,value)))ci.cancel();
    }
    @Inject(method="lambda$static$2",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void salts$route1(Player player,int sync,int value,CallbackInfo ci){
        if(player!=Minecraft.getInstance().player)return;
        if(ModernIndustrializationClient.route(sync,()->ModernIndustrializationClient.invoke("aztech.modern_industrialization.pipes.impl.PipePackets","lambda$static$2",player,sync,value)))ci.cancel();
    }
    @Inject(method="lambda$static$6",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void salts$route2(Player player,int sync,@Coerce Object value,CallbackInfo ci){
        if(player!=Minecraft.getInstance().player)return;
        if(ModernIndustrializationClient.route(sync,()->ModernIndustrializationClient.invoke("aztech.modern_industrialization.pipes.impl.PipePackets","lambda$static$6",player,sync,value)))ci.cancel();
    }
}
