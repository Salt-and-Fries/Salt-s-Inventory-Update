package com.salts_inventory_update.compat.alexsmobs;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import io.netty.buffer.Unpooled;
import java.lang.ref.WeakReference;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** The original machine panel is the window, including its native title row. */
public final class TransmutationWindows {
    private static final String MENU="alexsmobs:transmutation_table";
    private static final ResourceLocation CHOICE=new ResourceLocation("salts_inventory_compat","alexsmobs/transmute");
    private static final ResourceLocation CHOICES=new ResourceLocation("salts_inventory_compat","alexsmobs/choices");
    private static final ResourceLocation TEXTURE=new ResourceLocation("alexsmobs","textures/gui/transmutation_table.png");
    private static final ResourceLocation CONTROLS=new ResourceLocation("salts_inventory_update","textures/gui/window_controls.png");
    private static final int HEADER=16;
    private static final DesktopChromeMetrics CHROME=new DesktopChromeMetrics(HEADER,6,6,11,11,3,4,3,0);
    private static final Map<AbstractContainerMenu,WeakReference<AbstractContainerScreen<?>>> SCREENS=new WeakHashMap<>();
    private static final ThreadLocal<DesktopInputContext<?,?>> INPUT=new ThreadLocal<>();
    private static boolean bound,installed;
    private TransmutationWindows() { }
    public static void remember(AbstractContainerScreen<?> screen){SCREENS.put(screen.getMenu(),new WeakReference<>(screen));}
    private static AbstractContainerScreen<?> screen(AbstractContainerMenu menu){var ref=SCREENS.get(menu);return ref==null?null:ref.get();}
    private static Object field(Object owner,String name){
        for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
        catch(NoSuchFieldException absent){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        throw new IllegalStateException("Missing Transmutation Table field: "+name);
    }
    private static Object proxy(){try{return Class.forName("com.github.alexthe666.alexsmobs.AlexsMobs").getField("PROXY").get(null);}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}}
    private static <R> R input(DesktopInputContext<?,?> c,Supplier<R> action){var previous=INPUT.get();INPUT.set(c);try{return action.get();}finally{if(previous==null)INPUT.remove();else INPUT.set(previous);}}
    /** Intercepts only a native choice callback while this adapter dispatches input. */
    public static boolean choose(AbstractContainerMenu menu,int choice){
        var c=INPUT.get();if(c==null||c.model()!=menu)return false;
        if(choice>=0&&choice<3)c.sendPayload(CHOICE,new byte[]{(byte)choice});return true;
    }
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}
    public static void initializeClient(){install();DesktopClientApi.onClientStarted(mc->{bind();install();});}
    private static final class Sync {byte[] previous;}
    private static synchronized void bind(){
        var type=NativeMenuSupport.type(MENU);if(type==null||bound)return;
        NativeMenuSupport.bindNative(type,false);DesktopMenuBindings.registerStorage(type,(menu,slot)->false);
        DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Sync>(){
            @Override public boolean supports(net.minecraft.server.level.ServerPlayer p,AbstractContainerMenu menu){return menu.getClass().getName().equals("com.github.alexthe666.alexsmobs.inventory.MenuTransmutationTable")&&menu.slots.size()==37;}
            @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu menu){return true;}
            @Override public Sync createState(DesktopServerSessionContext<AbstractContainerMenu,Sync> c){return new Sync();}
            @Override public void tick(DesktopServerSessionContext<AbstractContainerMenu,Sync> c){
                Object table=field(c.menu(),"table");if(table==null)return;
                var b=new FriendlyByteBuf(Unpooled.buffer());
                try{
                    for(int i=0;i<3;i++)b.writeItem((ItemStack)ForeignMenuAccess.invoke(table,"getPossibility",new Class<?>[]{int.class},i));
                    byte[] data=new byte[b.readableBytes()];b.readBytes(data);
                    if(!Arrays.equals(data,c.state().previous)){c.sendToClient(CHOICES,data);c.state().previous=data;}
                }finally{b.release();}
            }
        });
        DesktopServerApi.registerPayload(type,CHOICE,c->{
            byte[] data=c.data();if(data.length!=1||(data[0]&255)>2){c.reject("Invalid transmutation choice");return;}
            if(c.menu().getSlot(0).getItem().isEmpty())return;
            Object table=field(c.menu(),"table");int choice=data[0]&255;
            if(table==null||!Boolean.TRUE.equals(ForeignMenuAccess.invoke(table,"hasPossibilities"))||((ItemStack)ForeignMenuAccess.invoke(table,"getPossibility",new Class<?>[]{int.class},choice)).isEmpty())return;
            // Keep upstream XP validation, conversion counts, learning, rerolls,
            // advancements and sound. No player or world identity comes from input.
            ForeignMenuAccess.invoke(c.menu(),"transmute",new Class<?>[]{Player.class,int.class},c.player(),choice);c.broadcastChanges();
        });bound=true;
    }
    private static synchronized void install(){
        var type=NativeMenuSupport.type(MENU);if(type==null||installed)return;
        DesktopClientApi.registerNativeMatcher(new ResourceLocation("salts_inventory_compat","alexsmobs/transmutation"),100,Set.of(type),lookup->true,lookup->new Template());installed=true;
    }
    static final class State {final transient ItemStack[] choices={ItemStack.EMPTY,ItemStack.EMPTY,ItemStack.EMPTY};}
    private static void restore(DesktopWindowContext<AbstractContainerMenu,State> c){
        Object proxy=proxy();for(int i=0;i<3;i++)ForeignMenuAccess.invoke(proxy,"setDisplayTransmuteResult",new Class<?>[]{int.class,ItemStack.class},i,c.state().choices[i]);
        // The upstream proxy is global. Reconcile each detached screen's same
        // native visibility rule after restoring its own choices, so another
        // open table cannot hide/show this table's controls during native ticks.
        var s=screen(c.model());if(s!=null){int choice=0;for(var child:s.children())if(child instanceof AbstractWidget w&&w.getClass().getSimpleName().equals("ButtonTransmute")&&choice<3)w.visible=!c.model().getSlot(0).getItem().isEmpty()&&!c.state().choices[choice++].isEmpty();}
    }
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,State>,DesktopWindowPresentation {
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c){return new State();}
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c){
            // The native first 16 rows are the window's own draggable header.
            // Body rows retain their original position and end before inventory.
            return DesktopNativeLayout.builder().keep("original-table",new DesktopWindowBounds(0,HEADER,176,107-HEADER),0,0)
                // The lower curve exposes three gray inventory pixels and its
                // continuing outer edge per corner. Remove only those steps;
                // retain every pixel of the native purple curve and bottom lip.
                .hide(new DesktopWindowBounds(0,105,2,1))
                .hide(new DesktopWindowBounds(0,106,3,1))
                .hide(new DesktopWindowBounds(174,105,2,1))
                .hide(new DesktopWindowBounds(173,106,3,1))
                .canvas(176,107-HEADER).scrollable(false).build();
        }
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c){return new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(0,HEADER,0,0),true,List.of(),DesktopViewportPolicy.CLAMP);}
        @Override public DesktopWindowDecoration<AbstractContainerMenu,State> decoration(DesktopWindowContext<AbstractContainerMenu,State> c){return new DesktopWindowDecoration<>(){
            @Override public DesktopChromeMetrics metrics(){return CHROME;}
            @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> draw,List<DesktopControlState> controls){
            int x=draw.windowX(),y=draw.windowY();
            // The original unlabeled atlas restores this exact native header,
            // so no centered title remnants, extra strip or painted-over border.
            draw.texture(TEXTURE,x,y,0,0,176,HEADER,176,HEADER,256,256);
            int right=controls.stream().filter(s->s.control()!=DesktopWindowControl.RESIZE_GRIP).mapToInt(s->s.bounds().x()-3).min().orElse(x+170);
            draw.text(draw.trimToWidth(draw.originalTitle().getString(),Math.max(0,right-x-CHROME.titleLeftPadding())),x+CHROME.titleLeftPadding(),y+CHROME.titleTextY(),0xff4eff21,false);
            for(var state:controls){int column=switch(state.control()){case FOCUS->0;case SORT_INVENTORY->1;case CLOSE->2;case ELLIPSIS->3;case LOCK->5;case PIN->6;case LINK->9;case SORT_WINDOWS->10;default->-1;};
                if(column<0)continue;var b=state.bounds();draw.texture(CONTROLS,b.x(),b.y(),column*11,(state.pressed()||state.selected()?2:state.hovered()||state.keyboardFocused()?1:0)*11,b.width(),b.height(),11,11,132,33);
            }
        }};}
        @Override public void renderBehindFrame(DesktopRenderContext<?,?> c){@SuppressWarnings("unchecked")var typed=(DesktopWindowContext<AbstractContainerMenu,State>)c;restore(typed);}
        @Override public void tick(DesktopWindowContext<AbstractContainerMenu,State> c){restore(c);}
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e,boolean twice){
            restore(c);var p=((DesktopNativeContext<?,?>)c).nativeSourceAt(e.x(),e.y());var s=screen(c.model());if(p.isEmpty()||s==null)return false;
            double x=p.get().x()+((Number)ForeignMenuAccess.invoke(s,"getGuiLeft")).intValue(),y=p.get().y()+((Number)ForeignMenuAccess.invoke(s,"getGuiTop")).intValue();
            for(var child:s.children())if(child instanceof AbstractWidget w&&w.getClass().getSimpleName().equals("ButtonTransmute")&&w.visible&&w.active&&w.isMouseOver(x,y)){
                boolean handled=input(c,()->w.mouseClicked(x,y,e.button()));if(handled)s.setFocused(w);return handled;
            }return false;
        }
        @Override public boolean keyPressed(DesktopInputContext<AbstractContainerMenu,State> c,KeyEvent e){var s=screen(c.model());if(s==null)return false;var focused=s.getFocused();return focused!=null&&focused.getClass().getSimpleName().equals("ButtonTransmute")&&input(c,()->focused.keyPressed(e.key(),e.scancode(),e.modifiers()));}
        @Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,State> c,ResourceLocation channel,byte[] data){
            if(!channel.equals(CHOICES))return;var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            try{ItemStack[] items={b.readItem(),b.readItem(),b.readItem()};if(b.isReadable())throw new IllegalArgumentException("Trailing transmutation choices");System.arraycopy(items,0,c.state().choices,0,3);restore(c);}finally{b.release();}
        }
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){SCREENS.remove(c.model());}
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return "alexsmobs-native-panel-clean-corners-3";}
    }
}
