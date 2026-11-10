package com.salts_inventory_update.compat.ae2.extended.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.salts_inventory_update.compat.ae2.nativeview.ExtendedAePackets;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
@Pseudo @Mixin(targets={"appeng.menu.implementations.PatternAccessTermMenu", "com.glodblock.github.extendedae.container.ContainerAssemblerMatrix", "com.glodblock.github.extendedae.container.ContainerCaner", "com.glodblock.github.extendedae.container.ContainerConfigModifier", "com.glodblock.github.extendedae.container.ContainerExInterface", "com.glodblock.github.extendedae.container.ContainerExMolecularAssembler", "com.glodblock.github.extendedae.container.ContainerModExportBus", "com.glodblock.github.extendedae.container.ContainerModStorageBus", "com.glodblock.github.extendedae.container.ContainerPreciseStorageBus", "com.glodblock.github.extendedae.container.ContainerRenamer", "com.glodblock.github.extendedae.container.ContainerTagStorageBus", "com.glodblock.github.extendedae.container.ContainerThresholdExportBus", "com.glodblock.github.extendedae.container.ContainerVoidCell", "com.glodblock.github.extendedae.container.ContainerWirelessConnector", "com.glodblock.github.extendedae.container.ContainerWirelessHub", "com.glodblock.github.extendedae.xmod.wt.ContainerWirelessExPAT"},remap=false)
public abstract class ContainerUpdatesMixin {
 @WrapMethod(method="broadcastChanges",remap=false) private void updates(Operation<Void> original){ExtendedAePackets.scoped(this,()->original.call());}
}
