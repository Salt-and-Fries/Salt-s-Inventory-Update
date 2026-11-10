package com.salts_inventory_update.compat.bumblezone;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** Bumblezone's live artwork and actions, presented in the reviewed SIU windows. */
public final class BumblezoneWindows {
    enum Kind { FLOWER, COCOON, BRIEFCASE }
    private static final Map<String,Kind> MENUS=new LinkedHashMap<>();
    private static final Set<String> BOUND=new HashSet<>(),INSTALLED=new HashSet<>();
    static final ResourceLocation CASE=new ResourceLocation("the_bumblezone","textures/gui/buzzing_briefcase/background.png");
    static {
        MENUS.put("crystalline_flower",Kind.FLOWER);
        MENUS.put("buzzing_briefcase",Kind.BRIEFCASE);
        for(int rows=1;rows<=6;rows++)MENUS.put("strict_9x"+rows,Kind.COCOON);
    }
    private BumblezoneWindows() { }
    // The publisher relocated this asset in newer releases. Read the native
    // screen's own identifier so the title patch also follows resource packs.
    private static ResourceLocation caseBackground;
    private static ResourceLocation caseTexture() {
        if(caseBackground!=null)return caseBackground;
        try {
            var screen=Class.forName("com.telepathicgrunt.the_bumblezone.client.screens.BuzzingBriefcaseScreen");
            var background=screen.getDeclaredField("BACKGROUND");background.setAccessible(true);
            return caseBackground=(ResourceLocation)background.get(null);
        }catch(ReflectiveOperationException|LinkageError unavailable){return caseBackground=CASE;}
    }

