package com.salts_inventory_update.compat.ae2.extended.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import com.salts_inventory_update.compat.ae2.nativeview.ExtendedAePackets;
/** Enter confirms the native edit and closes its desktop lifetime. */
@Pseudo @Mixin(targets="com.glodblock.github.extendedae.client.gui.GuiRenamer",remap=false)
public abstract class RenamerCloseMixin {
 @Redirect(method="keyPressed",at=@At(value="INVOKE",target="Lcom/glodblock/github/extendedae/client/gui/GuiRenamer;onClose()V"),remap=false)
 private void close(@Coerce Object owner){
  if(owner instanceof appeng.client.gui.AEBaseScreen<?> screen&&screen.isDesktopHosted())ExtendedAePackets.invoke(screen,"closeCurrentMenu");
  else ((net.minecraft.client.gui.screens.Screen)owner).onClose();
 }
}
