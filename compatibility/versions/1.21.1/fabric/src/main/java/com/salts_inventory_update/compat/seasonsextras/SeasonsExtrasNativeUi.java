package com.salts_inventory_update.compat.seasonsextras;

import com.salts_inventory_update.compat.nativewindows.NativeMachineLayouts;
import com.salts_inventory_update.compat.nativewindows.NativeWindowPolish;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;

public final class SeasonsExtrasNativeUi {
    private SeasonsExtrasNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.installTemplate("seasonsextras", "seasonsextras:air_conditioning_screen", Template::new);
    }

    private static final class State { int button=-1;long pressedUntil; }
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,State> {
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> context){return new State();}
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> context){
            return NativeMachineLayouts.conditioning(context.nativeView());
        }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,State> context,MouseButtonEvent event,boolean twice){
            if(event.button()!=0)return false;
            var layout=NativeMachineLayouts.conditioning(((DesktopNativeContext<?,?>)context).nativeView());
            var point=layout.exteriorSourceAt(event.x()-context.windowX(),event.y()-context.windowY());
            if(point.isEmpty())point=layout.sourceAt(event.x()-context.contentX(),event.y()-context.contentY());
            if(point.isEmpty())return false;
            double x=point.get().x(),y=point.get().y();int button=-1;
            if(x>=163 && x<170 && y>=6 && y<13)button=3;
            if(y>=59 && y<66)for(int i=0;i<3;i++)if(x>=102+18*i && x<109+18*i)button=i;
            if(button<0)return false;
            context.sendMenuButton(button);
            context.state().button=button;context.state().pressedUntil=System.nanoTime()+150_000_000L;
            context.minecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK,1));
            return true;
        }
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> context){
            int button=context.state().button;
            if(button<0 || System.nanoTime()>context.state().pressedUntil)return;
            try{
                Object menu=context.model(),conditioning=menu.getClass().getMethod("getConditioning").invoke(menu);
                var texture=(ResourceLocation)conditioning.getClass().getMethod("getTexture").invoke(conditioning);
                boolean enabled;
                if(button==3)enabled=(Boolean)menu.getClass().getMethod("hasParticles").invoke(menu);
                else{
                    Object[] slots=(Object[])menu.getClass().getMethod("getBurnSlots").invoke(menu);
                    enabled=slots[button].getClass().getField("enabled").getBoolean(slots[button]);
                }
                int x=button==3?context.contentX()+122:context.contentX()+77+18*button;
                int y=button==3?context.contentY():context.contentY()+43;
                int u=button==3?(enabled?190:197):(enabled?176:183);
                context.texture(texture,x,y,u,21,7,7,7,7,256,256);
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Seasons button rendering failed",failure);}
        }
    }
}
