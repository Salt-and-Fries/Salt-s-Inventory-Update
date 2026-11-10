package com.salts_inventory_update.compat.quark;

import java.nio.ByteBuffer;
import java.util.*;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.*;

/** Explicit compositions of the original Quark artwork, with complete native slot rims. */
public final class QuarkClient {
    private static final Set<String> INSTALLED=new HashSet<>();
    private static final Map<AbstractContainerMenu,DesktopInputContext<AbstractContainerMenu,State>> SENDERS=new IdentityHashMap<>();
    private static final Set<AbstractContainerMenu> HOSTED=Collections.newSetFromMap(new IdentityHashMap<>());
    private QuarkClient() { }
    public static void initialize() {
        DesktopMenuFactories.register(QuarkWindows.BACKPACK,(mc,p,id,data)->{
            var b=new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data));
            try { var m=QuarkBackpackMenu.read(id,p.getInventory(),b);if(b.isReadable())throw new IllegalArgumentException("Backpack opening data");return m; }
            finally { b.release(); }
        });
        DesktopClientApi.registerWindow(QuarkWindows.BACKPACK,new QuarkBackpackWindow());
        DesktopClientApi.registerGeometryResetPolicy(QuarkWindows.id("quark/paired_geometry"), id -> !QuarkBackpackLink.paired(id));
        DesktopClientApi.onClientTick(QuarkBackpackLink::tick);
        install();DesktopClientApi.onClientStarted(mc->install());
    }
    private static synchronized void install() {
        for(String path:List.of("crate","feeding_trough","held_shulker_box","matrix_enchanting")) {
            var type=NativeMenuSupport.type("quark:"+path);if(type==null||!INSTALLED.add(path))continue;
            String factory=switch(path) {
                case "crate"->"addons.oddities.inventory.CrateMenu";
                case "held_shulker_box"->null;
                case "matrix_enchanting"->"addons.oddities.inventory.MatrixEnchantingMenu";
                default->null;
            };
            if(path.equals("held_shulker_box"))DesktopMenuFactories.register(type,(mc,p,id,data)->{
                var b=new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data));
                try { return (AbstractContainerMenu)QuarkAccess.type("content.management.inventory.HeldShulkerBoxMenu").getConstructor(int.class,net.minecraft.world.entity.player.Inventory.class,FriendlyByteBuf.class).newInstance(id,p.getInventory(),b); }
                catch(ReflectiveOperationException e) { throw new IllegalStateException("Quark held-box factory",e); }
                finally { b.release(); }
            });
            if(path.equals("feeding_trough"))DesktopMenuFactories.register(type,(mc,p,id,data)->{
                if(data.length!=1||data[0]!=1)throw new IllegalArgumentException("Trough opening version");
                return new DispenserMenu(id,p.getInventory()) { @Override public MenuType<?> getType() { return type; } };
            });
            if(factory!=null)DesktopMenuFactories.register(type,(mc,p,id,data)->{
                var b=new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data));
                try { return (AbstractContainerMenu)QuarkAccess.call(QuarkAccess.type(factory),"fromNetwork",id,p.getInventory(),b); }
                finally { b.release(); }
            });
            DesktopClientApi.registerNativeMatcher(QuarkWindows.id("quark/"+path),100,Set.of(type),lookup->true,lookup->new Template(path));
        }
    }
    public static boolean hosted(AbstractContainerMenu menu) { return HOSTED.contains(menu); }
    public static boolean useInventoryPair() { return QuarkWindows.BACKPACK!=null&&DesktopClientApi.isDesktopAvailable(); }
    public static boolean send(AbstractContainerMenu menu,Object message) {
        if(!HOSTED.contains(menu))return false;
        var c=SENDERS.get(menu);if(c==null||!c.windowHandle().isOpen())return true;
        byte[] data;
        if(message.getClass().getSimpleName().equals("ScrollCrateMessage"))data=new byte[]{(byte)((Boolean)QuarkAccess.field(message,"down")?1:0)};
        else if(message.getClass().getSimpleName().equals("MatrixEnchanterOperationMessage")) {
            var b=ByteBuffer.allocate(16);for(String f:List.of("operation","arg0","arg1","arg2"))b.putInt((Integer)QuarkAccess.field(message,f));data=b.array();
        } else return false;
        c.sendPayload(QuarkWindows.ACTION,data);return true;
    }
    public static void originalSend(Object packet,Object additional) {
        try { QuarkAccess.call(Class.forName("net.neoforged.neoforge.network.PacketDistributor"),"sendToServer",packet,additional); }
        catch(ClassNotFoundException e) { throw new IllegalStateException(e); }
    }
    static final class State { EditBox search; }
    private record Template(String kind) implements DesktopNativeTemplate<AbstractContainerMenu,State> {
        @Override public Component title(DesktopWindowContext<AbstractContainerMenu,State> c) { return kind.equals("matrix_enchanting")?Component.literal("Matrix Enchanting"):c.originalTitle(); }
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c) {
            var s=new State();if(kind.equals("crate")) {
                s.search=new EditBox(net.minecraft.client.Minecraft.getInstance().font,0,0,125,12,Component.literal("Search storage"));
                s.search.setMaxLength(50);s.search.setBordered(false);s.search.setTextColor(0xff404040);
            }return s;
        }
        @Override public void opened(DesktopWindowContext<AbstractContainerMenu,State> c) { HOSTED.add(c.model()); }
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c) { HOSTED.remove(c.model());SENDERS.remove(c.model()); }
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c) { return "quark-reviewed-1:"+kind; }
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c) {
            return new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(7,24,7,kind.equals("matrix_enchanting")?7:8),true,List.of(),DesktopViewportPolicy.CLAMP);
        }
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c) {
            var b=DesktopNativeLayout.builder().scrollable(false);
            switch(kind) {
                case "crate" -> b.canvas(186,132).region(new DesktopNativeRegion("crate-body",box(7,17,184,113),1,19).cut(box(7,125,165,5)));
                case "feeding_trough" -> b.canvas(130,56).keep("nine-native-feeding-cells",box(61,16,54,54),38,2);
                
                case "held_shulker_box" -> b.canvas(162,56).keep("shulker-storage",box(7,17,162,54),0,2);
                case "matrix_enchanting" -> b.canvas(166,67).region(new DesktopNativeRegion("matrix-puzzle",box(5,9,166,67),0,0)
                    .cut(box(6,9,73,5)).cut(box(6,72,73,4)));
                default -> throw new IllegalArgumentException(kind);
            }return b.build();
        }
        private static DesktopWindowBounds box(int x,int y,int w,int h) { return new DesktopWindowBounds(x,y,w,h); }
        private boolean search(DesktopWindowContext<?,?> c,double x,double y) { return c.state() instanceof State s&&s.search!=null&&x>=c.windowX()+7&&x<c.windowX()+140&&y>=c.windowY()+24&&y<c.windowY()+38; }
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> c) {
            if(c.state().search==null)return;
            int x=c.windowX()+7,y=c.windowY()+24;var edit=c.state().search;
            c.fill(x,y,x+133,y+14,0xff373737);c.fill(x+1,y+1,x+132,y+13,0xff8b8b8b);
            c.fill(x,y+13,x+133,y+14,0xffffffff);c.fill(x+132,y,x+133,y+14,0xffffffff);
            edit.setX(x+3);edit.setY(y+3);edit.render(c.nativeGraphics(),c.mouseX(),c.mouseY(),0);
            if(edit.getValue().isEmpty()&&!edit.isFocused())c.text("Search...",x+3,y+3,0xff555555,false);
            String count=QuarkAccess.call(c.model(),"getTotal")+"/"+QuarkAccess.field(QuarkAccess.type("addons.oddities.module.CrateModule"),"maxItems");
            c.text(count,c.windowX()+191-c.fontWidth(count),y+3,0xff404040,false);
            if(!edit.getValue().isBlank()) {
                Object zeta=QuarkAccess.field(QuarkAccess.type("base.Quark"),"ZETA");
                Object module=QuarkAccess.call(QuarkAccess.field(zeta,"modules"),"get",QuarkAccess.type("content.client.module.ChestSearchingModule"));
                int overlay=(Integer)QuarkAccess.call(QuarkAccess.field(module,"overlayColor"),"getColor"),matches=0;
                c.rawGraphics().nextStratum();
                for(var slot:((DesktopNativeContext<?,?>)c).nativeView().slots())if(!slot.player()&&slot.active()) {
                    var stack=c.menuSlot(slot.menuSlotId()).getItem();
                    if(!stack.isEmpty()&&!(Boolean)QuarkAccess.call(module,"namesMatch",stack,edit.getValue())) {
                        int sx=c.windowX()+9+slot.bounds().x()-7,sy=c.windowY()+44+slot.bounds().y()-17;
                        c.fill(sx,sy,sx+16,sy+16,overlay);
                    } else if(!stack.isEmpty())matches++;
                }
                edit.setTextColor(matches==0?0xffff5555:0xffffffff);
            } else {
                edit.setTextColor(0xff404040);
            }
        }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e,boolean twice) {
            SENDERS.put(c.model(),c);
            if(c.state().search!=null) {
                var edit=c.state().search;boolean inside=search(c,e.x(),e.y());edit.setFocused(inside);
                if(inside) { if(e.button()==1)edit.setValue("");else edit.mouseClicked(e.x(),e.y(),e.button());return true; }
            }return false;
        }
        @Override public boolean mouseDragged(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e,double dx,double dy) { SENDERS.put(c.model(),c);return false; }
        @Override public boolean mouseScrolled(DesktopInputContext<AbstractContainerMenu,State> c,double x,double y,double dx,double dy) { SENDERS.put(c.model(),c);return false; }
        @Override public boolean keyPressed(DesktopInputContext<AbstractContainerMenu,State> c,KeyEvent e) {
            var edit=c.state().search;if(edit==null||!edit.isFocused())return false;
            if(e.isEscape()) { edit.setFocused(false);return true; }edit.keyPressed(e.key(),e.scancode(),e.modifiers());return true;
        }
        @Override public boolean charTyped(DesktopInputContext<AbstractContainerMenu,State> c,CharacterEvent e) { var edit=c.state().search;return edit!=null&&edit.isFocused()&&edit.charTyped(e.codepoint(),e.modifiers()); }
        @Override public boolean wantsTextInput(DesktopWindowContext<AbstractContainerMenu,State> c) { return c.state().search!=null&&c.state().search.isFocused(); }
        @Override public List<DesktopWindowBounds> textInputBounds(DesktopWindowContext<AbstractContainerMenu,State> c) { return c.state().search==null?List.of():List.of(box(c.windowX()+7,c.windowY()+24,133,14)); }
    }
}
