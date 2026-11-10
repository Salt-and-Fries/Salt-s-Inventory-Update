package com.salts_inventory_update.compat.ae2.extended.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.salts_inventory_update.compat.ae2.nativeview.ExtendedAePackets;
import net.minecraft.client.gui.GuiGraphics;
import com.salts_inventory_update.compat.ae2.nativeview.ExtendedAeLayout;
@Pseudo @Mixin(targets="com.glodblock.github.extendedae.client.gui.GuiExPatternTerminal",remap=false)
public abstract class PatternRowsMixin {
 @Inject(method="drawFG",at=@At("TAIL"),remap=false) private void rows(GuiGraphics g,int ox,int oy,int mx,int my,CallbackInfo ci){ExtendedAeLayout.afterForeground(this);}
}
