package com.salts_inventory_update.functionaltest.probe;

import java.util.HashSet;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import com.salts_inventory_update.client.automatic.AutomaticNativeScope;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** QA-only; records but never changes native screen rendering. */
@Mixin(Screen.class)
public abstract class TransparentBackgroundProbeMixin {
    private static final Set<String> SEEN = new HashSet<>();

    @Inject(method="renderTransparentBackground", at=@At("HEAD"))
    private void saltsQaTraceTransparent(GuiGraphics graphics, CallbackInfo ci) {
        String name=getClass().getName();
        boolean capturing=AutomaticNativeScope.capturingSlots(this);
        if(!capturing || !(name.contains("Drawer") || name.contains("Smither") ||
                name.contains("Anvil") || name.contains("Grinder") || name.contains("Smithing") || name.contains("Grindstone"))) return;
        if(!SEEN.add(name)) return;
        Screen current=Minecraft.getInstance().screen;
        System.out.println("SIU_QA_TRANSPARENT_CAPTURE native="+name+" minecraftScreen="+(current==null?"null":current.getClass().getName())+
                " sameScreen="+(current==(Object)this)+" scope="+capturing);
        for(StackTraceElement element:Thread.currentThread().getStackTrace())
            System.out.println("SIU_QA_TRANSPARENT_STACK "+element);
    }
}
