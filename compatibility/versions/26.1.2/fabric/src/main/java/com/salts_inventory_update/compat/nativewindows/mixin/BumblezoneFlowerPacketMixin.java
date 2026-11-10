package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.compat.bumblezone.BumblezoneFlowerPackets;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep the upstream decoder and scheduling; redirect only a hosted list update. */
@Pseudo
@Mixin(targets="com.telepathicgrunt.the_bumblezone.packets.CrystallineFlowerEnchantmentPacket$Handler",remap=false)
public abstract class BumblezoneFlowerPacketMixin {
    @Inject(method="lambda$handle$0",at=@At("HEAD"),cancellable=true,remap=false)
    private static void salts$flowerList(@Coerce Object packet,CallbackInfo ci) {
        if(BumblezoneFlowerPackets.receive(packet))ci.cancel();
    }
}