    public static void initializeCommon() {
        bind();DesktopEnvironment.onServerStarting(server->bind());
    }
    public static void initializeClient() {
        bind();install();DesktopClientApi.onClientStarted(client->{bind();install();});
    }
    private static synchronized void bind() {
        for(var e:MENUS.entrySet()) {
            String id="the_bumblezone:"+e.getKey();var type=NativeMenuSupport.type(id);
            if(type==null||!BOUND.add(id))continue;
            if(e.getValue()==Kind.BRIEFCASE) {
                // This is an off-screen synchronization cell for the held case,
                // not an item-storage slot or a slot the player may move/sort.
                NativeMenuSupport.bindNative(type,false,new DesktopMenuBindings.SlotAccess(){
                    @Override public int size(AbstractContainerMenu menu){return menu.slots.size();}
                    @Override public boolean isVirtualSlot(AbstractContainerMenu menu,Slot slot){return true;}
                },BumblezoneWindows::prepareBriefcaseSnapshot);
                if(DesktopServerApi.findWindowHandler(type)==null)
                    DesktopServerApi.registerWindow(type,new com.salts_inventory_update.api.server.desktop.DesktopServerWindowHandler<AbstractContainerMenu,Void>(){
                        @Override public boolean supports(net.minecraft.server.level.ServerPlayer player,AbstractContainerMenu menu){
                            return menu.getClass().getName().equals("com.telepathicgrunt.the_bumblezone.menus.BuzzingBriefcaseMenu");
                        }
                    });
            }else NativeMenuSupport.bindNative(type,false);
            DesktopMenuBindings.registerStorage(type,(menu,slot)->e.getValue()==Kind.COCOON&&!(slot.container instanceof Inventory));
            if(e.getValue()==Kind.FLOWER) {
                // Exactly one upstream action; retain the original menu's
                // enchantment validation inside the authenticated session.
                // Fabric's upstream NetworkChannel namespaces each packet
                // beneath networking/<packet namespace>/<packet path>.
                var channel=new ResourceLocation("the_bumblezone","networking/the_bumblezone/crystalline_flower_clicked_enchantment_button_packet");
                DesktopServerApi.registerNativePayload(type,channel,c->{
                    var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
                    try {
                        int container=b.readInt();var enchantment=b.readResourceLocation();
                        if(container!=c.menu().containerId||b.isReadable()){c.reject("Invalid flower selection");return;}
                        c.menu().getClass().getMethod("clickMenuEnchantment",Player.class,ResourceLocation.class).invoke(c.menu(),c.player(),enchantment);
                        c.broadcastChanges();
                    }catch(ReflectiveOperationException|RuntimeException failure){c.reject("Invalid flower selection");}
                    finally{b.release();}
                });
            }
        }
    }
    private static void prepareBriefcaseSnapshot(AbstractContainerMenu menu,List<net.minecraft.world.item.ItemStack> items) {
        if(menu.slots.size()!=1)return;
        var original=menu.getSlot(0);
        if(original.x!=Integer.MIN_VALUE||original.y!=Integer.MIN_VALUE)return;
        // Upstream uses MIN_VALUE for a hidden synchronization cell. Subtracting
        // a 1px frame from that sentinel overflows geometry. Adapt this client
        // snapshot only; preserve the same container, stack, slot index and
        // the menu's original briefcaseSlot reference/behavior.
        var hidden=new Slot(original.container,original.getContainerSlot(),-9999,-9999) {
            @Override public boolean isActive(){return false;}
            @Override public boolean mayPlace(net.minecraft.world.item.ItemStack stack){return false;}
            @Override public boolean mayPickup(Player player){return false;}
        };
        hidden.index=original.index;menu.slots.set(0,hidden);
    }
    private static synchronized void install() {
        for(var e:MENUS.entrySet()) {
            var id=new ResourceLocation("the_bumblezone",e.getKey());
            if(INSTALLED.contains(e.getKey())||!BuiltInRegistries.MENU.containsKey(id))continue;
            DesktopClientApi.registerNativeMatcher(new ResourceLocation("salts_inventory_compat","bumblezone/"+e.getKey()),100,
                Set.of(BuiltInRegistries.MENU.get(id)),lookup->true,lookup->new Template(e.getValue()));
            INSTALLED.add(e.getKey());
        }
    }
    static DesktopNativeLayout layout(Kind kind,DesktopNativeView view,int titleWidth) {
        if(kind==Kind.BRIEFCASE) {
            // The original renderer blits a 382x434 virtual atlas. Its opaque
            // case occupies the first 245 rows, above/beyond the declared menu.
            return DesktopNativeLayout.builder().keep("bee-case",new DesktopWindowBounds(-71,-45,382,245),0,0).build();
        }
        var b=DesktopNativeLayout.builder();
        if(kind==Kind.COCOON) {
            var cells=view.slots().stream().filter(s->!s.player()&&s.active()).map(DesktopNativeView.Slot::bounds).toList();
            var grid=DesktopNativeView.union(cells).orElse(view.automaticBounds());
            int width=Math.max(grid.width(),titleWidth+34);
            return b.keep("honey-cells",grid,(width-grid.width())/2,0).canvas(width,grid.height()).build();
        }
        int width=Math.max(170,titleWidth+34),dx=(width-170)/2;
        // The XP bar is native y=99..103 inclusive: keep its bottom border.
        return b.keep("consume",new DesktopWindowBounds(5,13,60,91),dx,0)
            .keep("enchant",new DesktopWindowBounds(67,13,106,97),dx+64,0)
            .canvas(width,102).build();
    }
    private record Template(Kind kind) implements DesktopNativeTemplate<AbstractContainerMenu,Void>,DesktopWindowPresentation {
        @Override public void opened(DesktopWindowContext<AbstractContainerMenu,Void> c){if(kind==Kind.FLOWER)BumblezoneFlowerPackets.open(c.model());}
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,Void> c){if(kind==Kind.FLOWER)BumblezoneFlowerPackets.close(c.model());}
        @Override public void renderBehindFrame(DesktopRenderContext<?,?> c){if(kind==Kind.FLOWER)BumblezoneFlowerPackets.restore((AbstractContainerMenu)c.model());}
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,Void> c,com.salts_inventory_update.api.client.input.MouseButtonEvent event,boolean twice){
            if(kind==Kind.FLOWER)BumblezoneFlowerPackets.restore(c.model());return false;
        }
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,Void> c) {
            return layout(kind,c.nativeView(),c.fontWidth(title(c)));
        }
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,Void> c) {
            return new DesktopWindowSurface(DesktopSurfaceMode.SALT,
                kind==Kind.BRIEFCASE?new DesktopInsets(0,23,0,0):new DesktopInsets(7,22,7,7),true,List.of(),DesktopViewportPolicy.CLAMP);
        }
        @Override public DesktopWindowDecoration<AbstractContainerMenu,Void> decoration(DesktopWindowContext<AbstractContainerMenu,Void> c) {
            return (draw,controls)->BumblezoneChrome.paint(draw,controls,title(draw),kind==Kind.BRIEFCASE);
        }
        @Override public Component title(DesktopWindowContext<AbstractContainerMenu,Void> c) {
            return switch(kind) {
                case FLOWER->Component.translatable("block.the_bumblezone.crystalline_flower");
                case BRIEFCASE->Component.translatable("item.the_bumblezone.buzzing_briefcase");
                case COCOON->c.originalTitle();
            };
        }
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,Void> c) {
            if(kind!=Kind.BRIEFCASE)return;
            // Replace only the old title glyphs with their exact wood pixels.
            // Its two honey latches and full shaped lid remain unobstructed.
            int w=c.fontWidth(c.originalTitle())+3;
            c.texture(caseTexture(),c.contentX()+145,c.contentY()+7,145,7,w,11,w,11,382,434);
        }
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,Void> c){return "bumblezone-native-1";}
    }
}
