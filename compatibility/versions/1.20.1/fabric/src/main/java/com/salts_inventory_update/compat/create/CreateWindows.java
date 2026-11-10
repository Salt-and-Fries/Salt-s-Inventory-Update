package com.salts_inventory_update.compat.create;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import java.util.*;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/** Approved Create-family compositions, with original menus and original actions. */
public final class CreateWindows {
    enum Kind { TOOLBOX,MOUNTED_TOOLBOX,SCHEMATICANNON,SCHEMATIC_TABLE,BLUEPRINT,FROGPORT,REQUESTER,CATEGORIES,STOCK,GAUGE,FILTER,ATTRIBUTE,PACKAGE_FILTER,CONTROLLER,SCHEDULE,AMMO,TIMED_FUZE,PROXIMITY_FUZE,INERTIA_FUZE,IMPACT_FUZE }
    private static final Map<String,Kind> MENUS=Map.ofEntries(
        Map.entry("create:toolbox",Kind.TOOLBOX),Map.entry("railways:mounted_toolbox",Kind.MOUNTED_TOOLBOX),
        Map.entry("create:schematicannon",Kind.SCHEMATICANNON),Map.entry("create:schematic_table",Kind.SCHEMATIC_TABLE),
        Map.entry("create:crafting_blueprint",Kind.BLUEPRINT),Map.entry("create:package_port",Kind.FROGPORT),
        Map.entry("create:redstone_requester",Kind.REQUESTER),Map.entry("create:stock_keeper_category",Kind.CATEGORIES),
        Map.entry("create:stock_keeper_request",Kind.STOCK),Map.entry("create:factory_panel_set_item",Kind.GAUGE),
        Map.entry("create:filter",Kind.FILTER),Map.entry("create:attribute_filter",Kind.ATTRIBUTE),
        Map.entry("create:package_filter",Kind.PACKAGE_FILTER),Map.entry("create:linked_controller",Kind.CONTROLLER),
        Map.entry("create:schedule",Kind.SCHEDULE),Map.entry("createbigcannons:autocannon_ammo_container",Kind.AMMO),
        Map.entry("createbigcannons:set_timed_fuze",Kind.TIMED_FUZE),Map.entry("createbigcannons:set_proximity_fuze",Kind.PROXIMITY_FUZE),
        Map.entry("createbigcannons:set_delayed_inertia_fuze",Kind.INERTIA_FUZE),Map.entry("createbigcannons:set_delayed_impact_fuze",Kind.IMPACT_FUZE));
    private static final Set<String> BOUND=new HashSet<>(),INSTALLED=new HashSet<>();
    private CreateWindows() { }
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}
    public static void initializeClient(){install();DesktopClientApi.onClientStarted(mc->{bind();install();});}
    private static boolean ghost(Kind k){return switch(k){case FILTER,ATTRIBUTE,BLUEPRINT,CONTROLLER,SCHEDULE,GAUGE,REQUESTER->true;default->false;};}
    @SuppressWarnings({"rawtypes","unchecked"})
    private static synchronized void bind(){
        for(var entry:MENUS.entrySet()){
            var id=new ResourceLocation(entry.getKey());if(BOUND.contains(entry.getKey())||!BuiltInRegistries.MENU.containsKey(id))continue;
            var type=BuiltInRegistries.MENU.get(id);var kind=entry.getValue();
            DesktopEnvironment.registerIntegration(id.getNamespace(),()->{
                DesktopMenuBindings.register(type,new DesktopMenuBindings.SlotAccess(){
                    @Override public int size(AbstractContainerMenu menu){return menu.slots.size();}
                    @Override public boolean isVirtualSlot(AbstractContainerMenu menu,Slot slot){return ghost(kind)&&!isPlayer(menu,slot);}
                    private boolean isPlayer(AbstractContainerMenu menu,Slot slot){return slot.container instanceof net.minecraft.world.entity.player.Inventory;}
                },true,CreateOwners::source,null,new DesktopMenuBindings.GesturePolicy(){
                    @Override public boolean useNativeQuickMove(AbstractContainerMenu menu){return true;}
                    @Override public boolean allowsTargetedQuickMoveSource(AbstractContainerMenu menu,Slot slot){return false;}
                    @Override public boolean canQuickCraft(AbstractContainerMenu menu,Slot slot,ItemStack carried){return !DesktopMenuBindings.isVirtualSlot(menu,slot)&&AbstractContainerMenu.canItemQuickReplace(slot,carried,true);}
                    @Override public boolean allowsPickupAll(AbstractContainerMenu menu,ItemStack stack,Slot slot){return !DesktopMenuBindings.isVirtualSlot(menu,slot);}
                });
                // Grouped toolbox summaries and ghost rules cannot be sorted as
                // ordinary storage. Only the actual package inventory opts in.
                DesktopMenuBindings.registerStorage(type,(menu,slot)->kind==Kind.FROGPORT&&!(slot.container instanceof net.minecraft.world.entity.player.Inventory));
                com.salts_inventory_update.api.desktop.DesktopItemSources.register(type,CreateOwners::itemSource);
                CreatePackets.register((MenuType)type,entry.getKey());
            });BOUND.add(entry.getKey());
        }
    }
    private static synchronized void install(){
        for(var entry:MENUS.entrySet()){
            var id=new ResourceLocation(entry.getKey());if(INSTALLED.contains(entry.getKey())||!BuiltInRegistries.MENU.containsKey(id))continue;
            var type=BuiltInRegistries.MENU.get(id);
            DesktopEnvironment.registerIntegration(id.getNamespace(),()->DesktopClientApi.registerNativeMatcher(
                new ResourceLocation("salts_inventory_compat","create-overhaul/"+id.getNamespace()+"/"+id.getPath()),100,
                Set.of(type),lookup->true,lookup->new Template(entry.getValue())));
            INSTALLED.add(entry.getKey());
        }
    }
    static final class State { boolean settings,editing,renaming;int ruleCount,ruleScroll,headerHeight=14;DesktopWindowBounds nameSource;DesktopNativeLayout layout; }
    private record Template(Kind kind) implements DesktopNativeTemplate<AbstractContainerMenu,State>,DesktopWindowPresentation {
        @Override public State createState(DesktopWindowSetupContext<AbstractContainerMenu> c){return new State();}
        @Override public void prepareLayout(DesktopWindowContext<AbstractContainerMenu,State> c){CreateNativeAccess.prepare(c.model(),c.minecraft());}
        @Override public DesktopNativeLayout nativeLayout(DesktopNativeContext<AbstractContainerMenu,State> c){
            var p=CreateLayouts.build(kind,c.nativeView(),c.model(),c.state());
            c.state().headerHeight=kind==Kind.SCHEMATICANNON?15:CreateNativeFrame.of(kind,c.state(),c.model()).height;
            c.state().layout=p;return p;
        }
        @Override public List<DesktopWindowRegion> externalRegions(DesktopWindowContext<?,?> c){
            return kind==Kind.CATEGORIES&&c.state() instanceof State state&&state.editing?List.of(DesktopWindowRegion.visual(204,97,48,48)):List.of();
        }
        @Override public void renderBehindFrame(DesktopRenderContext<?,?> c){
            if(kind==Kind.CATEGORIES&&c.state() instanceof State state&&state.editing){
                // Same native item, scale and anchor as Create's renderForeground,
                // without capturing its full-screen modal gradient around it.
                var stack=BuiltInRegistries.ITEM.get(new ResourceLocation("create","stock_ticker")).getDefaultInstance();
                c.withTransform(c.windowX()+204,c.windowY()+97,3f,()->c.item(stack,0,0));
            }
        }
        @Override public DesktopWindowSurface surface(DesktopWindowContext<AbstractContainerMenu,State> c){
            if(kind==Kind.SCHEMATICANNON)return SchematicannonChrome.SURFACE;
            var frame=CreateNativeFrame.of(kind,c.state(),c.model());var surface=frame.surface();
            if(kind==Kind.FROGPORT){
                var controls=new ArrayList<DesktopWindowRegion>();
                controls.add(new DesktopWindowRegion(124,frame.height-14,13,13,true,true,true,DesktopHitRole.CONTROL));
                if(c.state()!=null&&c.state().renaming)controls.add(new DesktopWindowRegion(12,frame.height-11,110,10,true,true,true,DesktopHitRole.CONTROL));
                return surface.withRegions(controls);
            }
            return surface;
        }
        @Override public DesktopWindowDecoration<AbstractContainerMenu,State> decoration(DesktopWindowContext<AbstractContainerMenu,State> c){
            if(kind==Kind.SCHEMATICANNON)return new DesktopWindowDecoration<>() {
                @Override public DesktopChromeMetrics metrics(){return SchematicannonChrome.METRICS;}
                @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> draw,List<DesktopControlState> controls){SchematicannonChrome.paint(draw,controls,title(draw));}
            };
            var frame=CreateNativeFrame.of(kind,c.state(),c.model());
            return new DesktopWindowDecoration<>() {
                @Override public DesktopChromeMetrics metrics(){return frame.metrics();}
                @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> draw,List<DesktopControlState> controls){frame.paint(draw,controls,title(draw));}
            };
        }
        @Override public void renderNativeUnderlay(DesktopRenderContext<AbstractContainerMenu,State> c){
            if(kind==Kind.SCHEMATICANNON)return;
            var body=c.state().layout.regions().stream().filter(r->r.id().equals("native-panel")).findFirst().orElseThrow();
            int x=c.contentX()+body.x(),y=c.contentY()+body.y();
            if(kind==Kind.TOOLBOX||kind==Kind.MOUNTED_TOOLBOX)
                c.texture(new ResourceLocation("create","textures/gui/toolbox.png"),x,y+133,0,147,188,24,188,24,256,256);
            if(kind==Kind.SCHEDULE&&c.state().editing)
                c.texture(new ResourceLocation("create","textures/gui/schedule_2.png"),x,y+68,0,82,256,7,256,7,256,256);
        }
        @Override public Component title(DesktopWindowContext<AbstractContainerMenu,State> c){
            Component original=c.originalTitle();var screen=CreateNativeAccess.screen(c.model());
            if(kind==Kind.SCHEMATIC_TABLE){
                boolean uploading=Boolean.TRUE.equals(CreateNativeAccess.field(CreateNativeAccess.field(c.model(),"contentHolder"),"isUploading"));
                String status=uploading?"uploading":c.model().getSlot(1).hasItem()?"finished":null;
                if(status!=null&&CreateNativeAccess.field(screen,status) instanceof Component text)return text;
            }
            if(kind==Kind.FROGPORT){
                if(CreateNativeAccess.field(screen,"addressBox") instanceof EditBox box&&!box.getValue().isBlank())return Component.literal(box.getValue());
                if(CreateNativeAccess.field(screen,"icon") instanceof ItemStack stack)return stack.getHoverName();
            }
            if(kind==Kind.CATEGORIES)return c.state()!=null&&c.state().editing?Component.translatable("create.gui.stock_ticker.category_editor"):Component.translatable("block.create.stock_ticker");
            if(kind==Kind.STOCK)return Component.translatable("create.gui.stock_keeper.title");
            if(kind==Kind.GAUGE)return Component.translatable("create.gui.factory_panel.place_item_to_monitor");
            if(kind==Kind.REQUESTER)return Component.translatable("block.create.redstone_requester");
            if(kind==Kind.SCHEDULE&&c.state()!=null&&c.state().editing)return Component.translatable(CreateNativeAccess.field(screen,"editingCondition")!=null?"create.schedule.condition.editor":"create.schedule.instruction.editor");
            if(kind==Kind.MOUNTED_TOOLBOX)return original.copy().append(" - Mounted");
            return original;
        }
        @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> c){
            var state=c.state();var screen=CreateNativeAccess.screen(c.model());int x=c.contentX(),y=c.contentY();
            if(kind==Kind.TOOLBOX||kind==Kind.MOUNTED_TOOLBOX){
                var view=((DesktopNativeContext<?,?>)c).nativeView();
                for(var s:view.slots())if(!s.player()&&s.bounds().x()>=0&&s.menuSlotId()%4==0){
                    int index=s.menuSlotId()/4;var nativeStack=CreateNativeAccess.invoke(c.model(),"getFilter",index);
                    var count=CreateNativeAccess.invoke(c.model(),"totalCountInCompartment",index);
                    var slot=c.menuSlot(s.menuSlotId());ItemStack stack=slot!=null&&!slot.getItem().isEmpty()?slot.getItem():nativeStack instanceof ItemStack item?item:ItemStack.EMPTY;
                    for(var place:state.layout.placements(s.bounds()))if(!stack.isEmpty())c.virtualItem(stack,count instanceof Number n?n.longValue():stack.getCount(),x+place.bounds().x()+1,y+place.bounds().y()+1);
                }
            }
        }
        @Override public boolean mouseClicked(DesktopInputContext<AbstractContainerMenu,State> c,com.salts_inventory_update.api.client.input.MouseButtonEvent e,boolean twice){
            int header=c.state().headerHeight;
            if(kind==Kind.FROGPORT&&e.x()>=c.windowX()+124&&e.x()<c.windowX()+137&&e.y()>=c.windowY()+header-14&&e.y()<c.windowY()+header-1){
                var screen=CreateNativeAccess.screen(c.model());
                if(CreateNativeAccess.field(screen,"addressBox") instanceof EditBox box){boolean editing=!box.isFocused();screen.setFocused(editing?box:null);box.setFocused(editing);return true;}
            }
            return false;
        }
        @Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){CreateNativeAccess.forget(c.model());}
        @Override public String layoutRevision(DesktopWindowContext<AbstractContainerMenu,State> c){return kind==Kind.SCHEMATICANNON?"schematicannon-native-frame-1":"create-native-frame-2:"+kind+":"+(c.state()!=null&&c.state().renaming);}
    }
}
