package com.salts_inventory_update.compat.sophisticated.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

import com.salts_inventory_update.internal.desktop.DesktopSynchronizerOverride;

import net.minecraft.world.inventory.ContainerSynchronizer;
import net.p3pp3rf1y.sophisticatedcore.common.gui.StorageContainerMenuBase;

@Mixin(value = StorageContainerMenuBase.class, remap = false)
public abstract class StorageContainerSynchronizerMixin {
    @ModifyArg(
        method = "setSynchronizer",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/world/inventory/AbstractContainerMenu;setSynchronizer(Lnet/minecraft/world/inventory/ContainerSynchronizer;)V"
        ),
        index = 0
    )
    private ContainerSynchronizer salts_inventory_update$preserveDetachedSynchronizer(ContainerSynchronizer proposed) {
        return DesktopSynchronizerOverride.preserveDesktop(proposed);
    }
}
