package com.salts_inventory_update.compat.tomstrading;

import com.salts_inventory_update.api.client.desktop.*;
import net.minecraft.client.input.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import io.netty.buffer.Unpooled;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** The original vending panels, with only the player-inventory band removed. */
public final class TomsTradingWindows {
    private static final Identifier DATA=Identifier.fromNamespaceAndPath("toms_trading_network","data_c2s");
    private static final Identifier POPUP_DATA=Identifier.fromNamespaceAndPath("salts_inventory_compat","toms-trading/popup");
    private static final Map<AbstractContainerMenu,WeakReference<AbstractContainerScreen<?>>> SCREENS=new WeakHashMap<>();
    private static final ThreadLocal<Pointer> POINTER=new ThreadLocal<>();
    private static final Set<String> BOUND=new HashSet<>(),INSTALLED=new HashSet<>();
    private record Pointer(Object screen,Slot slot,DesktopInputContext<AbstractContainerMenu,Void> input) { }
    private TomsTradingWindows() { }
    public static void remember(AbstractContainerScreen<?> s){SCREENS.put(s.getMenu(),new WeakReference<>(s));}
    private static AbstractContainerScreen<?> screen(AbstractContainerMenu m){var r=SCREENS.get(m);return r==null?null:r.get();}
    public static Slot hovered(Object screen){var p=POINTER.get();return p!=null&&p.screen()==screen?p.slot():null;}
    public static boolean menuButton(Object screen,int id){var p=POINTER.get();return p!=null&&p.screen()==screen&&p.input().sendMenuButton(id);}
    public static boolean popupData(CompoundTag tag){
        var p=POINTER.get();if(p==null)return false;var b=new FriendlyByteBuf(Unpooled.buffer());
        try{b.writeNbt(tag);byte[] bytes=new byte[b.readableBytes()];b.readBytes(bytes);return p.input().sendPayload(POPUP_DATA,bytes);}finally{b.release();}
    }
    private static Object field(Object owner,String name){
        for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
        catch(NoSuchFieldException absent){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        throw new IllegalStateException("Missing Tom's Trading field: "+name);
    }
    private static void integer(Object owner,String name,int value){
        try{var f=owner.getClass().getDeclaredField(name);f.setAccessible(true);f.setInt(owner,value);}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
    }
    private static int number(Object o,String method){return ((Number)ForeignMenuAccess.invoke(o,method)).intValue();}
    private static <R> R input(DesktopInputContext<AbstractContainerMenu,Void> c,Slot slot,Supplier<R> action){
        var before=POINTER.get();var screen=screen(c.model());
        var access=(com.salts_inventory_update.compat.nativewindows.mixin.TomsTradingHoverAccess)screen;
        var previousSlot=access.salts_inventory_compat$hoveredSlot();
        POINTER.set(new Pointer(screen,slot,c));access.salts_inventory_compat$hoveredSlot(slot);
        try{return action.get();}finally{access.salts_inventory_compat$hoveredSlot(previousSlot);if(before==null)POINTER.remove();else POINTER.set(before);}
    }
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}
    public static void initializeClient(){install();DesktopClientApi.onClientStarted(mc->{bind();install();});}
    private static synchronized void bind(){
        for(String suffix:List.of("config","trading")){
            String id="toms_trading_network:vending_machine.menu_"+suffix;var type=NativeMenuSupport.type(id);if(type==null||!BOUND.add(id))continue;
            NativeMenuSupport.bindNative(type,false,new DesktopMenuBindings.SlotAccess(){
                @Override public int size(AbstractContainerMenu m){return m.slots.size();}
                @Override public boolean isVirtualSlot(AbstractContainerMenu m,Slot s){return m.slots.indexOf(s)<8;}
            },null);
            DesktopMenuBindings.registerStorage(type,(m,s)->false);
            NativeMenuSupport.support(type,"com.tom.trading.menu.VendingMachine"+(suffix.equals("config")?"Config":"Trading")+"Menu",false);
            if(suffix.equals("config")){
                // Both normal retained widgets and the native ghost-slot popup
                // invoke the original receiver on their authenticated menu.
                DesktopServerPayloadHandler<AbstractContainerMenu> receive=c->{
                    if(c.data().length>16384){c.reject("Vending configuration is too large");return;}
                    var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
                    try{
                        CompoundTag tag=b.readNbt();
                        if(tag==null||b.isReadable()||tag.keySet().stream().anyMatch(k->!Set.of("setName","setItemCount","setPhantom","setSide","mode","auto").contains(k))){c.reject("Invalid vending configuration");return;}
                        if(tag.contains("setSide")&&(tag.getByteOr("setSide",(byte)0)<0||tag.getByteOr("setSide",(byte)0)>=6||tag.getByteOr("mode",(byte)0)<0||tag.getByteOr("mode",(byte)0)>3)){c.reject("Invalid vending side");return;}
                        ForeignMenuAccess.invoke(c.menu(),"receive",new Class<?>[]{CompoundTag.class},tag);c.broadcastChanges();
                    }finally{b.release();}
                };
                DesktopServerApi.registerNativePayload(type,DATA,receive);DesktopServerApi.registerPayload(type,POPUP_DATA,receive);
            }
        }
    }
    private static synchronized void install(){
        for(String suffix:List.of("config","trading")){
            String id="toms_trading_network:vending_machine.menu_"+suffix;var type=NativeMenuSupport.type(id);if(type==null||!INSTALLED.add(id))continue;
            DesktopClientApi.registerNativeMatcher(Identifier.fromNamespaceAndPath("salts_inventory_compat","toms-trading/"+suffix),100,Set.of(type),lookup->true,lookup->new Template(suffix.equals("config")));
        }
    }
    private record Template(boolean config) implements DesktopNativeTemplate<AbstractContainerMenu,Void>,DesktopWindowPresentation {
        private Object popup(AbstractContainerMenu m){var s=screen(m);return config&&s!=null?field(s,"popup"):null;}
        private boolean popupOpen(AbstractContainerMenu m){Object p=popup(m);return p!=null&&field(p,"menu")!=null;}
        private int sourceY(){return config?4:17;}
        private int left(AbstractContainerMenu m){return ((com.salts_inventory_update.compat.nativewindows.mixin.NativeScreenGeometryAccess)screen(m)).salts_inventory_compat$left();}
        private int top(AbstractContainerMenu m){return ((com.salts_inventory_update.compat.nativewindows.mixin.NativeScreenGeometryAccess)screen(m)).salts_inventory_compat$top();}
        private double nativeX(DesktopWindowContext<AbstractContainerMenu,Void> c,double x){return x-c.contentX()+4+left(c.model());}
        private double nativeY(DesktopWindowContext<AbstractContainerMenu,Void> c,double y){return y-c.contentY()+sourceY()+top(c.model());}
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,Void> c){
            var region=new DesktopNativeRegion("original-vending-panel",new DesktopWindowBounds(4,sourceY(),168,config?114:65),0,0);
            // The native trading screen uses the standard inventory caption at
            // y=72; its amount field and Trade button remain completely intact.
            if(!config)region=region.cut(new DesktopWindowBounds(6,71,65,11));
            var b=DesktopNativeLayout.builder().region(region).canvas(168,config?114:65).scrollable(false);
            if(config&&c.nativeView().widgets().stream().anyMatch(w->w.visible()&&w.bounds().y()<0))
                b.exterior("creative-vending",new DesktopWindowBounds(158,-18,16,16),154,-18);
            return b.build();
        }
        @Override public Component title(DesktopWindowContext<AbstractContainerMenu,Void> c){
            var s=screen(c.model());return config&&s!=null?(Component)field(s,"title"):c.originalTitle();
        }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,Void> c,MouseButtonEvent e,boolean twice){
            if(popupOpen(c.model()))return input(c,null,()->(Boolean)ForeignMenuAccess.invoke(popup(c.model()),"mouseClick",new Class<?>[]{double.class,double.class,int.class},nativeX(c,e.x()),nativeY(c,e.y()),e.button()));
            if(!config||e.button()!=1)return false;
            for(int i=0;i<8;i++){
                Slot s=c.model().getSlot(i);double x=c.contentX()+s.x-4,y=c.contentY()+s.y-sourceY();
                if(e.x()>=x-1&&e.x()<x+17&&e.y()>=y-1&&e.y()<y+17&&!s.getItem().isEmpty())
                    return input(c,s,()->screen(c.model()).mouseClicked(new net.minecraft.client.input.MouseButtonEvent(nativeX(c,e.x()),nativeY(c,e.y()),e.buttonInfo()),twice));
            }
            return false;
        }
        @Override public boolean keyPressed(DesktopInputContext<AbstractContainerMenu,Void> c,KeyEvent e){
            return popupOpen(c.model())&&input(c,null,()->(Boolean)ForeignMenuAccess.invoke(popup(c.model()),"keyPressed",new Class<?>[]{KeyEvent.class},e));
        }
        @Override public boolean charTyped(DesktopInputContext<AbstractContainerMenu,Void> c,CharacterEvent e){
            return popupOpen(c.model())&&input(c,null,()->(Boolean)ForeignMenuAccess.invoke(popup(c.model()),"charTyped",new Class<?>[]{CharacterEvent.class},e));
        }
        @Override public boolean wantsTextInput(DesktopWindowContext<AbstractContainerMenu,Void> c){return popupOpen(c.model());}
        @Override public void renderOverlay(DesktopRenderContext<AbstractContainerMenu,Void> c){
            if(!popupOpen(c.model()))return;Object p=popup(c.model());int x=(int)field(p,"x"),y=(int)field(p,"y");
            integer(p,"x",c.contentX()+x-left(c.model())-4);integer(p,"y",c.contentY()+y-top(c.model())-sourceY());
            try{ForeignMenuAccess.invoke(p,"render",new Class<?>[]{GuiGraphicsExtractor.class,net.minecraft.client.gui.Font.class,int.class,int.class},c.nativeGraphics(),c.minecraft().font,c.mouseX(),c.mouseY());}
            finally{integer(p,"x",x);integer(p,"y",y);}
        }
        @Override public boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu,Void> c,int x,int y){return popupOpen(c.model());}
        @Override public List<DesktopPopup> popups(DesktopWindowContext<AbstractContainerMenu,Void> c){
            return popupOpen(c.model())?List.of(new DesktopPopup("native-vending-rule",externalRegions(c),false,true,false,true)):List.of();
        }
        @Override public boolean dismissPopup(DesktopWindowContext<AbstractContainerMenu,Void> c,String id,DesktopPopup.Dismissal why){
            if(!id.equals("native-vending-rule")||!popupOpen(c.model()))return false;ForeignMenuAccess.invoke(popup(c.model()),"close");return true;
        }
        @Override public List<DesktopWindowRegion> externalRegions(DesktopWindowContext<?,?> raw){
            @SuppressWarnings("unchecked") var c=(DesktopWindowContext<AbstractContainerMenu,Void>)raw;if(!popupOpen(c.model()))return List.of();Object p=popup(c.model());
            int x=c.contentX()+(int)field(p,"x")-left(c.model())-4-c.windowX(),y=c.contentY()+(int)field(p,"y")-top(c.model())-sourceY()-c.windowY();
            int rows=((List<?>)field(p,"menu")).size();return List.of(new DesktopWindowRegion(x-4,y-4,180,rows*10+8,true,true,true,DesktopHitRole.CONTROL));
        }
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,Void> c){SCREENS.remove(c.model());}
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,Void> c){return "toms-trading-native-inventory-cut-1";}
    }
}
