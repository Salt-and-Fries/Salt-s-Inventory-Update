package com.salts_inventory_update.compat.zenith;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import com.salts_inventory_update.compat.zenith.ZenithWindows.Kind;
import io.netty.buffer.Unpooled;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.EnchantmentMenu;
import net.minecraft.world.item.enchantment.EnchantmentInstance;

/** Approved native insets, sampled palettes and ordinary SIU management controls. */
public final class ZenithClient {
    static final DesktopInsets INSETS=new DesktopInsets(7,26,7,7);
    private static final DesktopChromeMetrics METRICS=new DesktopChromeMetrics(22,7,8,11,11,3,5,6,0);
    private static final ResourceLocation CONTROLS=new ResourceLocation("salts_inventory_update","textures/gui/window_controls.png");
    private static final Set<Kind> INSTALLED=EnumSet.noneOf(Kind.class);
    private static final Map<AbstractContainerMenu,WeakReference<AbstractContainerScreen<?>>> SCREENS=new WeakHashMap<>();
    private record InformationReturn(Screen parent,DesktopWindowHandle owner) { }
    private static final Map<Screen,InformationReturn> INFORMATION=new WeakHashMap<>();
    private ZenithClient() { }
    public static void initializeClient(){install();DesktopClientApi.onClientStarted(mc->install());}
    private static synchronized void install(){
        for(var kind:Kind.values()){
            var type=NativeMenuSupport.type("zenith:"+kind.path);if(type==null||!INSTALLED.add(kind))continue;
            // Simple menu types already provide their own client factory.
            // Only extended menus require the authenticated block-position bytes.
            if(kind.position)DesktopMenuFactories.register(type,(mc,player,id,data)->ZenithWindows.create(kind,id,player.getInventory(),data));
            DesktopClientApi.registerNativeScreen(type,(menu,inventory,title)->{
                try{
                    for(var constructor:Class.forName(kind.screen).getConstructors()){
                        var p=constructor.getParameterTypes();
                        if(p.length==3&&p[0].isInstance(menu)&&p[1]==Inventory.class&&p[2]==Component.class){
                            var screen=(AbstractContainerScreen<?>)constructor.newInstance(menu,inventory,title);
                            SCREENS.put(menu,new WeakReference<>(screen));return screen;
                        }
                    }
                    throw new NoSuchMethodException(kind.screen);
                }catch(ReflectiveOperationException failure){throw new IllegalStateException("Zenith native screen",failure);}
            });
            DesktopClientApi.registerNativeMatcher(ZenithWindows.id(kind.path),100,Set.of(type),lookup->true,lookup->new Template(kind));
        }
    }
    private static AbstractContainerScreen<?> screen(AbstractContainerMenu menu){var ref=SCREENS.get(menu);return ref==null?null:ref.get();}
    private static boolean informationAvailable(AbstractContainerMenu model){
        if(!(model instanceof EnchantmentMenu menu)||!menu.getSlot(0).hasItem())return false;
        return Arrays.stream(menu.enchantClue).allMatch(id->BuiltInRegistries.ENCHANTMENT.byId(id)!=null);
    }
    static DesktopNativeLayout layout(Kind kind,int titleWidth,boolean info){
        int width=Math.max(kind.width,titleWidth+47);
        var builder=DesktopNativeLayout.builder().canvas(width-INSETS.horizontal(),kind.height-INSETS.vertical()).scrollable(false);
        if(kind.bodyHeight>0)builder.keep("native-working-panel",box(0,0,176,kind.bodyHeight),(width-190)/2,0);
        else if(kind==Kind.FLETCHING)builder.keep("complete-fletching",box(5,14,144,58),(width-176)/2+9,1);
        else{
            builder.keep("enchanting",box(7,13,163,89),(width-178)/2,1);
            if(info)builder.exterior("native-information-tab",box(145,-15,27,15),width-33,-15);
        }
        return builder.build();
    }
    private record Template(Kind kind) implements DesktopNativeTemplate<AbstractContainerMenu,Void> {
        @Override public Component title(DesktopWindowContext<AbstractContainerMenu,Void> c){
            String block=switch(kind){case ENCHANTING->"enchanting_table";case LIBRARY->"library";case FLETCHING->"fletching_table";
                case SALVAGING->"salvaging_table";case GEM_CUTTING->"gem_cutting_table";case REFORGING->"reforging_table";case AUGMENTING->"augmenting_table";};
            if(kind==Kind.REFORGING||kind==Kind.LIBRARY){
                var id=BuiltInRegistries.BLOCK.getKey(c.minecraft().level.getBlockState(ZenithWindows.position(c.model())).getBlock());
                if(id.getNamespace().equals("zenith")&&(id.getPath().equals("simple_reforging_table")||id.getPath().equals("ender_library")))block=id.getPath();
            }
            return Component.translatable("block."+(kind==Kind.ENCHANTING||kind==Kind.FLETCHING?"minecraft.":"zenith.")+block);
        }
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,Void> c){return layout(kind,c.fontWidth(title(c)),kind==Kind.ENCHANTING&&informationAvailable(c.model()));}
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,Void> c){return new DesktopWindowSurface(DesktopSurfaceMode.SALT,INSETS,true,List.of(),DesktopViewportPolicy.CLAMP);}
        @Override public DesktopWindowDecoration<AbstractContainerMenu,Void> decoration(DesktopWindowContext<AbstractContainerMenu,Void> c){
            return new DesktopWindowDecoration<>(){
                @Override public DesktopChromeMetrics metrics(){return METRICS;}
                @Override public void render(DesktopRenderContext<AbstractContainerMenu,Void> draw,List<DesktopControlState> controls){paint(draw,controls,title(draw),kind);}
            };
        }
        @Override public List<DesktopWindowBounds> textInputBounds(DesktopWindowContext<AbstractContainerMenu,Void> c){return kind==Kind.LIBRARY?List.of(box(16,16,110,11)):List.of();}
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,Void> c,MouseButtonEvent e,boolean doubleClick){
            if(kind!=Kind.ENCHANTING||e.button()!=0||!informationAvailable(c.model()))return false;
            var point=((DesktopNativeContext<?,?>)c).nativeSourceAt(e.x(),e.y());
            if(point.isEmpty()||!point.get().regionId().equals("native-information-tab"))return false;
            try{
                var nativeScreen=screen(c.model());
                var info=(Screen)Class.forName(ZenithWindows.BASE+"ench.table.EnchantingInfoScreen").getConstructor(nativeScreen.getClass()).newInstance(nativeScreen);
                var parent=c.minecraft().screen;c.windowHandle().cancelInput();
                INFORMATION.put(info,new InformationReturn(parent,c.windowHandle()));
                c.minecraft().setScreen(info);return true;
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Zenith enchanting information",failure);}
        }
        @Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,Void> c,ResourceLocation channel,byte[] bytes){
            if(kind!=Kind.ENCHANTING||!channel.equals(ZenithWindows.SYNC))return;
            var data=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
            try{
                var stats=new FriendlyByteBuf(Unpooled.wrappedBuffer(data.readByteArray(4096)));
                try{
                    var cls=Class.forName(ZenithWindows.BASE+"ench.table.ApothEnchantmentMenu$TableStats");
                    ZenithWindows.set(c.model(),"stats",cls.getMethod("read",FriendlyByteBuf.class).invoke(null,stats));
                }finally{stats.release();}
                int offers=data.readUnsignedByte();if(offers>3)throw new IllegalArgumentException("Invalid Zenith offer count");
                var s=screen(c.model());
                if(s!=null)((Map<?,?>)ZenithWindows.field(s,"clues")).clear();
                for(int offerIndex=0;offerIndex<offers;offerIndex++){
                    var clue=new FriendlyByteBuf(Unpooled.wrappedBuffer(data.readByteArray(4096)));
                    try{
                        int size=clue.readUnsignedByte();var clues=new ArrayList<EnchantmentInstance>(size);
                        for(int i=0;i<size;i++)clues.add(new EnchantmentInstance(Objects.requireNonNull(BuiltInRegistries.ENCHANTMENT.byId(clue.readUnsignedShort())),clue.readUnsignedByte()));
                        int offer=clue.readUnsignedByte();boolean all=clue.readBoolean();
                        if(offer>2||clue.isReadable())throw new IllegalArgumentException("Invalid Zenith clue update");
                        if(s!=null)s.getClass().getMethod("acceptClues",int.class,List.class,boolean.class).invoke(s,offer,clues,all);
                    }finally{clue.release();}
                }
                if(data.isReadable())throw new IllegalArgumentException("Trailing Zenith state data");
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Zenith enchanting update",failure);}
            finally{data.release();}
        }
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,Void> c){return "zenith-approved-1:"+kind.path;}
    }
    private static void paint(DesktopRenderContext<?,?> c,List<DesktopControlState> controls,Component title,Kind kind){
        boolean gray=kind.bodyHeight==0;
        var texture=gray?new ResourceLocation("salts_inventory_update","textures/gui/window.png")
            :new ResourceLocation("salts_inventory_compat","textures/gui/zenith/"+kind.path+"_window.png");
        c.windowNineSlice(texture,c.windowX(),c.windowY(),c.windowWidth(),c.windowHeight());
        int right=controls.stream().filter(s->s.control()!=DesktopWindowControl.RESIZE_GRIP).mapToInt(s->s.bounds().x()-7).min().orElse(c.windowX()+c.windowWidth()-7);
        c.text(c.trimToWidth(title.getString(),Math.max(0,right-c.windowX()-7)),c.windowX()+7,c.windowY()+8,gray?0xFF404040:0xFFE7E1E8,false);
        for(var s:controls){
            int col=switch(s.control()){case FOCUS->0;case CLOSE->2;case ELLIPSIS->3;case LOCK->s.selected()?5:4;case PIN->6;case LINK->9;default->-1;};
            if(col<0)continue;var b=s.bounds();int row=s.pressed()||s.selected()?2:s.hovered()||s.keyboardFocused()?1:0;
            c.texture(CONTROLS,b.x(),b.y(),col*11,row*11,11,11,11,11,132,33);
        }
    }
    /** Only change the return destination; Zenith owns the entire information screen. */
    public static boolean returnInformation(Screen screen){
        var back=INFORMATION.remove(screen);if(back==null)return false;
        Minecraft.getInstance().setScreen(back.parent());if(back.owner().isOpen())back.owner().focus();return true;
    }
    public static void tickInformation(Screen screen){
        var back=INFORMATION.get(screen);if(back!=null&&!back.owner().isOpen())returnInformation(screen);
    }
    private static DesktopWindowBounds box(int x,int y,int w,int h){return new DesktopWindowBounds(x,y,w,h);}
}
