package com.salts_inventory_update.compat.ssn;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import com.salts_inventory_update.compat.ssn.SsnLayouts.Kind;
import io.netty.buffer.Unpooled;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Native pixels, input and callbacks composed inside one ordinary SIU window. */
public final class SsnClient {
    private static final DesktopInsets INSETS=new DesktopInsets(7,26,7,7);
    private static final Set<Kind> INSTALLED=EnumSet.noneOf(Kind.class);
    private static final Map<AbstractContainerMenu,AbstractContainerScreen<?>> SCREENS=new IdentityHashMap<>();
    private static final Map<AbstractContainerMenu,DesktopInputContext<AbstractContainerMenu,State>> CONTEXTS=new IdentityHashMap<>();
    private static final Map<AbstractContainerMenu,List<Object>> PENDING=new IdentityHashMap<>();
    private static final Map<Object,AbstractContainerScreen<?>> WIDGETS=new WeakHashMap<>();
    private static final ThreadLocal<AbstractContainerMenu> CONSTRUCTING=new ThreadLocal<>(),ACTIVE=new ThreadLocal<>();
    private SsnClient() { }
    public static void initializeClient(){install();DesktopClientApi.onClientStarted(mc->install());}
    private static synchronized void install(){
        SsnWindows.bind();
        for(var kind:Kind.values()){
            var type=NativeMenuSupport.type("storagenetwork:"+kind.path);if(type==null||!INSTALLED.add(kind))continue;
            DesktopMenuFactories.register(type,(mc,player,id,data)->SsnWindows.create(kind,id,player.getInventory(),data));
            DesktopClientApi.registerNativeScreen(type,(menu,inventory,title)->{
                var previous=CONSTRUCTING.get();CONSTRUCTING.set(menu);
                try{
                    for(var ctor:Class.forName(kind.screen).getConstructors()){
                        var p=ctor.getParameterTypes();if(p.length==3&&p[0].isInstance(menu)&&p[1]==Inventory.class&&p[2]==Component.class){
                            var screen=(AbstractContainerScreen<?>)ctor.newInstance(menu,inventory,title);SCREENS.put(menu,screen);return screen;
                        }
                    }
                    throw new NoSuchMethodException(kind.screen);
                }catch(ReflectiveOperationException failure){throw new IllegalStateException("SSN native screen: "+kind.path,failure);}
                finally{if(previous==null)CONSTRUCTING.remove();else CONSTRUCTING.set(previous);}
            });
            DesktopClientApi.registerNativeMatcher(SsnWindows.id(kind.path),100,Set.of(type),lookup->true,lookup->new Template(kind));
        }
    }
    /** Bracket publisher callbacks, including lambdas on native settings buttons. */
    public static boolean hosted(AbstractContainerScreen<?> screen){return CONTEXTS.containsKey(screen.getMenu());}
    public static void trackWidgets(AbstractContainerScreen<?> screen){for(var child:screen.children())WIDGETS.put(child,screen);}
    public static <T>T nativeWidgetCall(Object widget,Supplier<T> callback){var screen=WIDGETS.get(widget);return screen==null?callback.get():nativeCall(screen,callback);}
    public static <T>T nativeCall(Object screen,Supplier<T> callback){
        if(!(screen instanceof AbstractContainerScreen<?> s)||!CONTEXTS.containsKey(s.getMenu()))return callback.get();
        var previous=ACTIVE.get();ACTIVE.set(s.getMenu());
        try{return callback.get();}finally{if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);}
    }
    public static boolean send(Object message){
        if(!SsnPackets.serverMessage(message))return false;
        var menu=ACTIVE.get();if(menu==null)menu=CONSTRUCTING.get();if(menu==null)return false;
        var context=CONTEXTS.get(menu);
        if(context==null){PENDING.computeIfAbsent(menu,k->new ArrayList<>()).add(message);return true;}
        if(context.windowHandle().isOpen())context.sendPayload(SsnWindows.ACTION,SsnPackets.encode(message,context.minecraft().player));
        return true; // A closing owner must not fall through to another native menu.
    }
    private static Object network(Object screen){return SsnWindows.invoke(screen,"getNetwork");}
    public static boolean searchFocused(Object screen){
        if(!screen.getClass().getName().startsWith(SsnLayouts.BASE))return false;
        try{return ((EditBox)SsnWindows.field(network(screen),"searchBar")).isFocused();}catch(IllegalStateException absent){return false;}
    }
    public static final class State {int sequence,total,received;byte[] pending;}
    private record Template(Kind kind)implements DesktopNativeTemplate<AbstractContainerMenu,State>{
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c){return new State();}
        @Override public Component title(DesktopWindowContext<AbstractContainerMenu,State> c){return Component.translatable(kind.titleKey());}
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c){return SsnLayouts.layout(kind,c.fontWidth(title(c)));}
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c){return new DesktopWindowSurface(DesktopSurfaceMode.SALT,INSETS,true,List.of(),DesktopViewportPolicy.CLAMP);}
        @Override @SuppressWarnings("unchecked")public void opened(DesktopWindowContext<AbstractContainerMenu,State> c){CONTEXTS.put(c.model(),(DesktopInputContext<AbstractContainerMenu,State>)c);var screen=SCREENS.get(c.model());if(screen!=null)trackWidgets(screen);}
        @Override public void tick(DesktopWindowContext<AbstractContainerMenu,State> c){
            var queue=PENDING.remove(c.model());if(queue==null)return;
            var input=CONTEXTS.get(c.model());if(input!=null)for(var message:queue)input.sendPayload(SsnWindows.ACTION,SsnPackets.encode(message,c.minecraft().player));
        }
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){CONTEXTS.remove(c.model());PENDING.remove(c.model());SCREENS.remove(c.model());WIDGETS.entrySet().removeIf(e->e.getValue().getMenu()==c.model());c.state().pending=null;}
        @Override public boolean wantsTextInput(DesktopWindowContext<AbstractContainerMenu,State> c){var s=SCREENS.get(c.model());return kind.network&&s!=null&&searchFocused(s);}
        @Override public void keyboardFocusChanged(DesktopWindowContext<AbstractContainerMenu,State> c,boolean focused){
            var s=SCREENS.get(c.model());if(!kind.network||s==null)return;
            var box=(EditBox)SsnWindows.field(network(s),"searchBar");
            box.setFocused(focused&&Boolean.TRUE.equals(SsnWindows.invoke(s,"getAutoFocus")));
        }
        @Override public List<DesktopWindowBounds> textInputBounds(DesktopWindowContext<AbstractContainerMenu,State> c){
            var s=SCREENS.get(c.model());if(!kind.network||s==null)return List.of();
            var b=(EditBox)SsnWindows.field(network(s),"searchBar");
            return List.of(new DesktopWindowBounds(b.getX()-(int)SsnWindows.invoke(s,"getGuiLeft"),b.getY()-(int)SsnWindows.invoke(s,"getGuiTop"),b.getWidth(),b.getHeight()));
        }
        @Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,State> c,Identifier channel,byte[] bytes){
            if(!channel.equals(SsnWindows.UPDATE))return;var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));var state=c.state();
            try{
                int sequence=b.readInt(),total=b.readInt(),offset=b.readInt();
                if(total<=0||total>16*1024*1024||offset<0||offset+b.readableBytes()>total)throw new IllegalArgumentException("Invalid SSN update fragment");
                if(offset==0){state.sequence=sequence;state.total=total;state.received=0;state.pending=new byte[total];}
                if(state.pending==null||sequence!=state.sequence||total!=state.total||offset!=state.received)throw new IllegalArgumentException("Out-of-order SSN update");
                int length=b.readableBytes();b.readBytes(state.pending,offset,length);state.received+=length;
                if(state.received==total){
                    var screen=SCREENS.get(c.model());if(screen!=null)SsnPackets.receiveClient(screen,state.pending);state.pending=null;
                }
            }finally{b.release();}
        }
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return "ssn-approved-1:"+kind.path;}
    }
}
