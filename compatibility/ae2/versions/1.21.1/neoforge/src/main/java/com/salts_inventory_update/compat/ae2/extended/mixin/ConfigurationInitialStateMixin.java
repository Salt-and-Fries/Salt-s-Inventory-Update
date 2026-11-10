package com.salts_inventory_update.compat.ae2.extended.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.salts_inventory_update.compat.ae2.nativeview.ExtendedAePackets;
/** Native host state must exist before AE2's first GuiSync snapshot. */
@Pseudo @Mixin(targets="com.glodblock.github.extendedae.container.ContainerConfigModifier",remap=false)
public abstract class ConfigurationInitialStateMixin {
 @Inject(method="<init>",at=@At("TAIL"),remap=false) private void initialized(CallbackInfo ci){ExtendedAePackets.initializeConfiguration(this);}
}
