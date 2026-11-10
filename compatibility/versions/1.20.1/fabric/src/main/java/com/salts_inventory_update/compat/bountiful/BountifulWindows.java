package com.salts_inventory_update.compat.bountiful;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.util.*;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Original bounty previews and board cells, in the compact SIU merchant composition. */
public final class BountifulWindows {
    private static final ResourceLocation SELECT=new ResourceLocation("salts_inventory_compat","bountiful/select");
    private static final Map<AbstractContainerMenu,WeakReference<AbstractContainerScreen<?>>> SCREENS=new WeakHashMap<>();
    private static final Set<Object> PREPARED=Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean bound,installed;
    private BountifulWindows() { }
    public static void remember(AbstractContainerScreen<?> screen){SCREENS.put(screen.getMenu(),new WeakReference<>(screen));}
    private static AbstractContainerScreen<?> screen(AbstractContainerMenu menu){var ref=SCREENS.get(menu);return ref==null?null:ref.get();}
    static Object field(Object owner,String name){
        for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
        catch(NoSuchFieldException absent){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        throw new IllegalStateException("Missing Bountiful field: "+name);
    }
    private static void integer(Object owner,String name,int value){
        for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())try{
            Field f=type.getDeclaredField(name);f.setAccessible(true);f.setInt(owner,value);return;
        }catch(NoSuchFieldException absent){}catch(ReflectiveOperationException failure){throw new IllegalStateException("Bountiful compact viewport: "+name,failure);}
        throw new IllegalStateException("Missing Bountiful integer: "+name);
    }
    public static boolean immediateText(Object dsl,int x,int y,Component text){
        Object gui=field(dsl,"ctx"),s=ForeignMenuAccess.invoke(gui,"getScreen");
        if(!PREPARED.contains(s))return false;
        var graphics=(GuiGraphics)field(dsl,"context");var font=Minecraft.getInstance().font;
        int absX=((Number)ForeignMenuAccess.invoke(gui,"absX",new Class<?>[]{int.class},x)).intValue();
        int absY=((Number)ForeignMenuAccess.invoke(gui,"absY",new Class<?>[]{int.class},y)).intValue();
        // Upstream starts a fresh PoseStack here, losing the native capture's
        // translation. Keep its original coordinates, count style and z=201,
        // but use the original GUI's pose so the complete quantity is retained.
        graphics.pose().pushPose();
        try{graphics.pose().translate(0,0,201);graphics.drawString(font,text,absX+font.width(text),absY,0xFFFFFFFF,true);}
        finally{graphics.pose().popPose();}
        return true;
    }
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}
    public static void initializeClient(){install();DesktopClientApi.onClientStarted(mc->{bind();install();});}
    private static synchronized void bind(){
        var type=NativeMenuSupport.type("bountiful:board");if(type==null||bound)return;
        NativeMenuSupport.bindNative(type,false,new DesktopMenuBindings.SlotAccess(){
            @Override public int size(AbstractContainerMenu menu){return menu.slots.size();}
            // Bounties are per-player board offers, not sortable stored items.
            // The hidden selected-offer cell is an alias of a board bounty.
            @Override public boolean isVirtualSlot(AbstractContainerMenu menu,Slot slot){return menu.slots.indexOf(slot)<22;}
        },BountifulWindows::prepareSnapshot);
        DesktopMenuBindings.registerStorage(type,(menu,slot)->false);
        DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){
            @Override public boolean supports(net.minecraft.server.level.ServerPlayer player,AbstractContainerMenu menu){return menu.getClass().getName().equals("io.ejekta.bountiful.content.gui.BoardScreenHandler");}
            @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu menu){return true;}
        });
        DesktopServerApi.registerPayload(type,SELECT,c->{
            byte[] data=c.data();if(data.length!=1||(data[0]&255)>=21){c.reject("Invalid board selection");return;}
            // Use the authenticated session's inventory. The original native
            // packet trusts a UUID string; no player identity is supplied here.
            ForeignMenuAccess.invoke(ForeignMenuAccess.invoke(c.menu(),"getInventory"),"select",new Class<?>[]{int.class},data[0]&255);
            c.broadcastChanges();
        });
        bound=true;
    }
    private static void prepareSnapshot(AbstractContainerMenu menu,List<ItemStack> items){
        if(menu.slots.size()<25||menu.getSlot(21).x!=500216)return;
        Slot old=menu.getSlot(21);
        try{
            // Preserve the exact upstream slot type, container and -1 alias.
            // Only its off-screen coordinate changes for bounded native geometry.
            Slot hidden=(Slot)old.getClass().getConstructor(old.container.getClass(),int.class,int.class,int.class)
                .newInstance(old.container,-1,-9999,31);
            hidden.index=old.index;menu.slots.set(21,hidden);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Bountiful selected-offer alias",failure);}
    }
    private static synchronized void install(){
        var type=NativeMenuSupport.type("bountiful:board");if(type==null||installed)return;
        DesktopClientApi.registerNativeMatcher(new ResourceLocation("salts_inventory_compat","bountiful/board"),100,Set.of(type),lookup->true,lookup->new Template());
        installed=true;
    }
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,Void>,DesktopWindowPresentation {
        private boolean draggingScrollbar;
        private void scrollbar(DesktopInputContext<AbstractContainerMenu,Void> c,double y){
            Object scroller=field(screen(c.model()),"scroller");
            int knob=((Number)ForeignMenuAccess.invoke(scroller,"getKnobSize")).intValue();
            // Kambrik's click callback returns the vanilla screen's result,
            // discarding its reactor's consumption. Capture this narrow track
            // locally while retaining the native thumb, range and list logic.
            integer(scroller,"dragStart",Math.max(0,Math.min(80-knob,(int)y-c.contentY()-14-knob/2)));
        }
        @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu,Void> c){
            var s=screen(c.model());if(s!=null&&PREPARED.add(s)){
                // Only this hosted instance changes; ordinary native boards keep
                // seven rows. Original list filtering, wheel and thumb stay native.
                integer(field(s,"buttonList"),"shown",4);integer(field(s,"scroller"),"height",80);
            }
        }
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,Void> c){
            return DesktopNativeLayout.builder()
                .keep("bounty-previews",new DesktopWindowBounds(5,18,160,80),0,14)
                .keep("bounty-scrollbar",new DesktopWindowBounds(166,18,6,80),163,14)
                // Include the complete 20px selection highlight around end cells.
                .keep("posted-bounties",new DesktopWindowBounds(179,16,128,56),180,14)
                .keep("decrees",new DesktopWindowBounds(315,16,20,56),316,14)
                .keep("reputation",new DesktopWindowBounds(174,72,134,11),175,83)
                .canvas(336,98).scrollable(false).build();
        }
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,Void> c){
            return new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(6,16,6,6),true,List.of(),DesktopViewportPolicy.CLAMP);
        }
        @Override public DesktopWindowDecoration<AbstractContainerMenu,Void> decoration(DesktopWindowContext<AbstractContainerMenu,Void> c){return (draw,controls)->BountifulChrome.paint(draw,controls);}
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,Void> c){
            int x=c.contentX(),y=c.contentY();
            c.text("Bounties",x+(160-c.fontWidth("Bounties"))/2,y,BountifulChrome.TEXT,false);
            c.text("Board",x+180+(128-c.fontWidth("Board"))/2,y,BountifulChrome.TEXT,false);
            c.text("Decrees",x+336-c.fontWidth("Decrees"),y,BountifulChrome.TEXT,false);
            c.text("Reputation",x+205+(102-c.fontWidth("Reputation"))/2,y+73,BountifulChrome.TEXT,false);
        }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,Void> c,MouseButtonEvent e,boolean twice){
            if(e.button()==0&&e.x()>=c.contentX()+163&&e.x()<c.contentX()+169&&e.y()>=c.contentY()+14&&e.y()<c.contentY()+94){
                draggingScrollbar=true;scrollbar(c,e.y());return true;
            }
            if(e.button()!=0||e.x()<c.contentX()||e.x()>=c.contentX()+160||e.y()<c.contentY()+14||e.y()>=c.contentY()+94)return false;
            Object s=screen(c.model()),list=field(s,"buttonList");
            Object range=ForeignMenuAccess.invoke(list,"getShownRange");
            int first=((Number)ForeignMenuAccess.invoke(range,"getFirst")).intValue();
            @SuppressWarnings("unchecked") var buttons=(List<Object>)ForeignMenuAccess.invoke(s,"getValidButtons");
            int row=first+((int)e.y()-c.contentY()-14)/20;if(row>=buttons.size())return true;
            Object button=buttons.get(row);int index=((Number)ForeignMenuAccess.invoke(button,"getBountyIndex")).intValue();
            if(c.sendPayload(SELECT,new byte[]{(byte)index})){
                ForeignMenuAccess.invoke(ForeignMenuAccess.invoke(c.model(),"getInventory"),"select",new Class<?>[]{int.class},index);
                ForeignMenuAccess.invoke(list,"select",new Class<?>[]{List.class},List.of(button));
            }
            return true;
        }
        @Override public boolean mouseDragged(DesktopInputContext<AbstractContainerMenu,Void> c,MouseButtonEvent e,double dx,double dy){
            if(!draggingScrollbar||e.button()!=0)return false;scrollbar(c,e.y());return true;
        }
        @Override public boolean mouseReleased(DesktopInputContext<AbstractContainerMenu,Void> c,MouseButtonEvent e){
            if(!draggingScrollbar||e.button()!=0)return false;draggingScrollbar=false;return true;
        }
        @Override public void cancelPointer(DesktopWindowContext<?,?> c){draggingScrollbar=false;}
        @Override public List<DesktopScrollRegion> controllerScrollRegions(DesktopWindowContext<AbstractContainerMenu,Void> c){
            Object s=screen(c.model()),range=ForeignMenuAccess.invoke(field(s,"buttonList"),"getShownRange");
            int first=((Number)ForeignMenuAccess.invoke(range,"getFirst")).intValue();
            int count=((List<?>)ForeignMenuAccess.invoke(s,"getValidButtons")).size();
            return List.of(new DesktopScrollRegion(c.contentX(),c.contentY()+14,169,80,20,first>0,first+4<count));
        }
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,Void> c){PREPARED.remove(screen(c.model()));SCREENS.remove(c.model());}
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,Void> c){return "bountiful-compact-board-1";}
    }
}
