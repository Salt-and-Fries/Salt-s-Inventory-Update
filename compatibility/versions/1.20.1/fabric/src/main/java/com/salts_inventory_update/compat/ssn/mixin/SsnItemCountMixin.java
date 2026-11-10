package com.salts_inventory_update.compat.ssn.mixin;

import com.salts_inventory_update.compat.ssn.*;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** The legacy publisher scales counts in a global matrix; a captured window needs its local pose. */
@Pseudo
@Mixin(targets="com.lothrazar.storagenetwork.gui.slot.ItemSlotNetwork",remap=false)
public abstract class SsnItemCountMixin {
    @Inject(method="drawSlot",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$localCounts(GuiGraphics graphics,Font font,int mouseX,int mouseY,CallbackInfo ci){
        Object parent=SsnWindows.field(this,"parent");
        if(!(parent instanceof AbstractContainerScreen<?> screen)||!SsnClient.hosted(screen))return;
        ci.cancel();
        var stack=(ItemStack)SsnWindows.field(this,"stack");if(stack.isEmpty())return;
        int x=((Number)SsnWindows.field(this,"x")).intValue(),y=((Number)SsnWindows.field(this,"y")).intValue();
        int count=((Number)SsnWindows.field(this,"size")).intValue();
        if(Boolean.TRUE.equals(SsnWindows.field(this,"showNumbers"))&&count>1){
            String amount=Screen.hasShiftDown()?Integer.toString(count):String.valueOf(format(count));
            var pose=graphics.pose();pose.pushPose();
            try{pose.translate(x+3,y+3,0);pose.scale(.85f,.85f,.85f);pose.translate(-x,-y,0);graphics.renderItemDecorations(font,stack,x,y,amount);}
            finally{pose.popPose();}
        }
        if(Boolean.TRUE.equals(SsnWindows.invoke(this,"isMouseOverSlot",mouseX,mouseY)))graphics.fillGradient(x,y,x+16,y+16,-2130706433,-2130706433);
        graphics.renderItem(stack,x,y);
    }
    @Unique private static Object format(int count){
        try{return Class.forName(SsnLayouts.BASE+"util.UtilInventory").getMethod("formatLargeNumber",int.class).invoke(null,count);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Native SSN count format",e);}
    }
}
