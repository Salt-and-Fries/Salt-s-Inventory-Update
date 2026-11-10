package com.salts_inventory_update.compat.nativewindows.mixin;

import com.mojang.blaze3d.systems.RenderSystem;
import com.salts_inventory_update.api.client.desktop.DesktopNativeRenderTarget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Pseudo;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Spawn's raw pixel scissor must follow its native offscreen capture. */
@Pseudo
@Mixin(targets="com.ninni.spawn.client.inventory.PigmentShifterScreen",remap=false)
public abstract class PigmentPreviewScissorMixin extends AbstractContainerScreen<AbstractContainerMenu> {
    protected PigmentPreviewScissorMixin(AbstractContainerMenu menu,Inventory inventory,Component title){super(menu,inventory,title);}
    @Inject(method="enableScissorBox",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts_inventory_compat$previewClip(int x,int y,int w,int h,CallbackInfo ci) {
        var capture=DesktopNativeRenderTarget.current();
        if(capture.isEmpty())return;
        var target=capture.get();double scale=target.guiScale();
        int left=x-(width-imageWidth)/2,top=y-(height-imageHeight)/2;
        RenderSystem.enableScissor((int)Math.floor(left*scale),
            (int)Math.floor(target.pixelHeight()-(top+h)*scale),
            Math.max(0,(int)Math.ceil(w*scale)),Math.max(0,(int)Math.ceil(h*scale)));
        ci.cancel();
    }
}
