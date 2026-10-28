package com.salts_inventory_update.compat.netherchested.mixin;
import com.salts_inventory_update.api.desktop.DesktopSynchronization;
import net.minecraft.world.inventory.ContainerSynchronizer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
/** Limitless Containers deliberately disables the vanilla setter; retain its full-count setter. */
@Pseudo
@Mixin(targets="fuzs.limitlesscontainers.api.limitlesscontainers.v1.LimitlessContainerMenu",remap=false)
abstract class LimitlessSynchronizerMixin {
    @Shadow(remap=false) public abstract void setActualSynchronizer(ContainerSynchronizer synchronizer);
    @Inject(method={"setSynchronizer","method_34248"},at=@At("HEAD"),cancellable=true,remap=false)
    private void salts_inventory_update$desktopSynchronizer(ContainerSynchronizer synchronizer,CallbackInfo ci) {
        if(DesktopSynchronization.isDesktopSynchronizer(synchronizer)) {
            setActualSynchronizer(synchronizer);ci.cancel();
        }
    }
}
