package com.salts_inventory_update.compat.alloyforgery;

import com.salts_inventory_update.compat.nativewindows.*;
import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.client.input.*;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.entity.BlockEntity;
import java.lang.reflect.Array;

public final class AlloyForgeryNativeUi {
    private AlloyForgeryNativeUi() { }
    public static void initialize() {
        NativeWindowPolish.installTemplate("alloy-forgery","alloy-forgery:alloy_forge",Template::new);
    }
    private static final class State { int hovered=-1; }
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,State> {
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c){return new State();}
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c){return NativeMachineLayouts.alloyForge(c.nativeView());}
        private int inputAt(DesktopWindowContext<AbstractContainerMenu,State> c,double x,double y) {
            var point=NativeMachineLayouts.alloyForge(((DesktopNativeContext<?,?>)c).nativeView()).sourceAt(x-c.contentX(),y-c.contentY());
            if(point.isEmpty())return -1;
            for(int i=0;i<c.model().slots.size();i++){
                var slot=c.model().getSlot(i);
                if(slot.getClass().getName().equals("io.wispforest.alloyforgery.utils.ForgeInputSlot")
                        && point.get().x()>=slot.x-1&&point.get().x()<slot.x+17&&point.get().y()>=slot.y-1&&point.get().y()<slot.y+17)return i;
            }return -1;
        }
        @Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,State> c,net.minecraft.resources.Identifier channel,byte[] data){
            if(!channel.equals(AlloyPropertyBridge.CHANNEL))return;
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data));
            try{
                Class<?> packet=Class.forName("io.wispforest.owo.client.screens.MenuNetworkingInternals$SyncPropertiesPacket");
                Object message=packet.getConstructor(net.minecraft.network.FriendlyByteBuf.class).newInstance(buffer);
                c.model().getClass().getMethod("owo$readPropertySync",packet).invoke(c.model(),message);
                if(buffer.isReadable())throw new IllegalStateException("Trailing alloy property data");
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Alloy property synchronization failed",failure);}
            finally{buffer.release();}
        }
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> c){c.state().hovered=inputAt(c,c.mouseX(),c.mouseY());}
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e,boolean twice){
            int index=inputAt(c,e.x(),e.y());
            if(!AlloySlotModes.locking(c.model())||index<0||c.model().getSlot(index).hasItem()||c.minecraft().player.isSpectator())return false;
            for(int i=0;i<c.minecraft().options.keyHotbarSlots.length;i++)if(c.minecraft().options.keyHotbarSlots[i].matchesMouse(e)
                    &&!c.minecraft().player.getInventory().getItem(i).isEmpty()){
                if(isDisabled(c.model(),c.model().getSlot(index)))setEnabled(c,index,true);
                return false;
            }
            if(e.button()!=0&&e.button()!=1)return false;
            setEnabled(c,index,isDisabled(c.model(),c.model().getSlot(index)));
            // Preserve native placement when a carried item is present; empty
            // cursor clicks solely toggle the slot, without starting a drag.
            return c.carriedStack().isEmpty();
        }
        @Override public boolean keyPressed(DesktopInputContext<AbstractContainerMenu,State> c,KeyEvent e){
            int index=c.state().hovered;
            if(AlloySlotModes.locking(c.model())&&index>=0&&!c.model().getSlot(index).hasItem()&&!c.minecraft().player.isSpectator()
                    &&isDisabled(c.model(),c.model().getSlot(index)))
                for(int i=0;i<c.minecraft().options.keyHotbarSlots.length;i++)if(c.minecraft().options.keyHotbarSlots[i].matches(e)
                        &&!c.minecraft().player.getInventory().getItem(i).isEmpty()){setEnabled(c,index,true);break;}
            return false;
        }
    }
    private static boolean isDisabled(AbstractContainerMenu menu,Slot slot){
        try{return (Boolean)menu.getClass().getMethod("isSlotDisabled",Slot.class).invoke(menu,slot);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Alloy slot state unavailable",failure);}
    }
    private static void setEnabled(DesktopInputContext<AbstractContainerMenu,State> c,int index,boolean enabled){
        try{
            Object forge=c.model().getClass().getField("forge").get(c.model());
            Class<?> packet=Class.forName("io.wispforest.alloyforgery.networking.DisableSlotToggle");
            Object message=packet.getConstructor(BlockEntity.class,Integer.class,Boolean.class)
                    .newInstance(forge,c.model().getSlot(index).getContainerSlot(),!enabled);
            Object channel=Class.forName("io.wispforest.alloyforgery.networking.AlloyForgeNetworking").getField("CHANNEL").get(null);
            Object handle=channel.getClass().getMethod("clientHandle").invoke(channel);
            boolean sent=false;
            for(var send:handle.getClass().getMethods())if(send.getName().equals("send")&&send.getParameterCount()==1){
                Class<?> parameter=send.getParameterTypes()[0];
                if(parameter.isInstance(message)){send.invoke(handle,message);sent=true;break;}
                if(parameter.isArray()&&parameter.getComponentType().isInstance(message)){
                    Object array=Array.newInstance(parameter.getComponentType(),1);Array.set(array,0,message);send.invoke(handle,array);sent=true;break;
                }
            }
            if(!sent)throw new IllegalStateException("No supported upstream client send method");
            c.minecraft().getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.UI_BUTTON_CLICK,enabled?1:.75f));
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Alloy slot toggle failed",failure);}
    }
}
