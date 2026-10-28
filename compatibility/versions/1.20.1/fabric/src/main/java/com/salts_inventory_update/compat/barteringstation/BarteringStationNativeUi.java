package com.salts_inventory_update.compat.barteringstation;

import com.salts_inventory_update.compat.nativewindows.NativeMachineLayouts;
import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;
import com.salts_inventory_update.api.client.desktop.*;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.model.SkullModel;
import net.minecraft.client.model.SkullModelBase;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class BarteringStationNativeUi {
    private BarteringStationNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.installTemplate("barteringstation", "barteringstation:bartering_station", Template::new);
    }

    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,SkullModelBase> {
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,SkullModelBase> context){
            return NativeMachineLayouts.bartering(context.nativeView());
        }
        @Override public SkullModelBase createState(DesktopWindowSetupContext<AbstractContainerMenu> context){
            try{
                var layer=(ModelLayerLocation)Class.forName("fuzs.barteringstation.client.init.ModClientRegistry")
                        .getField("PIGLIN_HEAD_MODEL_LAYER_LOCATION").get(null);
                return new SkullModel(context.minecraft().getEntityModels().bakeLayer(layer));
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Bartering model unavailable",failure);}
        }
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,SkullModelBase> context){
            // Upstream uses RenderSystem directly, so its 3D model bypasses native
            // GuiGraphics capture. Reuse the public upstream renderer on the API surface.
            int x=context.contentX()+43,y=context.contentY()+4;
            var matrix=RenderSystem.getModelViewStack();
            context.nativeGraphics().flush();matrix.pushPose();
            try{
                Class<?> handler=Class.forName("fuzs.barteringstation.client.handler.PiglinHeadModelHandler");
                handler.getMethod("renderPiglinHeadGuiModel",int.class,int.class,int.class,SkullModelBase.class)
                        .invoke(handler.getField("INSTANCE").get(null),x,y,150,context.state());
                int count=((Number)context.model().getClass().getMethod("getNearbyPiglins").invoke(context.model())).intValue();
                String text=Integer.toString(count);
                context.text(text,x+17-context.minecraft().font.width(text),y+9,count>0?0x55ff55:0xff5555,true);
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Bartering model failed",failure);}
            finally{matrix.popPose();RenderSystem.applyModelViewMatrix();}
        }
    }
}
