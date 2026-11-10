package com.salts_inventory_update.compat.techreborn;

import com.salts_inventory_update.api.client.desktop.*;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Native Tech Reborn work areas with attached left controls and right upgrades. */
public final class TechRebornClient {
    private static final Set<String> INSTALLED=new HashSet<>();
    private static final Identifier FRAME=Identifier.fromNamespaceAndPath("salts_inventory_update","textures/gui/window.png");
    private TechRebornClient() { }
    public static void initializeClient(){install();DesktopClientApi.onClientStarted(c->install());}
    @SuppressWarnings({"unchecked","rawtypes"}) private static synchronized void install(){
        for(var type:BuiltInRegistries.MENU){var id=BuiltInRegistries.MENU.getKey(type);
            if(id==null||!id.getNamespace().equals("techreborn")||!INSTALLED.add(id.toString()))continue;
            DesktopMenuFactories.register(type,(mc,player,sync,data)->{
                if(data.length!=8)throw new IllegalArgumentException("Tech Reborn requires its native block position");
                var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
                try{var constructor=RebornAccess.type("techreborn.blockentity.GuiType$ScreenHandlerData").getDeclaredConstructor(net.minecraft.core.BlockPos.class);constructor.setAccessible(true);return(AbstractContainerMenu)RebornAccess.call(type,"create",sync,player.getInventory(),constructor.newInstance(buffer.readBlockPos()));}catch(ReflectiveOperationException failure){throw new IllegalStateException("Reborn opening data",failure);}finally{buffer.release();}
            });
            DesktopClientApi.registerNativeScreen((net.minecraft.world.inventory.MenuType)type,(m,i,t)->TechRebornScreens.create((AbstractContainerMenu)m,i,t));
            DesktopClientApi.registerNativeMatcher(Identifier.fromNamespaceAndPath("salts_inventory_compat","techreborn/"+id.getPath()),110,Set.of(type),l->true,l->new Template());
        }
    }
    private static class State {DesktopNativeLayout layout;boolean upgrades;int popupHeight;String popupTitle="";}
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,State>,DesktopWindowPresentation {
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c){return new State();}
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c){return new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(6,22,6,6),true,List.of(),DesktopViewportPolicy.CLAMP);}
        @Override public Component title(DesktopWindowContext<AbstractContainerMenu,State> c){return TechRebornScreens.tabName(c.model()).equals("RedstoneConfigGui")?Component.literal("Redstone"):TechRebornWindows.block(c.model()).getBlockState().getBlock().getName();}
        @Override public void opened(DesktopWindowContext<AbstractContainerMenu,State> c){TechRebornScreens.open(c.model());}
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){TechRebornScreens.close(c.model());}
        @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu,State> c){TechRebornScreens.prepare(c.model());}
        @Override public boolean retainPlayerSlot(DesktopNativeContext<AbstractContainerMenu,State> c,int id){var b=c.nativeView().slotBounds(id);return b.isPresent()&&b.get().y()<92;}
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c){
            var view=c.nativeView();var menu=c.model();String name=view.screenType().substring(view.screenType().lastIndexOf('.')+1),tab=TechRebornScreens.tabName(menu);
            boolean utility=Set.of("GuiBlastFurnace","GuiDistillationTower","GuiFluidReplicator","GuiFusionReactor","GuiImplosionCompressor","GuiIndustrialGrinder","GuiIndustrialSawmill","GuiVacuumFreezer","GuiAutoCrafting","GuiRollingMachine","GuiStorageUnit").contains(name);
            int top=utility?4:14,bottom=switch(name){case "GuiNuclearReactor"->142;case "GuiSolar","GuiElevator","GuiLaunchpad","GuiPlayerDetector"->76;case "GuiIronFurnace"->78;case "GuiAlloyFurnace"->75;case "GuiBatbox"->84;default->92;};
            int width=172,height=bottom-top+4;
            var builder=DesktopNativeLayout.builder();
            if(tab.equals("RedstoneConfigGui")){
                height=80;builder.keep("native-machine",new DesktopWindowBounds(7,94,162,76),5,4);
            }else if(name.equals("GuiElevator")&&tab.isEmpty()){
                width=96;builder.keep("native-machine",new DesktopWindowBounds(4,top,24,bottom-top),36,4);
            }else builder.keep("native-machine",new DesktopWindowBounds(4,top,168,bottom-top),2,4);
            if(tab.equals("SlotConfigGui")){
                // This is native configuration help, not the removed player grid.
                builder.keep("native-configuration",new DesktopWindowBounds(4,94,168,78),2,height+2);height+=82;
            }
            builder.canvas(width,height);
            boolean upgrades=TechRebornScreens.upgrades(menu);c.state().upgrades=upgrades;
            if(upgrades)builder.exterior("native-upgrade-cells",new DesktopWindowBounds(-21,9,20,74),width+10,27);
            if(name.equals("GuiNuclearReactor"))builder.exterior("native-reactor-energy",new DesktopWindowBounds(-25,6,28,75),-26,24);
            int sy=upgrades?86:6;
            for(int i=0;i<TechRebornScreens.tabs(menu);i++)builder.exterior("native-settings/"+i,new DesktopWindowBounds(-24,sy+i*24,24,24),-22,24+i*24);
            Object popup=TechRebornScreens.popup(menu);c.state().popupHeight=popup==null?0:(Integer)RebornAccess.call(popup,"getHeight");
            if(popup!=null){
                c.state().popupTitle=tab.equals("FluidConfigGui")?"Fluid Sides":c.state().popupHeight==107?"Item Output":"Item Input";
                builder.exterior("native-side-inspector",new DesktopWindowBounds(204,4,75,c.state().popupHeight-9),width+54,50);
            }
            return c.state().layout=builder.build();
        }
        @Override public void renderBehindFrame(DesktopRenderContext<?,?> c){
            if(!(c.state() instanceof State state))return;
            if(state.popupHeight>0){
                int x=c.windowX()+c.windowWidth()+28,y=c.windowY()+24,h=state.popupHeight+23;
                for(var b:List.of(new DesktopWindowBounds(x,y,104,26),new DesktopWindowBounds(x,y+26,14,h-26),new DesktopWindowBounds(x+89,y+26,15,h-26),new DesktopWindowBounds(x,y+h-6,104,6)))
                    c.withClip(b.x(),b.y(),b.width(),b.height(),()->c.windowNineSlice(FRAME,x,y,104,h));
            }
            if(state.upgrades){
                int x=c.windowX()+c.windowWidth()-4,y=c.windowY()+24;
                // Fill the complete surround of the native 20x74 cell region
                // at (2,3), including the gray gutters inside the outer rim.
                // A full frame would cover the already composited cells.
                for(var b:List.of(new DesktopWindowBounds(x,y,28,3),new DesktopWindowBounds(x+22,y,6,82),new DesktopWindowBounds(x,y+77,28,5)))
                    c.withClip(b.x(),b.y(),b.width(),b.height(),()->c.windowNineSlice(FRAME,x,y,28,82));
            }
        }
        @Override public List<DesktopWindowRegion> externalRegions(DesktopWindowContext<?,?> c){
            if(!(c.state() instanceof State s))return List.of();
            var regions=new ArrayList<DesktopWindowRegion>();
            if(s.upgrades)regions.add(DesktopWindowRegion.visual(c.windowWidth()-4,24,28,82));
            if(s.popupHeight>0)regions.add(DesktopWindowRegion.interactive(c.windowWidth()+28,24,104,s.popupHeight+23));
            return List.copyOf(regions);
        }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,State> c,net.minecraft.client.input.MouseButtonEvent e,boolean twice){
            if(c.state().popupHeight>0){
                int x=c.windowX()+c.windowWidth()+28,y=c.windowY()+24;
                if(e.x()>=x+88&&e.x()<x+99&&e.y()>=y+5&&e.y()<y+16){
                    Object tab=TechRebornScreens.tab(c.model());
                    if(TechRebornScreens.tabName(c.model()).equals("FluidConfigGui"))RebornAccess.call(TechRebornScreens.screen(c.model()),"closeSelectedTab");else RebornAccess.call(tab,"close");return true;
                }
            }
            if(!TechRebornScreens.tabName(c.model()).equals("SlotConfigGui")||TechRebornScreens.popup(c.model())!=null)return false;
            var point=((DesktopNativeContext<?,?>)c).nativeSourceAt(e.x(),e.y());
            if(point.isEmpty()||!point.get().regionId().equals("native-machine"))return false;
            var screen=TechRebornScreens.screen(c.model());
            double x=point.get().x()+(Integer)RebornAccess.call(screen,"getGuiLeft"),y=point.get().y()+(Integer)RebornAccess.call(screen,"getGuiTop");
            return (Boolean)RebornAccess.call(TechRebornScreens.tab(c.model()),"click",x,y,e.button());
        }
        @Override public boolean keyPressed(DesktopInputContext<AbstractContainerMenu,State> c,net.minecraft.client.input.KeyEvent e){
            Object tab=TechRebornScreens.tab(c.model());if(tab==null)return false;
            boolean handled=TechRebornScreens.key(c,tab,e.key(),e.scancode(),e.modifiers());
            if(!handled&&e.key()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE){RebornAccess.call(TechRebornScreens.screen(c.model()),"closeSelectedTab");return true;}return handled;
        }
        @Override public void renderOverlay(DesktopRenderContext<AbstractContainerMenu,State> c){
            if(c.state().popupHeight<=0)return;
            int x=c.windowX()+c.windowWidth()+28,y=c.windowY()+24;
            TechRebornScreens.drawFaces(c);
            c.text(c.state().popupTitle,x+7,y+7,0xFF404040,false);
            boolean hover=c.mouseX()>=x+88&&c.mouseX()<x+99&&c.mouseY()>=y+5&&c.mouseY()<y+16;
            c.texture(Identifier.fromNamespaceAndPath("salts_inventory_update","textures/gui/window_controls.png"),x+88,y+5,22,hover?11:0,11,11,11,11,132,33);
            if(hover)c.tooltip(Component.literal("Close inspector"),c.mouseX(),c.mouseY());
        }
        @Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,State> c,Identifier channel,byte[] data){
            if(!channel.equals(TechRebornWindows.UPDATE))return;
            var b=com.salts_inventory_update.compat.nativewindows.NativePayloads.buffer(data,c.minecraft().player.registryAccess());
            try{
                var codec=(net.minecraft.network.codec.StreamCodec)RebornAccess.staticField(RebornAccess.type("reborncore.common.network.clientbound.ScreenHandlerUpdatePayload"),"PACKET_CODEC");
                Object packet=codec.decode(b);if(b.isReadable())throw new IllegalArgumentException("Reborn update trailing bytes");
                RebornAccess.call(c.model(),"applyScreenHandlerData",RebornAccess.call(packet,"data"));
            }finally{b.release();}
        }

        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return "techreborn-native-right-upgrades-1";}
    }
}
