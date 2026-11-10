package com.salts_inventory_update.compat.powah;

import com.mojang.blaze3d.systems.RenderSystem;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import java.lang.reflect.Method;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.world.inventory.InventoryMenu;

/** The publisher's immediate-mode fluid renderer bypasses native GuiGraphics
 * capture. Reuse it on the final API surface, with its original tint and fill. */
final class PowahFluids {
    private static Method sprite, color, gauge;
    private PowahFluids() { }

    static void render(DesktopRenderContext<?,?> c, PowahWindows.Kind kind) {
        if(kind!=PowahWindows.Kind.REACTOR&&kind!=PowahWindows.Kind.MAGMATOR&&kind!=PowahWindows.Kind.THERMO)return;
        Object tank=PowahWindows.invoke(PowahWindows.tile((net.minecraft.world.inventory.AbstractContainerMenu)c.model()),"getTank");
        if((Boolean)PowahWindows.invoke(tank,"isEmpty"))return;
        try {
            if(sprite==null) {
                Class<?> fluid=Class.forName("dev.architectury.fluid.FluidStack");
                Class<?> hooks=Class.forName("dev.architectury.hooks.fluid.FluidStackHooks");
                sprite=hooks.getMethod("getStillTexture",fluid);color=hooks.getMethod("getColor",fluid);
                gauge=Class.forName("owmii.powah.lib.client.util.Draw").getMethod("gaugeV",TextureAtlasSprite.class,
                    int.class,int.class,int.class,int.class,int.class,int.class);
            }
            Object fluid=PowahWindows.invoke(tank,"getFluid");
            var texture=(TextureAtlasSprite)sprite.invoke(null,fluid);
            if(texture==null)return;
            int tint=((Number)color.invoke(null,fluid)).intValue();
            int capacity=((Number)PowahWindows.invoke(tank,"getCapacity")).intValue();
            int amount=((Number)PowahWindows.invoke(tank,"getFluidAmount")).intValue();
            int bodyX=(c.windowWidth()-14-kind.bodyWidth())/2;
            int x=c.contentX()+bodyX+157,y=c.contentY()+5;
            c.nativeGraphics().flush();
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            RenderSystem.setShaderTexture(0,InventoryMenu.BLOCK_ATLAS);
            RenderSystem.setShaderColor((tint>>16&255)/255F,(tint>>8&255)/255F,(tint&255)/255F,1);
            try {
                c.withClip(x,y,14,65,()->{
                    try {gauge.invoke(null,texture,x,y,14,65,capacity,amount);}
                    catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah fluid draw",failure);}
                });
            }finally {RenderSystem.setShaderColor(1,1,1,1);}
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah fluid renderer unavailable",failure);}
    }
}
