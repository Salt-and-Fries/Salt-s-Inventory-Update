package com.salts_inventory_update.compat.tradingpost;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.api.server.desktop.DesktopServerWindowHandler;
import com.salts_inventory_update.api.server.desktop.DesktopServerSessionContext;
import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.inventory.InventoryMenu;

/** Native Trading Post search/aggregation and transactions in the compact merchant layout. */
public final class TradingPostWindows {
    private static final Map<AbstractContainerMenu,WeakReference<AbstractContainerScreen<?>>> SCREENS=new WeakHashMap<>();
    private static final Set<Object> HOSTED=Collections.newSetFromMap(new WeakHashMap<>());
    private static boolean bound,installed;
    private static final int ROWS=4,LIST_Y=30,LIST_HEIGHT=80,SCROLL_X=92,THUMB_HEIGHT=27;
    private static final ResourceLocation VILLAGER=ResourceLocation.fromNamespaceAndPath("minecraft","textures/gui/container/villager2.png");
    private static final ResourceLocation SEARCH_ICON=ResourceLocation.fromNamespaceAndPath("tradingpost","item/magnifying_glass");
    private TradingPostWindows() { }
    public static void remember(AbstractContainerScreen<?> screen){SCREENS.put(screen.getMenu(),new WeakReference<>(screen));}
    public static boolean hosted(Object screen){return HOSTED.contains(screen);}
    private static AbstractContainerScreen<?> screen(AbstractContainerMenu menu){var ref=SCREENS.get(menu);return ref==null?null:ref.get();}
    private static EditBox searchBox(AbstractContainerMenu menu){
        return screen(menu).children().stream().filter(EditBox.class::isInstance).map(EditBox.class::cast).findFirst().orElseThrow();
    }
    public static boolean textFocused(Object screen){
        return screen instanceof AbstractContainerScreen<?> s&&s.children().stream().anyMatch(w->w instanceof EditBox b&&b.isFocused());
    }
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}
    public static void initializeClient(){install();DesktopClientApi.onClientStarted(mc->{bind();install();});}
    private static synchronized void bind(){
        var type=NativeMenuSupport.type("tradingpost:trading_post");if(type==null||bound)return;
        NativeMenuSupport.bindNative(type,false);
        DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,List<Object>>() {
            @Override public List<Object> createState(DesktopServerSessionContext<AbstractContainerMenu,List<Object>> c){return new ArrayList<>(((MerchantMenu)c.menu()).getOffers());}
            @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu menu){return true;}
            @Override public void tick(DesktopServerSessionContext<AbstractContainerMenu,List<Object>> c){
                var offers=new ArrayList<Object>(((MerchantMenu)c.menu()).getOffers());
                if(!offers.equals(c.state())){
                    // An upstream rebuild resets native selection/search. Return
                    // payment in the owning server menu at that same transition.
                    ForeignMenuAccess.invoke(c.menu(),"clearPaymentSlots");
                    ((MerchantMenu)c.menu()).setSelectionHint(-1);c.state().clear();c.state().addAll(offers);c.broadcastChanges();
                }
            }
        });
        // Ask the installed upstream transport for its own empty clear-payment
        // message. Accept only those exact bytes, in this authenticated menu.
        // This works with Fabric's separate channel and Forge's discriminator.
        try {
            var messageClass=Class.forName("fuzs.tradingpost.network.client.C2SClearSlotsMessage");
            Object network=Class.forName("fuzs.tradingpost.TradingPost").getField("NETWORK").get(null);
            Object message=messageClass.getConstructor().newInstance();
            Object outbound=messageClass.getMethod("toServerboundMessage").invoke(message);
            Object packet=ForeignMenuAccess.invoke(network,"toServerboundPacket",new Class<?>[]{Class.forName("fuzs.puzzleslib.api.network.v3.ServerboundMessage")},outbound);
            if(!(packet instanceof ServerboundCustomPayloadPacket payload))throw new IllegalStateException("Unsupported Trading Post transport");
            byte[] expected=com.salts_inventory_update.compat.nativewindows.NativePayloads.bytes(payload.payload(),net.minecraft.core.RegistryAccess.EMPTY);
            DesktopServerApi.registerNativePayload(type,payload.payload().type().id(),c->{
                if(!Arrays.equals(expected,c.data())){c.reject("Invalid Trading Post clear-payment message");return;}
                ForeignMenuAccess.invoke(c.menu(),"clearPaymentSlots");c.broadcastChanges();
            });
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Trading Post clear-payment transport",failure);}
        bound=true;
    }
    private static synchronized void install(){
        var type=NativeMenuSupport.type("tradingpost:trading_post");if(type==null||installed)return;
        DesktopClientApi.registerNativeMatcher(ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","trading-post"),100,Set.of(type),lookup->true,lookup->new Template());
        installed=true;
    }
    private static int value(Object screen,String accessor){return ((Number)ForeignMenuAccess.invoke(screen,"tradingpost$"+accessor)).intValue();}
    private static int scroll(AbstractContainerMenu menu){return value(screen(menu),"getScrollOff");}
    private static int maxScroll(AbstractContainerMenu menu){return Math.max(0,((MerchantMenu)menu).getOffers().size()-ROWS);}
    private static void scroll(AbstractContainerMenu menu,int target){ForeignMenuAccess.invoke(screen(menu),"tradingpost$setScrollOff",new Class<?>[]{int.class},Math.max(0,Math.min(maxScroll(menu),target)));}
    static final class State { boolean dragging,searchDragging;int grab; }
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,State>,DesktopWindowPresentation {
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c){return new State();}
        @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu,State> c){var s=screen(c.model());if(s!=null){HOSTED.add(s);TradingPostPackets.open(s);scroll(c.model(),scroll(c.model()));}}
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c){
            return DesktopNativeLayout.builder()
                // Retain only the editable viewport for native input mapping.
                // Its pixels are covered and the widget is drawn on clean chrome.
                .keep("search-input",new DesktopWindowBounds(13,6,80,9),19,14)
                .keep("four-native-offers",new DesktopWindowBounds(5,18,88,80),0,LIST_Y)
                .keep("merchant-xp",new DesktopWindowBounds(136,16,102,5),128,40)
                .keep("native-transaction",new DesktopWindowBounds(131,32,113,27),123,60)
                .canvas(242,114).scrollable(false).build();
        }
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c){
            return new DesktopWindowSurface(DesktopSurfaceMode.SALT,new DesktopInsets(6,16,6,6),true,List.of(),DesktopViewportPolicy.CLAMP);
        }
        @Override public DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu,State> c,double x,double y){
            return c.contains(x,y,c.contentX()+207,c.contentY()+60,26,26)?DesktopSlotHit.of(2,c.contentX()+212,c.contentY()+65):null;
        }
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> c){
            int x=c.contentX(),y=c.contentY(),top=scroll(c.model()),max=maxScroll(c.model());
            String caption="Offers";c.text(caption,x+(88-c.fontWidth(caption))/2,y,0xFF404040,false);
            renderSearch(c);
            int populated=Math.min(ROWS,((MerchantMenu)c.model()).getOffers().size());
            // The native tall sidebar has a darker empty backing. Omit that
            // backing below the compact rows, like the approved vanilla window.
            if(populated<ROWS)c.fill(x,y+LIST_Y+populated*20,x+88,y+LIST_Y+LIST_HEIGHT,0xFFC6C6C6);
            var menu=(MerchantMenu)c.model();
            var heading=(Component)ForeignMenuAccess.invoke(ForeignMenuAccess.invoke(menu,"getTraders"),"getDisplayName");
            if(heading==null)heading=c.originalTitle();
            String label=heading.getString();
            if(menu.getTraderLevel()>0)label+=" - "+Component.translatable("merchant.level."+menu.getTraderLevel()).getString();
            if(c.fontWidth(label)>106)label=c.trimToWidth(label,106-c.fontWidth("..."))+"...";
            c.text(label,x+128+(106-c.fontWidth(label))/2-3,y+29,0xFF404040,false);
            int selected=value(screen(c.model()),"getShopItem")-top;
            if(selected>=0&&selected<ROWS&&selected+top<((MerchantMenu)c.model()).getOffers().size()){
                int ry=y+LIST_Y+selected*20;
                c.fill(x,ry,x+88,ry+1,0xFFFFFFFF);c.fill(x,ry+19,x+88,ry+20,0xFFFFFFFF);
                c.fill(x,ry,x+1,ry+20,0xFFFFFFFF);c.fill(x+87,ry,x+88,ry+20,0xFFFFFFFF);
            }
            c.fill(x+SCROLL_X+1,y+LIST_Y,x+SCROLL_X+5,y+LIST_Y+79,0xFFAAAAAA);
            int thumb=y+LIST_Y+(max==0?0:(79-THUMB_HEIGHT)*top/max);
            c.sprite(ResourceLocation.fromNamespaceAndPath("minecraft",max==0?"container/villager/scroller_disabled":"container/villager/scroller"),x+SCROLL_X,thumb,6,27);
        }
        private void renderSearch(DesktopRenderContext<AbstractContainerMenu,State> c){
            int x=c.contentX(),y=c.contentY();
            // A complete inset, not a crop of Trading Post's old sidebar. The
            // icon comes straight from its transparent atlas sprite.
            c.fill(x+18,y+10,x+101,y+26,0xFF373737);
            c.fill(x+19,y+11,x+100,y+25,0xFF8B8B8B);
            c.fill(x+18,y+25,x+101,y+26,0xFFFFFFFF);
            c.fill(x+100,y+10,x+101,y+26,0xFFFFFFFF);
            c.sprite(ResourceLocation.fromNamespaceAndPath("tradingpost","container/villager/magnifying_glass"),x,y+10,16,16);
            var box=searchBox(c.model());var graphics=c.nativeGraphics();
            // Draw the live native widget directly, so caret/selection cannot
            // pick up the previous frame's border or neighboring native artwork.
            c.withClip(x+19,y+12,80,12,()->{
                graphics.pose().pushPose();
                try{
                    graphics.pose().translate(x+19-box.getX(),y+14-box.getY(),0);
                    box.render(graphics,c.mouseX()-x-19+box.getX(),c.mouseY()-y-14+box.getY(),0);
                }finally{graphics.pose().popPose();}
            });
        }
        private boolean search(DesktopWindowContext<?,?> c,double x,double y){return x>=c.contentX()+18&&x<c.contentX()+101&&y>=c.contentY()+10&&y<c.contentY()+26;}
        @Override public List<DesktopWindowBounds> textInputBounds(DesktopWindowContext<AbstractContainerMenu,State> c){return List.of(new DesktopWindowBounds(c.contentX()+18,c.contentY()+10,83,16));}
        private boolean list(DesktopWindowContext<?,?> c,double x,double y){return x>=c.contentX()&&x<c.contentX()+100&&y>=c.contentY()+LIST_Y&&y<c.contentY()+LIST_Y+LIST_HEIGHT;}
        @Override public boolean mouseScrolled(DesktopInputContext<AbstractContainerMenu,State> c,double x,double y,double dx,double dy){
            if(!list(c,x,y))return false;scroll(c.model(),scroll(c.model())-(int)Math.signum(dy));return true;
        }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e,boolean twice){
            if(e.button()==0&&search(c,e.x(),e.y())){
                var box=searchBox(c.model());screen(c.model()).setFocused(box);box.setFocused(true);
                box.mouseClicked(box.getX()+Math.max(0,Math.min(79,e.x()-c.contentX()-19)),box.getY()+4,0);
                c.state().searchDragging=true;return true;
            }
            if(e.button()!=0||!list(c,e.x(),e.y()))return false;
            if(e.x()<c.contentX()+88){
                int index=scroll(c.model())+((int)e.y()-c.contentY()-LIST_Y)/20;
                var menu=(MerchantMenu)c.model();var offers=menu.getOffers();if(index>=offers.size())return true;
                var offer=offers.get(index);Object traders=ForeignMenuAccess.invoke(menu,"getTraders");
                if(!Boolean.TRUE.equals(ForeignMenuAccess.invoke(traders,"checkOffer",new Class<?>[]{net.minecraft.world.item.trading.MerchantOffer.class},offer)))return true;
                int original=((Number)ForeignMenuAccess.invoke(offers,"getOrigShopItem",new Class<?>[]{int.class},index)).intValue();
                if(c.sendMenuButton(original)){
                    // The same native selection/prediction calls as the upstream
                    // button, with its unfiltered index sent to Salt's session.
                    ForeignMenuAccess.invoke(screen(menu),"tradingpost$setShopItem",new Class<?>[]{int.class},index);
                    menu.setSelectionHint(index);ForeignMenuAccess.invoke(traders,"setActiveOffer",new Class<?>[]{net.minecraft.world.item.trading.MerchantOffer.class},offer);menu.tryMoveItems(index);
                }
                return true;
            }
            if(e.x()<c.contentX()+SCROLL_X)return false;
            int thumb=(maxScroll(c.model())==0?0:(79-THUMB_HEIGHT)*scroll(c.model())/maxScroll(c.model()));
            int at=(int)e.y()-c.contentY()-LIST_Y;
            c.state().grab=at>=thumb&&at<thumb+THUMB_HEIGHT?at-thumb:THUMB_HEIGHT/2;c.state().dragging=true;
            drag(c,e.y());return true;
        }
        private void drag(DesktopInputContext<AbstractContainerMenu,State> c,double y){scroll(c.model(),(int)Math.round((y-c.contentY()-LIST_Y-c.state().grab)*maxScroll(c.model())/(79-THUMB_HEIGHT)));}
        @Override public boolean mouseDragged(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e,double dx,double dy){
            if(c.state().searchDragging){var box=searchBox(c.model());box.mouseDragged(box.getX()+e.x()-c.contentX()-19,box.getY()+4,e.button(),dx,dy);return true;}
            if(!c.state().dragging)return false;drag(c,e.y());return true;
        }
        @Override public boolean mouseReleased(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e){boolean drag=c.state().dragging||c.state().searchDragging;c.state().dragging=false;c.state().searchDragging=false;return drag;}
        @Override public void cancelPointer(DesktopWindowContext<?,?> c){if(c.state() instanceof State s){s.dragging=false;s.searchDragging=false;}}
        @Override public List<DesktopScrollRegion> controllerScrollRegions(DesktopWindowContext<AbstractContainerMenu,State> c){return List.of(new DesktopScrollRegion(c.contentX(),c.contentY()+LIST_Y,100,80,20,scroll(c.model())>0,scroll(c.model())<maxScroll(c.model())));}
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){HOSTED.remove(screen(c.model()));SCREENS.remove(c.model());TradingPostPackets.close(c.model());}
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return "trading-post-compact-merchant-1";}
    }
}
