package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.modernindustrialization.ModernIndustrializationPackets;
import net.minecraft.server.MinecraftServer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;

/** Keep the original decoder and action inside the validated desktop callback. */
@Pseudo @Mixin(targets={
    "aztech.modern_industrialization.machines.MachinePackets$C2S",
    "aztech.modern_industrialization.inventory.ConfigurableInventoryPacketHandlers$C2S",
    "aztech.modern_industrialization.pipes.impl.PipePackets",
    "aztech.modern_industrialization.proxy.CommonProxy"},remap=false)
public abstract class ModernServerActionMixin {
    @Redirect(method="*",at=@At(value="INVOKE",target="Lnet/minecraft/server/MinecraftServer;execute(Ljava/lang/Runnable;)V",remap=false),require=0,remap=false)
    private static void salts$owningMenu(MinecraftServer server,Runnable action){
        if(ModernIndustrializationPackets.inDesktopAction())action.run();else server.execute(action);
    }
}
