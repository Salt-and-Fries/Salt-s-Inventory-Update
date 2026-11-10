package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.modernindustrialization.ModernIndustrializationClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets={"aztech.modern_industrialization.network.machines.MachineComponentSyncPacket","aztech.modern_industrialization.network.machines.UpdateItemSlotPacket","aztech.modern_industrialization.network.machines.UpdateFluidSlotPacket","aztech.modern_industrialization.network.pipes.SetPriorityPacket","aztech.modern_industrialization.network.pipes.SetConnectionTypePacket","aztech.modern_industrialization.network.pipes.SetItemWhitelistPacket","aztech.modern_industrialization.network.pipes.SetNetworkFluidPacket"},remap=false)
public abstract class ModernAddressedPacketMixin {
    @Inject(method="handle",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private void salts$route(@Coerce Object context,CallbackInfo ci){
        if(ModernIndustrializationClient.routePacket(this,context))ci.cancel();
    }
}
