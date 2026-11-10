package com.salts_inventory_update.compat.ae2.extended.mixin;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import com.salts_inventory_update.compat.ae2.nativeview.ExtendedAePackets;
import net.minecraft.client.gui.GuiGraphics;
import com.salts_inventory_update.compat.ae2.nativeview.ExtendedAeLayout;
@Pseudo
@Mixin(targets={"com.github.glodblock.extendedae.client.gui.GuiPatternModifier", "com.glodblock.github.extendedae.client.gui.GuiPatternModifier"},remap=false)
public abstract class PatternModifierLabelsMixin {
 @Inject(method="drawFG",at=@At("HEAD"),cancellable=true,remap=false) private void siu$labels(GuiGraphics g,int ox,int oy,int mx,int my,CallbackInfo ci){if(ExtendedAeLayout.patternLabels(this,g))ci.cancel();}
}
