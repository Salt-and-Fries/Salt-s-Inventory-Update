package com.salts_inventory_update.compat.travelersbackpack;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.client.input.*;
import com.salts_inventory_update.api.desktop.*;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import io.netty.buffer.Unpooled;
import java.lang.ref.WeakReference;
import java.util.*;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;
import static com.salts_inventory_update.compat.travelersbackpack.TravelersAccess.*;

/** Sophisticated-style storage window retaining Traveler's original upgrade/tool panels. */
public final class TravelersWindows {
    private static final String ID="SaltsInventoryWindowId";
    private static final Map<AbstractContainerMenu,WeakReference<AbstractContainerScreen<?>>> SCREENS=new WeakHashMap<>();
    private static final Set<Object> DIRTY=Collections.newSetFromMap(new WeakHashMap<>());
    private static final Set<String> BOUND=new HashSet<>(),INSTALLED=new HashSet<>();
    private TravelersWindows() { }
    public static void remember(AbstractContainerScreen<?> s){SCREENS.put(s.getMenu(),new WeakReference<>(s));}
    static AbstractContainerScreen<?> screen(AbstractContainerMenu m){var r=SCREENS.get(m);return r==null?null:r.get();}
    public static void changed(Object w){Object level=call(w,"getLevel");if(level instanceof net.minecraft.world.level.Level l&&l.isClientSide)DIRTY.add(w);}
    public static void updateDetachedMenus(Object w,int slot){
        for(Object user:(List<?>)call(w,"getPlayersUsing"))if(user instanceof net.minecraft.server.level.ServerPlayer player)
            DesktopSessions.hasOpenSessionMatching(player,m->{
                if(m!=player.containerMenu&&!settings(m)&&m.getClass().getName().startsWith("com.tiviacz.travelersbackpack.")&&wrapper(m)==w){
                    if(slot<0)call(m,"rebuildModifiableSlots");else call(m,"updateModifiableSlotsPosition",slot);
                }
                return false;
            });
    }
    public static void identity(AbstractContainerMenu m){
        if(!TravelersPackets.supported())return;
        Player player=(Player)field(m,"player");if(player.level().isClientSide)return;Object w=wrapper(m);
        if(number(w,"getScreenID")==1&&!stack(m).getOrCreateTag().hasUUID(ID))stack(m).getOrCreateTag().putUUID(ID,UUID.randomUUID());
    }
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}
    public static void initializeClient(){install();DesktopClientApi.onClientStarted(mc->{bind();install();});}
    private static synchronized void bind(){
        if(!TravelersPackets.supported())return;
        for(String name:List.of("backpack_item","backpack_block","backpack_settings")){
            var type=NativeMenuSupport.type("travelersbackpack:"+name);if(type==null||!BOUND.add(name))continue;
            DesktopMenuBindings.register(type,new DesktopMenuBindings.SlotAccess(){
                @Override public int size(AbstractContainerMenu m){return m.slots.size();}
                @Override public boolean isVirtualSlot(AbstractContainerMenu m,Slot s){
                    // The disabled carrier cell is a read-only alias of the
                    // locked source item. Its NBT changes as storage is saved;
                    // it is never an independent sortable ingredient.
                    return settings(m)||Set.of("FilterSlotItemHandler","DisabledSlot").contains(s.getClass().getSimpleName());
                }
            },true,(player,m,openingData)->{
                Object w=wrapper(m);int kind=number(w,"getScreenID");
                if(kind==3&&call(w,"getBackpackPos") instanceof BlockPos pos)return DesktopSessions.blockSourceKey(player,pos);
                if(kind==1&&stack(m).getOrCreateTag().hasUUID(ID))return "travelersbackpack:item:"+stack(m).getTag().getUUID(ID);
                Object owner=call(w,"getBackpackOwner");return owner instanceof Player p?"travelersbackpack:worn:"+p.getUUID():null;
            },null,new DesktopMenuBindings.GesturePolicy(){@Override public boolean useNativeQuickMove(AbstractContainerMenu m){return true;}});
            DesktopMenuBindings.registerStorage(type,(m,s)->!settings(m)&&m.slots.indexOf(s)<integer(m,"BACKPACK_INV_END")&&!((List<?>)call(wrapper(m),"getUnsortableSlots")).contains(m.slots.indexOf(s)));
            DesktopItemSources.register(type,(player,m)->{
                Object w=wrapper(m);if(number(w,"getScreenID")!=1||!stack(m).getOrCreateTag().hasUUID(ID))return null;
                int index=number(w,"getBackpackSlotIndex");if(index<0)index=player.getInventory().selected;
                UUID id=stack(m).getTag().getUUID(ID);return new DesktopItemSources.Source(index,id.toString(),stack(m),s->s.hasTag()&&s.getTag().hasUUID(ID)&&s.getTag().getUUID(ID).equals(id));
            });
            DesktopServerApi.registerOpeningData(type,(player,m,buf)->{
                Object w=wrapper(m);int kind=number(w,"getScreenID");
                if(settings(m)){
                    buf.writeBoolean(kind==3);
                    if(kind==3)buf.writeBlockPos((BlockPos)call(w,"getBackpackPos"));
                    else {buf.writeInt(kind);buf.writeBlockPos(BlockPos.ZERO);buf.writeInt(number(w,"getBackpackSlotIndex"));}
                }else if(kind==3){buf.writeInt(-1);buf.writeBlockPos((BlockPos)call(w,"getBackpackPos"));}
                else {buf.writeInt(kind);Object owner=call(w,"getBackpackOwner");buf.writeInt(kind==2&&owner instanceof Player p?p.getId():number(w,"getBackpackSlotIndex"));}
            });
            DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,TravelersPackets.Sync>(){
                @Override public TravelersPackets.Sync createState(DesktopServerSessionContext<AbstractContainerMenu,TravelersPackets.Sync> c){return new TravelersPackets.Sync();}


                @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu m){return !settings(m);}
                @Override public void tick(DesktopServerSessionContext<AbstractContainerMenu,TravelersPackets.Sync> c){TravelersPackets.sync(c);}
            });TravelersPackets.register(type);
        }
    }
    private static synchronized void install(){
        if(!TravelersPackets.supported())return;
        for(String name:List.of("backpack_item","backpack_block","backpack_settings")){
            var type=NativeMenuSupport.type("travelersbackpack:"+name);if(type==null||!INSTALLED.add(name))continue;
            DesktopMenuFactories.register(type,(mc,player,id,data)->{
                var buf=new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
                String cls=switch(name){case "backpack_block"->"BackpackBlockEntityMenu";case "backpack_settings"->"BackpackSettingsMenu";default->"BackpackItemMenu";};
                try{return (AbstractContainerMenu)Class.forName("com.tiviacz.travelersbackpack.inventory.menu."+cls).getConstructor(int.class,net.minecraft.world.entity.player.Inventory.class,FriendlyByteBuf.class).newInstance(id,player.getInventory(),buf);}
                catch(ReflectiveOperationException failure){throw new IllegalStateException("Traveler's Backpack native menu reconstruction",failure);}
                finally{buf.release();}
            });
            DesktopClientApi.registerNativeMatcher(new ResourceLocation("salts_inventory_compat","travelers/"+name),100,Set.of(type),lookup->true,lookup->new Template());
        }
    }
    static final class State {DesktopNativeLayout layout;GuiEventListener drag;String dragRegion;boolean selecting;CompoundTag result;}
    private static final class Template implements DesktopNativeTemplate<AbstractContainerMenu,State>,DesktopWindowPresentation {
        @Override public net.minecraft.network.chat.Component title(DesktopWindowContext<AbstractContainerMenu,State> c){return net.minecraft.network.chat.Component.translatable(settings(c.model())?"screen.travelersbackpack.settings":"itemGroup.travelersbackpack");}
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c){return new State();}
        @Override public void tick(DesktopWindowContext<AbstractContainerMenu,State> c){
            Object w=wrapper(c.model());var s=screen(c.model());
            if(s!=null&&DIRTY.remove(w)&&!settings(c.model())){call(c.model(),"rebuildModifiableSlots");call(s,"updateScreen",false);}
            applyResult(c);
        }
        private void applyResult(DesktopWindowContext<AbstractContainerMenu,State> c){
            if(c.state().result!=null){
                CompoundTag tag=c.state().result;int id=tag.getInt("SiuResultSlot");
                if(id>0&&id<c.model().slots.size()){
                    var cell=c.model().getSlot(id);
                    if(cell.container instanceof ResultContainer container){
                        // Upstream ResultSlotExt deliberately ignores Slot.set.
                        // Its native recipe packet updates this container directly.
                        container.setItem(0,ItemStack.of(tag.getCompound("SiuResult")));
                        var mc=net.minecraft.client.Minecraft.getInstance();
                        container.setRecipeUsed(tag.contains("SiuRecipe")?mc.level.getRecipeManager().byKey(new ResourceLocation(tag.getString("SiuRecipe"))).orElse(null):null);
                    }
                }
            }
        }
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c){
            var s=screen(c.model());int rows=integer(s,"visibleRows"),columns=number(s,"getSlotsInRow"),height=rows*18;
            boolean tanks=!settings(c.model())&&(Boolean)field(s,"tanksVisible");int sx=tanks?29:7;
            var b=DesktopNativeLayout.builder().keep("storage",new DesktopWindowBounds(sx,17,columns*18,height),tanks?22:0,14);
            int width=columns*18+(tanks?44:0);
            if(tanks){b.keep("left-tank",new DesktopWindowBounds(7,17,18,height),0,14);b.keep("right-tank",new DesktopWindowBounds(195+(columns==11?36:0),17,18,height),width-18,14);}
            if(!settings(c.model())){
                Object tool=field(s,"toolSlotsWidget");
                if(tool!=null){int[] r=(int[])call(tool,"getWidgetSizeAndPos");int left=number(s,"getGuiLeft"),top=number(s,"getGuiTop");b.keep("edit-tools",new DesktopWindowBounds(r[0]-left-1,r[1]-top-1,12,12),width-12,0);
                    if((Boolean)call(wrapper(c.model()),"showToolSlots")){int[] a=(int[])call(tool,"getAdditionSizeAndPos");b.exterior("tool-drawer",new DesktopWindowBounds(a[0]-left,a[1]-top,a[2],a[3]),-20,24);}
                }
                @SuppressWarnings("unchecked") var buttons=(List<Object>)field(s,"buttons");int x=0;
                for(int i=0;i<buttons.size();i++){
                    Object button=buttons.get(i);if(!(Boolean)call(wrapper(c.model()),"showMoreButtons")&&!Set.of("MoreButton","EquipButton").contains(button.getClass().getSimpleName()))continue;
                    b.keep("action/"+i,new DesktopWindowBounds(integer(button,"x"),integer(button,"y"),integer(button,"width"),integer(button,"height")),x,0);x+=14;
                }
            }
            int left=number(s,"getGuiLeft"),top=number(s,"getGuiTop"),i=0;
            for(var child:s.children()){
                String cls=child.getClass().getSimpleName();if(Set.of("SortingButtons","ToolSlotsWidget","InventoryScroll").contains(cls))continue;
                if(!child.getClass().getName().startsWith("com.tiviacz.travelersbackpack."))continue;
                int[] r=(int[])call(child,"getWidgetSizeAndPos");int x=r[0]-left,y=r[1]-top;
                if(x>=sx+columns*18){
                    var source=new DesktopWindowBounds(x,y,r[2],r[3]);b.exterior("widget/"+i++,source,width+12,y-4+24);
                }
            }
            if(!settings(c.model())){
                int railX=tanks?217+(columns==11?36:0):173+(columns==11?36:0);
                // Native upgrade plates include the full four-pixel slot surround.
                for(int slot=integer(c.model(),"UPGRADE_START");slot<integer(c.model(),"UPGRADE_END");slot++){
                    var cell=c.nativeView().slots().get(slot);var r=cell.bounds();if(!cell.active()||r.y()<0)continue;
                    var plate=new DesktopWindowBounds(r.x()-3,r.y()-3,24,24);
                    boolean covered=b.build().regions().stream().anyMatch(region->region.covers(plate));
                    if(!covered)b.exterior("upgrade-plate/"+slot,plate,width+12,plate.y()-4+24);
                }
            }
            boolean scrolling=(Boolean)field(s,"isScrollable");
            if(scrolling)b.keep("native-scroll",new DesktopWindowBounds(sx+columns*18,17,4,height),width,14);
            b.canvas(width+(scrolling?4:0),14+height).scrollable(false);
            c.state().layout=b.build();return c.state().layout;
        }
        private double[] nativePoint(DesktopInputContext<AbstractContainerMenu,State> c,double x,double y){
            var p=((DesktopNativeContext<AbstractContainerMenu,State>)c).nativeSourceAt(x,y);if(p.isEmpty())return null;
            var s=screen(c.model());return new double[]{number(s,"getGuiLeft")+p.get().x(),number(s,"getGuiTop")+p.get().y()};
        }
        private Slot slot(DesktopInputContext<AbstractContainerMenu,State> c,double[] p){
            var s=screen(c.model());double x=p[0]-number(s,"getGuiLeft"),y=p[1]-number(s,"getGuiTop");
            return c.model().slots.stream().filter(a->a.isActive()&&x>=a.x-1&&x<a.x+17&&y>=a.y-1&&y<a.y+17).findFirst().orElse(null);
        }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e,boolean twice){
            double[] p=nativePoint(c,e.x(),e.y());if(p==null)return false;var s=screen(c.model());
            if(settings(c.model())){Slot slot=slot(c,p);if(slot!=null&&(Boolean)call(s,"selectSlots",slot,e.button())){c.state().selecting=true;TravelersPackets.input(c,()->{call(s,"sendDataToServer");return true;});return true;}}
            for(var child:s.children()){
                if(child.getClass().getSimpleName().equals("SortingButtons"))continue;
                // Tank hit areas intentionally extend beyond the tab's plate.
                boolean handled=TravelersPackets.input(c,()->child.mouseClicked(p[0],p[1],e.button()));
                if(handled){s.setFocused(child);c.state().drag=child;c.state().dragRegion=((DesktopNativeContext<?,?>)c).nativeSourceAt(e.x(),e.y()).orElseThrow().regionId();if(settings(c.model()))TravelersPackets.input(c,()->{call(s,"sendDataToServer");return true;});return true;}
            }
            if(!settings(c.model()))for(Object button:(List<?>)field(s,"buttons")){
                if(!(Boolean)call(wrapper(c.model()),"showMoreButtons")&&!Set.of("MoreButton","EquipButton").contains(button.getClass().getSimpleName()))continue;
                if(TravelersPackets.input(c,()->(Boolean)call(button,"mouseClicked",p[0],p[1],e.button())))return true;
            }
            return false;
        }
        @Override public boolean mouseDragged(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e,double dx,double dy){
            if(c.state().selecting){var p=nativePoint(c,e.x(),e.y());if(p!=null){Slot slot=slot(c,p);if(slot!=null&&(Boolean)call(screen(c.model()),"selectSlots",slot,e.button()))TravelersPackets.input(c,()->{call(screen(c.model()),"sendDataToServer");return true;});}return true;}
            if(c.state().drag==null)return false;var r=c.state().layout.regions().stream().filter(a->a.id().equals(c.state().dragRegion)).findFirst().orElse(null);if(r==null)return true;
            int x=r.exterior()?c.windowX():c.contentX(),y=r.exterior()?c.windowY():c.contentY();var s=screen(c.model());
            return TravelersPackets.input(c,()->c.state().drag.mouseDragged(e.x()-x-r.x()+r.source().x()+number(s,"getGuiLeft"),e.y()-y-r.y()+r.source().y()+number(s,"getGuiTop"),e.button(),dx,dy));
        }
        @Override public boolean mouseReleased(DesktopInputContext<AbstractContainerMenu,State> c,MouseButtonEvent e){
            boolean used=c.state().selecting||c.state().drag!=null;
            if(c.state().drag!=null){var p=nativePoint(c,e.x(),e.y());if(p!=null)TravelersPackets.input(c,()->c.state().drag.mouseReleased(p[0],p[1],e.button()));}
            c.state().selecting=false;c.state().drag=null;return used;
        }
        @Override public void cancelPointer(DesktopWindowContext<?,?> c){if(c.state() instanceof State s){s.drag=null;s.selecting=false;}}
        @Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,State> c,ResourceLocation channel,byte[] data){
            if(!channel.equals(TravelersPackets.PRESENTATION))return;var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            try{CompoundTag tag=b.readNbt();if(tag==null)return;Object w=wrapper(c.model());ItemStack old=stack(c.model()),copy=old.copy();boolean upgrades=!Objects.equals(old.getOrCreateTag().get("Upgrades"),tag.get("Upgrades"));
                c.state().result=tag.contains("SiuResult")?tag.copy():null;
                for(String key:tag.getAllKeys())if(!key.startsWith("Siu"))copy.getOrCreateTag().put(key,tag.get(key).copy());call(w,"setBackpackStack",copy);if(upgrades){call(w,"loadUpgrades");DIRTY.add(w);}
                applyResult(c);
            }finally{b.release();}
        }
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){SCREENS.remove(c.model());}
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return "travelers-native-tools-upgrades-1";}
    }
}
