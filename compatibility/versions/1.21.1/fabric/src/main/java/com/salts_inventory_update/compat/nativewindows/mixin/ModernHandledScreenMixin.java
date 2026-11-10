package com.salts_inventory_update.compat.nativewindows.mixin;

import com.salts_inventory_update.api.client.desktop.DesktopNativeRenderTarget;
import com.salts_inventory_update.compat.modernindustrialization.ModernIndustrializationClient;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Pseudo @Mixin(targets="aztech.modern_industrialization.client.screen.MIHandledScreen",remap=false)
public abstract class ModernHandledScreenMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    protected ModernHandledScreenMixin(AbstractContainerMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Inject(method="<init>",at=@At("RETURN"),remap=false)
    private void salts$remember(CallbackInfo ci){ModernIndustrializationClient.remember(this);}
    @Override protected void renderLabels(GuiGraphics graphics,int x,int y){
        // Native titles can paint over their own header buttons. SIU already
        // draws the title and owns the separate player inventory window.
        if(DesktopNativeRenderTarget.current().isPresent())return;
        super.renderLabels(graphics,x,y);
    }
}
