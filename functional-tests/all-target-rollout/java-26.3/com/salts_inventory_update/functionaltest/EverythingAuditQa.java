package com.salts_inventory_update.functionaltest;

import com.google.gson.*;
import com.salts_inventory_update.client.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.Container;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.level.ServerLevel;
import java.lang.reflect.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.function.*;

/** Disposable, command-driven native-window checks. Not part of any production source set. */
public final class EverythingAuditQa {
    static final Class<?> H=InWorldGameplayHarness.class;
    static final Gson G=new GsonBuilder().setPrettyPrinting().create();
    static final Path OUT=Path.of(System.getenv("SIU_QA_OUTPUT"));
    static Minecraft mc;
    static JsonObject pending;
    static CompletableFuture<?> work;
    static long last=-1;
    static int commandResult;
    static int settle;
    static boolean probing,ready,completed;
    static String failure="";
    static final List<Map<String,Object>> history=new ArrayList<>();
    public static void initialize() {
        verifyMods();
        InWorldGameplayHarness.initialize();
        if ("true".equalsIgnoreCase(System.getenv("SIU_QA_FULL_CORE"))) return;
        Deque<?> steps=(Deque<?>)field(H,"STEPS");while(steps.size()>3)steps.removeLast();
        h("add","native-port-observation",(Runnable)()->{
            mc=Minecraft.getInstance();
            set(H,"origin",new BlockPos(8,65,8));
            server(()->{
                for(int x=4;x<=12;x++)for(int z=4;z<=13;z++)level().setBlock(new BlockPos(x,64,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);
                h("command","tp @a 8.5 65 12.5 180 8");
                h("command","difficulty peaceful");
                h("command","gamerule spawn_mobs false");
                return null;
            }).join();
            SaltsInventoryConfig.update(c->{c.hasSeenInstructionsWindow=true;c.expandableInventory=false;c.enableDetailedConsoleLogs=false;c.forcedContainerWindows.clear();});
            write("ready.json",Map.of("ready",true));ready=true;
        },(BooleanSupplier)EverythingAuditQa::tick,(Runnable)()->{},1);
    }
    static void verifyMods(){try{Class<?> loader;Object instance;Method isLoaded;try{loader=Class.forName("net.fabricmc.loader.api.FabricLoader");instance=loader.getMethod("getInstance").invoke(null);isLoaded=loader.getMethod("isModLoaded",String.class);}catch(ClassNotFoundException e){loader=Class.forName("net.neoforged.fml.ModList");instance=loader.getMethod("get").invoke(null);isLoaded=loader.getMethod("isLoaded",String.class);}for(String id:List.of("salts_inventory_controlify"))if((Boolean)isLoaded.invoke(instance,id))throw new IllegalStateException("Forbidden mod loaded: "+id);}catch(ReflectiveOperationException e){throw new IllegalStateException("Could not verify mod isolation",e);}}
    static Object field(Object receiver,String name){return h("field",receiver,name);}
    static void set(Object receiver,String name,Object value){
        Class<?> type=receiver instanceof Class<?> c?c:receiver.getClass();
        for(;type!=null;type=type.getSuperclass())try{
            Field f=type.getDeclaredField(name);f.setAccessible(true);f.set(receiver instanceof Class<?>?null:receiver,value);return;
        }catch(NoSuchFieldException ignored){}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        throw new IllegalArgumentException("Missing field "+name);
    }
    static Object call(Object receiver,String name,Object... args){
        Class<?> c=receiver instanceof Class<?> k?k:receiver.getClass();
        for(;c!=null;c=c.getSuperclass())for(Method m:c.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){
            try{m.setAccessible(true);return m.invoke(receiver instanceof Class<?>?null:receiver,args);}
            catch(IllegalArgumentException mismatch){}catch(InvocationTargetException e){throw new IllegalStateException(e.getCause());}
            catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }throw new IllegalArgumentException("Missing method "+name+" on "+receiver);
    }
    static Object h(String name,Object...args){return call(H,name,args);}
    static void write(String name,Object value){try{Files.createDirectories(OUT);Files.writeString(OUT.resolve(name),G.toJson(value));}catch(Exception e){throw new IllegalStateException(e);}}
    static CompletableFuture<?> server(Supplier<?> action){return (CompletableFuture<?>)h("server",action);}
    static ServerPlayer player(){return (ServerPlayer)h("player");}
    static ServerLevel level(){return (ServerLevel)h("serverLevel");}
    static BlockPos origin(){return (BlockPos)field(H,"origin");}
    static List<DesktopContainerSession> sessions(){return (List<DesktopContainerSession>)h("sessions");}
    static void select(){if(!sessions().isEmpty())set(H,"activeSession",sessions().get(sessions().size()-1));}
    static AbstractContainerMenu serverMenu(){return sessions().isEmpty()?player().containerMenu:(AbstractContainerMenu)h("serverMenu");}
    static String str(JsonObject c,String key,String fallback){return c.has(key)?c.get(key).getAsString():fallback;}
    static int num(JsonObject c,String key,int fallback){return c.has(key)?c.get(key).getAsInt():fallback;}
    static int left(){return ((Number)h("left")).intValue();}
    static int button(JsonObject c){return ((Number)h(num(c,"button",0)==0?"left":"right")).intValue();}
    // Protocol normalization is shared by the Python entry drivers. Direct
    // QA key commands retain the same GLFW-to-SDL-scancode translation.
    static int auditKey(int key){
        if(key>=65&&key<=90)return key-65+4;
        if(key>=49&&key<=57)return key-49+30;
        return switch(key){case 48->39;case 256->41;case 257->40;case 258->43;
            case 259->42;case 261->76;case 262->79;case 263->80;case 264->81;
            case 265->82;case 266->75;case 267->78;case 268->74;case 269->77;
            default->key;};
    }
    static Object identifier(String value){
        try{return call(Class.forName("net.minecraft.resources.Identifier"),"parse",value);}
        catch(ClassNotFoundException legacy){try{
            var type=Class.forName("net.minecraft.resources.ResourceLocation");
            try{return call(type,"parse",value);}catch(IllegalArgumentException old){return type.getConstructor(String.class).newInstance(value);}
        }catch(Exception e){throw new IllegalStateException(e);}}
    }
    static Object registry(Object registry,String id){try{return call(registry,"getValue",identifier(id));}catch(IllegalArgumentException old){return call(registry,"get",identifier(id));}}
    static ItemStack stack(JsonObject c){return new ItemStack((net.minecraft.world.item.Item)registry(BuiltInRegistries.ITEM,str(c,"item","minecraft:diamond")),num(c,"count",32));}
    static Map<String,Object> item(ItemStack stack){var value=new LinkedHashMap<String,Object>();value.put("item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());value.put("count",stack.getCount());try{value.put("components",String.valueOf(call(stack,"getComponents")));}catch(Exception ignored){}try{value.put("nbt",String.valueOf(call(stack,"getTag")));}catch(Exception ignored){}return value;}
    static List<Map<String,Object>> contents(AbstractContainerMenu menu){
        var result=new ArrayList<Map<String,Object>>();
        for(int i=0;i<menu.slots.size();i++){var s=menu.getSlot(i);var row=new LinkedHashMap<String,Object>(item(s.getItem()));row.put("index",i);row.put("x",s.x);row.put("y",s.y);row.put("type",s.getClass().getName());row.put("player",s.container instanceof net.minecraft.world.entity.player.Inventory);result.add(row);}return result;
    }
    static Map<String,Object> nativeValues(AbstractContainerMenu menu){
        var out=new LinkedHashMap<String,Object>();
        for(String name:new String[]{"getSmeltProgress","getFuelProgress","getLavaProgress","getRequiredTierData"})
            try{out.put(name,menu.getClass().getMethod(name).invoke(menu));}catch(ReflectiveOperationException absent){}
        try{
            var method=menu.getClass().getMethod("isSlotDisabled",Slot.class);var disabled=new ArrayList<Integer>();
            for(int i=0;i<menu.slots.size();i++)if(menu.getSlot(i).getClass().getName().endsWith("ForgeInputSlot")&&Boolean.TRUE.equals(method.invoke(menu,menu.getSlot(i))))disabled.add(i);
            out.put("disabled",disabled);
        }catch(ReflectiveOperationException absent){}
        return out;
    }
    static void clearCaptureMessages(){
        for(String name:List.of("getToasts","getToastManager"))try{call(call(mc,name),"clear");break;}catch(IllegalArgumentException ignored){}
        Object gui=field(mc,"gui");try{call(call(gui,"getChat"),"clearMessages",false);}catch(IllegalArgumentException moved){try{call(call(field(gui,"hud"),"getChat"),"clearMessages",false);}catch(IllegalArgumentException ignored){}}
    }
    static void auditWidgets(net.minecraft.client.gui.components.events.GuiEventListener owner,List<Object> widgets,int depth){
        if(depth>8)return;
        if(owner instanceof net.minecraft.client.gui.components.AbstractWidget w){
            var row=new LinkedHashMap<String,Object>();row.putAll(Map.of("type",w.getClass().getName(),"message",w.getMessage().getString(),"active",w.active,"visible",w.visible,"x",w.getX(),"y",w.getY(),"width",w.getWidth(),"height",w.getHeight()));
            if(w.getClass().getName().equals("appeng.client.gui.widgets.TabButton"))try{row.put("tabStyle",String.valueOf(field(w,"style")));row.put("tabIcon",String.valueOf(field(w,"icon")));}catch(Exception ignored){}
            widgets.add(row);
        }
        if(owner instanceof net.minecraft.client.gui.components.events.ContainerEventHandler parent)for(var child:parent.children())auditWidgets(child,widgets,depth+1);
    }
    static Map<String,Object> state(){
        var out=new LinkedHashMap<String,Object>();Object nativeScreen=h("nativeScreen");out.put("screen",nativeScreen==null?"none":nativeScreen.getClass().getName());out.put("sessions",sessions().size());
        try{Object actual=h("nativeScreen");out.put("currentScreen",actual==null?"none":actual.getClass().getName());out.put("nativeCaptureScope",com.salts_inventory_update.client.automatic.AutomaticNativeScope.current()!=null);}catch(Exception ignored){}
        if(nativeScreen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container){
            out.put("nativeBounds",Map.of("x",field(container,"leftPos"),"y",field(container,"topPos"),"width",field(container,"imageWidth"),"height",field(container,"imageHeight")));
            out.put("nativeSlots",contents(container.getMenu()));
        }
        out.put("guiWidth",mc.getWindow().getGuiScaledWidth());out.put("guiHeight",mc.getWindow().getGuiScaledHeight());
        out.put("automaticReady",field(DesktopContainerClient.class,"automaticNativeReady"));
        out.put("advertisedMenus",com.salts_inventory_update.client.automatic.AutomaticNativeWindow.advertisedMenuIds(com.salts_inventory_update.api.client.desktop.DesktopClientApi.supportedMenuIds()));
        out.put("profileMenus",com.salts_inventory_update.client.automatic.NativeProfileFiles.menuIds());
        var windows=new ArrayList<Map<String,Object>>();
        for(var session:sessions()){
            var row=new LinkedHashMap<String,Object>();row.put("sessionId",session.sessionId());row.put("menu",session.menu().getClass().getName());try{row.put("menuId",BuiltInRegistries.MENU.getKey(session.menu().getType()).toString());}catch(Exception ignored){}row.put("source",session.sourceKey());row.put("slots",contents(session.menu()));row.put("nativeValues",nativeValues(session.menu()));
            Object window=h("windowSession",session.sessionId());if(window==null)continue;row.put("locked",field(window,"locked"));
            row.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
            Object decoration=field(window,"apiDecoration");row.put("decoration",decoration==null?"default":decoration.getClass().getName());Object definition=field(window,"apiDefinition");row.put("definition",definition==null?"none":definition.getClass().getName());
            if(definition!=null&&definition.getClass().getSimpleName().equals("NativeTemplateWindow")){row.put("layout",call(definition,"layout").toString()); row.put("template",call(definition,"layout"));row.put("layoutError",field(definition,"layoutError"));row.put("inspecting",field(definition,"inspecting"));}
            Object book=field(window,"recipeBook");row.put("recipeVisible",book!=null && (Boolean)call(book,"isVisible"));
            Object nativeState=field(window,"apiState");
            if(nativeState!=null)try{
                Object screen;try{screen=field(nativeState,"screen");}catch(Exception missing){screen=field(nativeState,"content");}
                row.put("nativeScreen",screen.getClass().getName());
                // Read-only observation of the original AE2 view; never alters production objects.
                if((screen.getClass().getName().startsWith("de.mari_023.ae2wtlib.") || screen.getClass().getName().startsWith("appeng.")))try{
                    var widgets=new ArrayList<Object>();auditWidgets((net.minecraft.client.gui.components.events.GuiEventListener)screen,widgets,0);row.put("nativeWidgets",widgets);
                    row.put("wirelessGeometry",call(screen,"desktopFidelityGeometry"));
                    row.put("nativeBounds",Map.of("x",call(screen,"getGuiLeft"),"y",call(screen,"getGuiTop")));
                    var positions=new ArrayList<Object>();for(var slot:session.menu().slots){var semantic=call(session.menu(),"getSlotSemantic",slot);positions.add(Map.of("index",session.menu().slots.indexOf(slot),"semantic",String.valueOf(semantic),"visible",call(screen,"desktopSlotVisible",slot)));}row.put("slotVisibility",positions);
                }catch(Exception e){row.put("wirelessObserverError",e.toString());}

                if(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container){
                    row.put("nativeBounds",Map.of("x",field(container,"leftPos"),"y",field(container,"topPos"),"width",field(container,"imageWidth"),"height",field(container,"imageHeight")));
                    var widgets=new ArrayList<Object>();auditWidgets(container,widgets,0);row.put("nativeWidgets",widgets);
                }
            }catch(Exception ignored){}
            if(nativeState!=null&&nativeState.getClass().getName().contains("chipped.ChippedWindow$State")){var custom=new LinkedHashMap<String,Object>();for(String name:List.of("selected","mode","picker","pending","scroll","eligible"))try{Object value=field(nativeState,name);custom.put(name,value==null||value instanceof Number||value instanceof Boolean?value:String.valueOf(value));}catch(Exception ignored){}row.put("chippedState",custom);}
            try{row.put("nativePreview",item((ItemStack)call(session.menu(),"getPreviewStack")));}catch(Exception ignored){}
            windows.add(row);
        }out.put("windows",windows);
        var workspace=new ArrayList<Object>();Object desktop=h("screen");
        if(desktop!=null)for(Object window:(List<?>)field(desktop,"windows")){
            String kind=String.valueOf(field(window,"kind"));if(kind.equals("CONTAINER")||Boolean.TRUE.equals(field(window,"persistentHidden")))continue;
            var info=new LinkedHashMap<String,Object>();info.put("kind",kind);info.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
            if(kind.equals("INVENTORY")){
                var cells=new ArrayList<Object>();var inventorySlots=(List<Slot>)call(desktop,"mainInventorySlots");
                Object layout=call(desktop,"storageLayout",window,call(desktop,"inventoryVirtualSlotCount"));
                int columns=((Number)call(layout,"columns")).intValue(),rows=((Number)call(layout,"visibleRows")).intValue(),scroll=((Number)field(window,"scrollRow")).intValue();
                int cx=((Number)call(window,"contentX")).intValue(),cy=((Number)call(window,"contentY")).intValue();
                for(int row=0;row<rows;row++)for(int col=0;col<columns;col++){
                    int index=scroll*columns+row*columns+col;if(index>=inventorySlots.size())continue;
                    var cell=inventorySlots.get(index);cells.add(Map.of("inventorySlot",cell.getContainerSlot(),"x",cx+col*18+8,"y",cy+row*18+8));
                }info.put("inventoryCells",cells);
            }workspace.add(info);
        }out.put("worldLoaded",mc.player!=null&&mc.level!=null);
        try{Object actual=h("nativeScreen");if(actual instanceof net.minecraft.client.gui.screens.Screen screen&&windows.isEmpty()){
            var widgets=new ArrayList<Object>();auditWidgets(screen,widgets,0);out.put("widgets",widgets);
            if(screen instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> container){out.put("bounds",Map.of("x",field(container,"leftPos"),"y",field(container,"topPos"),"width",field(container,"imageWidth"),"height",field(container,"imageHeight")));out.put("menuClass",container.getMenu().getClass().getName());out.put("slots",contents(container.getMenu()));}
        }}catch(Exception ignored){}
        out.put("workspaceWindows",workspace);out.put("cursor",item(mc.player.inventoryMenu.getCarried()));return out;
    }
    static Map<String,Object> authoritative(){
        var out=new LinkedHashMap<String,Object>();out.put("slots",contents(serverMenu()));out.put("cursor",item(player().inventoryMenu.getCarried()));out.put("menu",serverMenu().getClass().getName());out.put("commandResult",commandResult);out.put("nativeValues",nativeValues(serverMenu()));var inventory=new ArrayList<Object>();for(int i=0;i<player().getInventory().getContainerSize();i++){var value=new LinkedHashMap<String,Object>(item(player().getInventory().getItem(i)));value.put("index",i);try{value.put("components",String.valueOf(call(player().getInventory().getItem(i),"getComponents")));}catch(Exception ignored){}try{value.put("nbt",String.valueOf(call(player().getInventory().getItem(i),"getTag")));}catch(Exception ignored){}inventory.add(value);}out.put("playerInventory",inventory);try{out.put("nativePreview",item((ItemStack)call(serverMenu(),"getPreviewStack")));}catch(Exception ignored){}
        try{
            var method=serverMenu().getClass().getMethod("isSlotDisabled",Slot.class);
            var disabled=new ArrayList<Integer>();for(int i=0;i<serverMenu().slots.size();i++)if(Boolean.TRUE.equals(method.invoke(serverMenu(),serverMenu().getSlot(i))))disabled.add(i);
            out.put("disabledNativeSlots",disabled);
        }catch(ReflectiveOperationException absent){}
        Object sessions=((Map<?,?>)field(com.salts_inventory_update.server.DesktopContainerSessions.class,"PLAYERS")).get(player());
        if(sessions!=null){out.put("automaticReady",field(sessions,"automaticNativeReady"));out.put("supportedMenus",field(sessions,"supportedMenuIds"));out.put("templates",field(sessions,"nativeTemplateMenuIds"));}
        return out;
    }
    static void close(){InventoryDesktopScreen.permanentlyCloseAllOpenWindows(mc);if(mc.player.containerMenu!=mc.player.inventoryMenu)mc.player.closeContainer();h("setNativeScreen",new Object[]{null});}
    static void open(BlockPos pos,boolean realUse){
        var p=player();var level=level();
        Object provider=call(level.getBlockState(pos),"getMenuProvider",level,pos);
        if(provider==null&&level.getBlockEntity(pos) instanceof MenuProvider m)provider=m;
        if(provider!=null&&!realUse){p.openMenu((MenuProvider)provider);return;}
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0,0.5,0),net.minecraft.core.Direction.UP,pos,false);
        call(p.gameMode,"useItemOn",p,level,p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
    }
    static void execute(JsonObject c){
        work=null;failure="";settle=num(c,"ticks",16);select();
        if(regressionCommand(c))return;
        switch(str(c,"type","inspect")){
            case "native-wands-tools-request"->{
                // The publisher's ordinary Shift+menu key request. This does
                // not fabricate a menu or validate raw operating-system input.
                if(!net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(mc.player.getMainHandItem().getItem()).getNamespace().equals("wands"))
                    throw new IllegalStateException("Hold a Building Wands item first");
                try {
                    Class<?> keys=Class.forName("net.nicguzzo.wands.WandsMod$WandKeys");
                    Object menu=java.util.Arrays.stream(keys.getEnumConstants()).filter(v->((Enum<?>)v).name().equals("MENU")).findFirst().orElseThrow();
                    wandsKeySender()
                        .invoke(null,((Enum<?>)menu).ordinal(),true,false);
                }catch(ReflectiveOperationException error){throw new IllegalStateException("Publisher tool-picker key request failed",error);}
            }
case "catalog"->{Set<String> namespaces=new HashSet<>();if(c.has("namespaces"))for(var value:c.getAsJsonArray("namespaces"))namespaces.add(value.getAsString());
                write("catalog.json",Map.of("menus",BuiltInRegistries.MENU.keySet().stream().filter(id->(namespaces.isEmpty()||namespaces.contains(id.getNamespace()))).map(Object::toString).sorted().toList(),"blocks",BuiltInRegistries.BLOCK.keySet().stream().filter(id->(namespaces.isEmpty()||namespaces.contains(id.getNamespace()))).map(Object::toString).sorted().toList(),"items",BuiltInRegistries.ITEM.keySet().stream().filter(id->(namespaces.isEmpty()||namespaces.contains(id.getNamespace()))).map(Object::toString).sorted().toList(),"origin",origin().toShortString()));}
            case "server-command"->work=server(()->{Object commands=call(level().getServer(),"getCommands");commandResult=0;var source=((net.minecraft.commands.CommandSourceStack)call(player(),"createCommandSourceStack")).withCallback((success,result)->commandResult=result);call(commands,"performPrefixedCommand",source,str(c,"command",""));return null;});
            case "recipe-book"->{var w=h("windowSession",sessions().get(sessions().size()-1).sessionId());Object book=call(h("screen"),"recipeBook",w);if(book==null)throw new IllegalStateException("No native recipe book");call(book,"toggleVisibility");}
            case "fixture"->{close();work=server(()->{
                var p=player();p.inventoryMenu.setCarried(ItemStack.EMPTY);p.getInventory().clearContent();
                var block=(net.minecraft.world.level.block.Block)registry(BuiltInRegistries.BLOCK,str(c,"block","netherchested:nether_chest"));
                if(block==net.minecraft.world.level.block.Blocks.AIR)throw new IllegalArgumentException("Missing fixture block");
                var pos=origin();
                for(int x=-4;x<=4;x++)for(int z=-4;z<=4;z++){level().setBlock(pos.offset(x,-1,z),net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(),3);for(int y=0;y<4;y++)level().setBlock(pos.offset(x,y,z),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);}
                level().setBlock(pos,block.defaultBlockState(),3);
                if(c.has("campfire")&&c.get("campfire").getAsBoolean())level().setBlock(pos.below(),net.minecraft.world.level.block.Blocks.CAMPFIRE.defaultBlockState(),3);
                block.setPlacedBy(level(),pos,level().getBlockState(pos),p,new ItemStack(block.asItem()));
                return null;
            });}
            case "open"->work=server(()->{open(origin(),c.has("realUse")&&c.get("realUse").getAsBoolean());return null;});
            case "client-use-block"->mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(origin()).add(0,0.5,0),net.minecraft.core.Direction.UP,origin(),false));
            case "client-use-item"->mc.gameMode.useItem(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND);
            case "client-use-block-ray"->{if(!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)||!hit.getBlockPos().equals(origin()))throw new IllegalStateException("Player ray does not hit fixture");mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}
            case "client-use-any-block-ray"->{if(!(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit)||hit.getType()!=net.minecraft.world.phys.HitResult.Type.BLOCK)throw new IllegalStateException("Player ray does not hit a block");mc.gameMode.useItemOn(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND,hit);}
            case "aim-entity"->{
                String wanted=str(c,"entity","");
                var entity=mc.level.getEntities(mc.player,mc.player.getBoundingBox().inflate(num(c,"radius",12))).stream().filter(e->BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(wanted)).findFirst().orElseThrow(()->new IllegalStateException("Entity not near player: "+wanted));
                var d=entity.getBoundingBox().getCenter().subtract(mc.player.getEyePosition());
                mc.player.setYRot((float)(Math.atan2(d.z,d.x)*180/Math.PI-90));
                mc.player.setXRot((float)(-Math.atan2(d.y,Math.sqrt(d.x*d.x+d.z*d.z))*180/Math.PI));
            }
            case "sneak"->{boolean down=c.has("down")&&c.get("down").getAsBoolean();mc.options.keyShift.setDown(down);}
            case "client-use-entity-ray"->{
                if(!(mc.hitResult instanceof net.minecraft.world.phys.EntityHitResult hit))throw new IllegalStateException("Player ray does not hit an entity");
                var entity=hit.getEntity();String wanted=str(c,"entity","");
                if(!wanted.isEmpty()&&!BuiltInRegistries.ENTITY_TYPE.getKey(entity.getType()).toString().equals(wanted))throw new IllegalStateException("Player ray hits a different entity");
                boolean shift=c.has("shift")&&c.get("shift").getAsBoolean();mc.options.keyShift.setDown(shift);mc.player.setShiftKeyDown(shift);work=server(()->{player().setShiftKeyDown(shift);return null;});
                mc.gameMode.interact(mc.player,entity,hit,net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            case "client-use-entity"->{String wanted=str(c,"entity","");var entities=mc.level.getEntities(mc.player,new net.minecraft.world.phys.AABB(origin()).inflate(num(c,"radius",8)));var entity=entities.stream().filter(e->BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(wanted)).findFirst().orElseThrow(()->new IllegalStateException("Entity not in interaction range: "+wanted));boolean shift=c.has("shift")&&c.get("shift").getAsBoolean();mc.player.setShiftKeyDown(shift);work=server(()->{player().setShiftKeyDown(shift);return null;});mc.gameMode.interact(mc.player,entity,new net.minecraft.world.phys.EntityHitResult(entity),net.minecraft.world.InteractionHand.MAIN_HAND);}
            case "crouch-open"->work=server(()->{var p=player();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,ItemStack.EMPTY);p.setShiftKeyDown(true);try{open(origin(),true);}finally{p.setShiftKeyDown(false);}return null;});
            case "seed-player"->work=server(()->{for(var element:c.getAsJsonArray("items")){var value=element.getAsJsonObject();player().getInventory().setItem(num(value,"slot",9),stack(value));}player().inventoryMenu.broadcastFullState();return null;});
            case "player-click"->{if(h("windowKind","INVENTORY")==null)InventoryDesktopScreen.openOrToggleInventory(mc);double[] point=(double[])h("slotPoint",0,num(c,"slot",9),h("windowKind","INVENTORY"));h("mouse","mouseClicked",point[0],point[1],button(c),0D,0D);h("mouse","mouseReleased",point[0],point[1],button(c),0D,0D);}
            case "quick-desktop"->{select();double[] point=(double[])h("slotPoint",num(c,"slot",0));Object hit=call(h("screen"),"slotAt",point[0],point[1]);call(h("screen"),"quickMoveSlot",hit);}
            case "seed"->work=server(()->{serverMenu().getSlot(num(c,"slot",0)).set(stack(c));serverMenu().broadcastFullState();return null;});
            case "click"->{select();h("clickSlot",num(c,"slot",0),button(c));}
            case "store"->h("storeInPlayer");
            case "sort"->{var w=h("windowSession",sessions().get(sessions().size()-1).sessionId());h("clickWindowControl",w,"SORT_INVENTORY");}
            case "move"->{var w=h("windowSession",sessions().get(sessions().size()-1).sessionId());double x=((Number)field(w,"x")).doubleValue()+8,y=((Number)field(w,"y")).doubleValue()+5,dx=num(c,"dx",60),dy=num(c,"dy",40);h("mouse","mouseClicked",x,y,left(),0D,0D);h("mouse","mouseDragged",x+dx,y+dy,left(),dx,dy);h("mouse","mouseReleased",x+dx,y+dy,left(),0D,0D);}
            case "item-use"->work=server(()->{var p=player();p.setShiftKeyDown(c.has("shift")&&c.get("shift").getAsBoolean());p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack(c));p.getMainHandItem().use(level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);return null;});
            case "remove-source"->work=server(()->{level().setBlock(origin(),net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),3);return null;});
            case "decline"->DesktopContainerClient.declineAutomaticWindow(sessions().get(sessions().size()-1).sessionId());
            case "native-inspector"->InventoryDesktopScreen.current(mc).inspectNativeWindow();
            case "native-template-probe"->{
                var type=(net.minecraft.world.inventory.MenuType<?>)registry(BuiltInRegistries.MENU,"netherchested:nether_chest");
                call(com.salts_inventory_update.api.client.desktop.DesktopClientApi.class,"registerNativeMatcher",
                    identifier("salts_native_qa:probe"),100000,java.util.Set.of(type),(com.salts_inventory_update.api.client.desktop.DesktopWindowMatcher)(lookup->true),
                    (Function<Object,Object>)(lookup->new com.salts_inventory_update.api.client.desktop.DesktopNativeTemplate<AbstractContainerMenu,Void>(){
                        @Override public com.salts_inventory_update.api.client.desktop.DesktopNativeLayout nativeLayout(com.salts_inventory_update.api.client.desktop.DesktopNativeContext<AbstractContainerMenu,Void> context){
                            var bounds=com.salts_inventory_update.api.client.desktop.DesktopNativeView.union(context.nativeView().slots().stream().filter(s->!s.player()).map(s->s.bounds()).toList()).orElseThrow();
                            return com.salts_inventory_update.api.client.desktop.DesktopNativeLayout.builder().keep("probe",bounds,0,0).build();
                        }
                    }));DesktopContainerClient.refreshNativeTemplates();
            }
            case "profile"->{
                try {
                    var path=mc.gameDirectory.toPath().resolve("config/salts_inventory_update/native-windows/_siu_port_probe.json");
                    if(c.has("clear")&&c.get("clear").getAsBoolean())Files.deleteIfExists(path);
                    else {
                        var json=new JsonObject();json.addProperty("menu","netherchested:nether_chest");
                        json.addProperty("minecraft",str(c,"minecraft",com.salts_inventory_update.VersionInfo.MINECRAFT_VERSION));
                        var layout=com.salts_inventory_update.api.client.desktop.DesktopNativeLayout.builder().keep("probe",new com.salts_inventory_update.api.client.desktop.DesktopWindowBounds(7,18,162,108),0,0).build();
                        json.add("layout",com.salts_inventory_update.api.client.desktop.DesktopNativeProfiles.encode(layout));
                        Files.createDirectories(path.getParent());Files.writeString(path,G.toJson(json));
                    }
                    com.salts_inventory_update.client.automatic.NativeProfileFiles.reload();DesktopContainerClient.refreshNativeTemplates();
                }catch(java.io.IOException e){throw new IllegalStateException(e);}
            }
            case "native-click"->{Object s=h("nativeScreen");double x=c.get("x").getAsDouble(),y=c.get("y").getAsDouble();((net.minecraft.client.gui.screens.Screen)s).mouseMoved(x,y);call(s,"mouseClicked",new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(button(c),0)),false);call(s,"mouseReleased",new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(button(c),0)));}
            case "xy-press"->{double x=c.get("x").getAsDouble(),y=c.get("y").getAsDouble();h("mouse","mouseClicked",x,y,num(c,"button",0),0D,0D);}
            case "xy-release"->{double x=c.get("x").getAsDouble(),y=c.get("y").getAsDouble();h("mouse","mouseReleased",x,y,num(c,"button",0),0D,0D);}
            case "drag"->{double x=c.get("x").getAsDouble(),y=c.get("y").getAsDouble();h("mouse","mouseDragged",x,y,button(c),(double)num(c,"dx",0),(double)num(c,"dy",0));}
            case "xy-click"->{double x=c.get("x").getAsDouble(),y=c.get("y").getAsDouble();h("mouse","mouseClicked",x,y,left(),0D,0D);h("mouse","mouseReleased",x,y,left(),0D,0D);}
            case "hover"->{var window=mc.getWindow();double scale=window.getGuiScale();h("spatialMove",c.get("x").getAsDouble()*scale,c.get("y").getAsDouble()*scale,0D,0D);}
            case "inventory"->com.salts_inventory_update.api.client.desktop.DesktopClientApi.openPlayerWorkspace();
            case "native-inventory-control"->{close();try{Class<?> type=Class.forName("net.minecraft.client.gui.screens.inventory.InventoryScreen");var ctor=type.getConstructor(net.minecraft.world.entity.player.Player.class);call(mc,"setScreen",ctor.newInstance(mc.player));}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}}
            case "xy-scroll"->{double x=c.get("x").getAsDouble(),y=c.get("y").getAsDouble();h("scroll",x,y,(double)num(c,"amount",-1));}
            case "text"->{for(char letter:str(c,"text","").toCharArray())call(h("screen"),"charTyped",new net.minecraft.client.input.CharacterEvent(letter));}
            case "key"->call(h("screen"),"keyPressed",new net.minecraft.client.input.KeyEvent(c.has("sdl")&&c.get("sdl").getAsBoolean()?num(c,"key",41):auditKey(num(c,"key",256)),num(c,"keycode",0),num(c,"modifiers",0)));
            case "close"->{mc.options.keyShift.setDown(false);close();}
            case "capture"->clearCaptureMessages();
            case "wait","inspect","finish"->{}
            default->throw new IllegalArgumentException("Unknown command");
        }
    }
    static boolean tick(){
        if(completed)return true;if(!ready)return false;
        // The command loop is deliberately idle between observations; the parent
        // harness's 45-second per-step deadline applies to automatic steps only.
        set(H,"stepStarted",System.currentTimeMillis());
        try{
            if(pending==null){
                Path file=OUT.resolve("command.json");if(!Files.exists(file))return false;
                var c=JsonParser.parseString(Files.readString(file)).getAsJsonObject();if(c.get("sequence").getAsLong()<=last)return false;
                pending=c;last=c.get("sequence").getAsLong();probing=false;failure="";
                try{execute(c);}catch(Throwable e){failure=e.toString();work=null;settle=3;}return false;
            }
            if(work!=null&&!work.isDone())return false;
            if(work!=null&&work.isCompletedExceptionally()){try{work.join();}catch(Throwable e){failure=e.toString();}work=null;}
            if(settle-->0)return false;
            if(!probing){probing=true;select();work=server(str(pending,"type","").startsWith("regression-")?()->Map.of("regressionBarrier",true):EverythingAuditQa::authoritative);return false;}
            var result=new LinkedHashMap<String,Object>();result.put("sequence",last);result.put("command",pending);result.put("error",failure);result.put("client",str(pending,"type","").startsWith("regression-")&&!str(pending,"type","").equals("regression-move")?Map.of("regression",true):state());
            try{result.put("server",work.join());}catch(Throwable e){result.put("probeError",e.toString());}
            if(pending.has("capture"))h("capture",pending.get("capture").getAsString());
            regressionObserve(result);write("reply.json",result);
            completed=str(pending,"type","").equals("finish");pending=null;work=null;return completed;
        }catch(Throwable e){write("driver-error.json",Map.of("sequence",last,"error",e.toString()));pending=null;work=null;return false;}
    }

    static java.lang.reflect.Method wandsKeySender() throws ReflectiveOperationException {
        try{return Class.forName("net.nicguzzo.wands.networking.Networking").getMethod("send_key",int.class,boolean.class,boolean.class);}
        catch(NoSuchMethodException moved){return Class.forName("net.nicguzzo.wands.networking.ClientNetworking").getMethod("SendKbPacket",int.class,boolean.class,boolean.class);}
    }





























































































































    // Excluded regression commands: use real owned sessions and screen callbacks.
    // These helpers never register a menu/provider or replace publisher logic.
    static boolean regressionCommand(JsonObject c) {
        String kind=str(c,"type","");
        if(!kind.startsWith("regression-"))return false;
        if(mc.getSingleplayerServer()==null||mc.player==null)throw new IllegalStateException("RegressionWorldDisconnected");
        settle=num(c,"ticks",8);
        if(kind.equals("regression-world"))return true;
        if(kind.equals("regression-network-delay")){qaNetworkDelay(c);return true;}
        if(kind.equals("regression-network-status")){qaNetworkStatus();return true;}
        if(kind.equals("regression-debug-capture")){
            com.salts_inventory_update.SaltsInventoryRuntime.setDetailedConsoleLogs(c.has("enabled")&&c.get("enabled").getAsBoolean());return true;
        }
        if(kind.equals("regression-geckolib-id-comparisons")){
            work=server(()->{
                var methods=Arrays.stream(AbstractContainerMenu.class.getDeclaredMethods()).filter(m->
                    m.getName().endsWith("geckolib$xnorGeckolibStackIds")&&m.getParameterCount()==2&&m.getReturnType()==boolean.class).toList();
                if(methods.size()!=1)throw new IllegalStateException("Expected one actual GeckoLib ID comparator, found "+methods.size());
                var method=methods.get(0);method.setAccessible(true);
                var empty=new net.minecraft.nbt.CompoundTag();var zero=new net.minecraft.nbt.CompoundTag();zero.putInt("GeckoLibID",0);
                var other=new net.minecraft.nbt.CompoundTag();other.putInt("GeckoLibID",17);
                var pairs=new net.minecraft.nbt.CompoundTag[][]{{null,empty},{empty,null},{empty,empty},{zero,null},{zero,empty},{zero,zero},{zero,other},{other,other}};
                var names=List.of("null-empty","empty-null","empty-empty","zero-null","zero-empty","zero-zero","zero-other","other-other");
                var rows=new ArrayList<Object>();
                try{for(int i=0;i<pairs.length;i++)rows.add(Map.of("name",names.get(i),"actual",method.invoke(null,pairs[i][0],pairs[i][1])));}
                catch(ReflectiveOperationException failure){throw new IllegalStateException("Native GeckoLib comparator invocation failed",failure);}
                write("regression-geckolib-id-comparisons.json",Map.of("actualNativeMethod",method.getName(),"cases",rows,
                    "scope","Native helper invoked only on fresh disposable tags; no inventory, cursor or synchronization cache is assigned."));return null;
            });return true;
        }
        if(kind.equals("regression-pointer-position")){
            Object window=mc.getWindow();long handle;
            try{handle=((Number)call(window,"handle")).longValue();}
            catch(IllegalArgumentException legacy){handle=((Number)call(window,"getWindow")).longValue();}
            double x=c.get("x").getAsDouble()*((Number)call(window,"getScreenWidth")).doubleValue()/mc.getWindow().getGuiScaledWidth();
            double y=c.get("y").getAsDouble()*((Number)call(window,"getScreenHeight")).doubleValue()/mc.getWindow().getGuiScaledHeight();
            // Deliver the native callback without moving the OS pointer. Some
            // publishers derive click targets during their preceding render.
            try{call(mc.mouseHandler,"onMove",handle,x,y,0D,0D);}
            catch(IllegalArgumentException legacy){call(mc.mouseHandler,"onMove",handle,x,y);}
            write("regression-pointer.json",Map.of("screenX",x,"screenY",y,"guiX",c.get("x").getAsDouble(),"guiY",c.get("y").getAsDouble()));
            return true;
        }
        if(kind.equals("regression-world-use-button")){
            // Native mouse callback -> key mapping -> Minecraft use handling,
            // including loader click events. No OS pointer or provider call.
            h("dispatchMovementMouse",c.has("down")&&c.get("down").getAsBoolean()?"PRESS":"RELEASE");
            return true;
        }
        if(kind.equals("regression-ssn-hover")){
            Object w=regressionManagedWindow(num(c,"session",-1)),definition=field(w,"apiDefinition");
            Object nativeScreen=field(field(definition,"initial"),"screen"),network=call(nativeScreen,"getNetwork");
            Object hovered;
            try{hovered=call(network,"getStackUnderMouse");}
            catch(IllegalArgumentException legacyHover){
                if(!network.getClass().getName().equals("com.lothrazar.storagenetwork.gui.NetworkWidget"))throw legacyHover;
                hovered=field(network,"stackUnderMouse");
            }
            write("regression-ssn-hover.json",Map.of("stack",regressionStack((ItemStack)hovered),
                "nativeScreen",nativeScreen.getClass().getName()));return true;
        }
        if(kind.equals("regression-native-widget-catalog")){
            var rows=new ArrayList<Map<String,Object>>();Object desktop=h("screen");
            for(Object session:sessions()){
                int id=((Number)call(session,"sessionId")).intValue();Object w=h("windowSession",id);if(w==null)continue;
                Object definition=field(w,"apiDefinition"),state=field(w,"apiState");if(definition==null||state==null)continue;
                if(definition.getClass().getName().endsWith(".NativeTemplateWindow")){
                    Object view=field(definition,"view");
                    for(Object widget:(List<?>)call(view,"widgets")){
                        Object b=call(widget,"bounds");var row=regressionWidgetRow(id,definition,call(widget,"type").toString(),((net.minecraft.network.chat.Component)call(widget,"label")).getString(),
                            ((Number)call(b,"x")).intValue(),((Number)call(b,"y")).intValue(),((Number)call(b,"width")).intValue(),((Number)call(b,"height")).intValue());
                        row.put("visible",call(widget,"visible"));row.put("active",call(widget,"active"));rows.add(row);
                    }
                    Object screen=field(field(definition,"initial"),"screen");
                    for(Object child:(List<?>)call(screen,"children"))if(child.getClass().getName().startsWith("com.tiviacz.travelersbackpack.")&&child.getClass().getSimpleName().equals("SettingsWidget")){
                        int[] b=(int[])call(child,"getWidgetSizeAndPos");rows.add(regressionWidgetRow(id,definition,child.getClass().getName(),"",b[0],b[1],b[2],b[3]));
                    }
                }else if(definition.getClass().getName().contains("SophisticatedHostedScreenDefinition")){
                    Object components=call(field(state,"access"),"components");
                    var pendingWidgets=new ArrayDeque<Object>((List<?>)call(components,"interactiveWidgets"));
                    Set<Object> seen=Collections.newSetFromMap(new IdentityHashMap<>());
                    while(!pendingWidgets.isEmpty()){
                        Object widget=pendingWidgets.removeFirst();if(!seen.add(widget))continue;
                        try{
                            int x=((Number)call(widget,"getX")).intValue(),y=((Number)call(widget,"getY")).intValue();
                            int width=((Number)call(widget,"getWidth")).intValue(),height=((Number)call(widget,"getHeight")).intValue();
                            if(widget.getClass().getSimpleName().equals("StorageSettingsTab")){width=Math.min(width,18);height=Math.min(height,18);}
                            var row=regressionWidgetRow(id,definition,widget.getClass().getName(),"",x,y,width,height);
                            row.put("native_screen",field(state,"screen").getClass().getName());
                            try{row.put("visible",field(widget,"visible"));}catch(IllegalArgumentException absent){}
                            rows.add(row);
                        }catch(IllegalArgumentException noBounds){}
                        try{for(Object child:(Iterable<?>)call(widget,"children"))pendingWidgets.add(child);}catch(IllegalArgumentException noChildren){}
                    }
                }
            }
            write("regression-native-widget-catalog.json",rows);return true;
        }
        if(kind.equals("regression-command-info")){
            work=server(()->{
                var game=mc.getSingleplayerServer();String text=str(c,"command","");
                var parsed=game.getCommands().getDispatcher().parse(text,game.createCommandSourceStack());
                var errors=new ArrayList<String>();for(var error:parsed.getExceptions().values())errors.add(error.getRawMessage().getString());
                write("regression-command-info.json",Map.of("command",text,"fullyParsed",!parsed.getReader().canRead()&&!parsed.getContext().getNodes().isEmpty(),
                    "unread",parsed.getReader().getRemaining(),"errors",errors));return null;
            });return true;
        }
        if(kind.equals("regression-use-held-item")){
            // A desktop can consume ordinary movement polling. Deliver the
            // real modifier packet immediately before the real item-use packet.
            boolean previous=mc.player.isShiftKeyDown(),shift=c.has("shift")&&c.get("shift").getAsBoolean();
            regressionSendShift(shift);mc.player.setShiftKeyDown(shift);
            try {mc.gameMode.useItem(mc.player,net.minecraft.world.InteractionHand.MAIN_HAND);}
            finally {regressionSendShift(previous);mc.player.setShiftKeyDown(previous);}
            return true;
        }
        if(kind.equals("regression-use-entity")){
            String wanted=str(c,"entity","");
            var entity=mc.level.getEntities(mc.player,mc.player.getBoundingBox().inflate(12)).stream()
                .filter(e->BuiltInRegistries.ENTITY_TYPE.getKey(e.getType()).toString().equals(wanted)).findFirst()
                .orElseThrow(()->new IllegalStateException("Native interaction entity is not near the player: "+wanted));
            boolean shift=c.has("shift")?c.get("shift").getAsBoolean():mc.player.isShiftKeyDown();mc.player.setShiftKeyDown(shift);
            work=server(()->{player().setShiftKeyDown(shift);return null;});
            if(c.has("at")&&c.get("at").getAsBoolean()){
                mc.gameMode.interact(mc.player,entity,new net.minecraft.world.phys.EntityHitResult(entity),net.minecraft.world.InteractionHand.MAIN_HAND);
            }else{
                mc.gameMode.interact(mc.player,entity,new net.minecraft.world.phys.EntityHitResult(entity),net.minecraft.world.InteractionHand.MAIN_HAND);
            }
            return true;
        }
        if(kind.equals("regression-character")){
            if(h("screen")==null||h("windowKind","CHARACTER")==null)com.salts_inventory_update.client.InventoryDesktopScreen.openOrToggleCharacter(mc);
            regressionCharacterInfo();return true;
        }
        if(kind.equals("regression-character-info")){regressionCharacterInfo();return true;}
        if(kind.equals("regression-builtin-info")){
            Object w=h("windowKind",str(c,"kind","INVENTORY"));if(w==null)throw new IllegalStateException("Missing player window");
            write("regression-builtin.json",Map.of("locked",field(w,"locked"),"instance",String.valueOf(field(w,"apiInstanceId")),"bounds",Map.of("x",field(w,"x"),"y",field(w,"y"),"width",field(w,"width"),"height",field(w,"height"))));return true;
        }
        if(kind.equals("regression-builtin-control")||kind.equals("regression-builtin-move")){
            regressionDismissPopup();
            Object w=h("windowKind",str(c,"kind","INVENTORY"));if(w==null)throw new IllegalStateException("Missing player window");
            call(h("screen"),"bringToFront",w);call(h("screen"),"setFocusedWindow",w);
            if(kind.equals("regression-builtin-control"))h("clickWindowControl",w,str(c,"control","CLOSE"));
            else{
                double x=((Number)field(w,"x")).doubleValue()+8,y=((Number)field(w,"y")).doubleValue()+7,dx=num(c,"dx",-30),dy=num(c,"dy",20);int b=((Number)h("left")).intValue();
                h("mouse","mouseClicked",x,y,b,0D,0D);h("mouse","mouseDragged",x+dx,y+dy,b,dx,dy);h("mouse","mouseReleased",x+dx,y+dy,b,0D,0D);
            }regressionDismissPopup();return true;
        }
        if(kind.equals("regression-character-control")){
            regressionDismissPopup();
            Object w=h("windowKind","CHARACTER");call(h("screen"),"bringToFront",w);call(h("screen"),"setFocusedWindow",w);
            h("clickWindowControl",w,str(c,"control","CLOSE"));regressionDismissPopup();return true;
        }
        if(kind.equals("regression-attachment-click")){
            regressionDismissPopup();
            Object w=h("windowKind","CHARACTER"),desktop=h("screen");call(desktop,"bringToFront",w);call(desktop,"setFocusedWindow",w);
            Object context=call(desktop,"apiContext",w,null,0,0),attachment=null;
            for(Object candidate:(List<?>)field(w,"builtinAttachments"))if(candidate.getClass().getName().contains(str(c,"attachment",""))){attachment=candidate;break;}
            if(attachment==null)throw new IllegalStateException("Character attachment not registered: "+str(c,"attachment",""));
            var regions=(List<?>)call(attachment,"regions",context);Object region=regions.get(num(c,"region",0));
            if(!Boolean.TRUE.equals(call(region,"interactive")))throw new IllegalStateException("Attachment region is not interactive");
            double x=((Number)field(w,"x")).doubleValue()+((Number)call(region,"x")).doubleValue()+((Number)call(region,"width")).doubleValue()/2;
            double y=((Number)field(w,"y")).doubleValue()+((Number)call(region,"y")).doubleValue()+((Number)call(region,"height")).doubleValue()/2;
            int button=regressionButton(c);h("mouse","mouseClicked",x,y,button,0D,0D);h("mouse","mouseReleased",x,y,button,0D,0D);return true;
        }
        if(kind.equals("regression-native-browser")){
            work=server(()->{
                var menu=player().containerMenu;var cells=new ArrayList<Object>();
                for(int i=0;i<menu.slots.size();i++){
                    Slot slot=menu.slots.get(i);if(slot.container==player().getInventory())continue;
                    var value=new LinkedHashMap<String,Object>(regressionStack(slot.getItem()));value.put("index",i);cells.add(value);
                }
                write("regression-native-browser.json",Map.of("menuClass",menu.getClass().getName(),"slots",cells));return null;
            });return true;
        }
        if(kind.equals("regression-native-slot-tap")){
            Object s=h("nativeScreen");
            if(!(s instanceof net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?> screen))throw new IllegalStateException("Native container required");
            Slot slot=screen.getMenu().slots.get(num(c,"slot",-1));
            double x=((Number)field(screen,"leftPos")).doubleValue()+slot.x+8,y=((Number)field(screen,"topPos")).doubleValue()+slot.y+8;
            int button=regressionButton(c);screen.mouseMoved(x,y);
            var event=new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(button,0));call(s,"mouseClicked",event,false);call(s,"mouseReleased",event);
            return true;
        }
        if(kind.equals("regression-native-tap")){
            Object s=h("nativeScreen");
            if(s==null||s instanceof com.salts_inventory_update.client.InventoryDesktopScreen)throw new IllegalStateException("A preserved native screen is required");
            double x=c.get("x").getAsDouble(),y=c.get("y").getAsDouble();int button=regressionButton(c);
            ((net.minecraft.client.gui.screens.Screen)s).mouseMoved(x,y);
            var event=new net.minecraft.client.input.MouseButtonEvent(x,y,new net.minecraft.client.input.MouseButtonInfo(button,0));call(s,"mouseClicked",event,false);call(s,"mouseReleased",event);
            return true;
        }
        if(kind.equals("regression-native-key")){
            Object s=h("nativeScreen");if(s==null||s instanceof com.salts_inventory_update.client.InventoryDesktopScreen)throw new IllegalStateException("A preserved native screen is required");
            int key=num(c,"key",256),modifiers=num(c,"modifiers",0);
            call(s,"keyPressed",new net.minecraft.client.input.KeyEvent(key,num(c,"keycode",0),modifiers));
            return true;
        }
        if(kind.equals("regression-template-widget-tap")){
            Object w=regressionManagedWindow(num(c,"session",-1)),desktop=h("screen");
            call(desktop,"bringToFront",w);call(desktop,"setFocusedWindow",w);
            Object definition=field(w,"apiDefinition"),context=call(desktop,"apiContext",w,null,0,0);
            if(!definition.getClass().getName().endsWith(".NativeTemplateWindow"))throw new IllegalArgumentException("Native extraction required");
            Object view=field(definition,"view"),layout=call(definition,"layout"),widget=null;int ordinal=num(c,"ordinal",0);
            for(Object candidate:(List<?>)call(view,"widgets"))if(Boolean.TRUE.equals(call(candidate,"visible"))&&str(c,"widget_type","").equals(call(candidate,"type"))&&str(c,"message","").equals(((net.minecraft.network.chat.Component)call(candidate,"label")).getString())){if(ordinal--==0){widget=candidate;break;}}
            Object bounds;
            if(widget==null){
                Object screen=field(field(definition,"initial"),"screen");int index=num(c,"ordinal",0);Object found=null;
                for(Object child:(List<?>)call(screen,"children"))if(child.getClass().getName().equals(str(c,"widget_type",""))&&child.getClass().getSimpleName().equals("SettingsWidget")){if(index--==0){found=child;break;}}
                if(found==null)throw new IllegalStateException("Publisher widget missing or disabled");
                int[] b=(int[])call(found,"getWidgetSizeAndPos");int left=((Number)field(screen,"leftPos")).intValue(),top=((Number)field(screen,"topPos")).intValue();
                bounds=new com.salts_inventory_update.api.client.desktop.DesktopWindowBounds(b[0]-left,b[1]-top,b[2],b[3]);
            }else{
                if(!Boolean.TRUE.equals(call(widget,"active")))throw new IllegalStateException("Publisher widget missing or disabled");bounds=call(widget,"bounds");
            }
            for(Object region:(List<?>)call(layout,"regions"))if(Boolean.TRUE.equals(call(region,"interactive"))&&Boolean.TRUE.equals(call(region,"covers",bounds))){
                Object source=call(region,"source");
                double x=((Number)call(definition,"regionX",context,region)).doubleValue()+((Number)call(bounds,"x")).doubleValue()-((Number)call(source,"x")).doubleValue()+((Number)call(bounds,"width")).doubleValue()/2;
                double y=((Number)call(definition,"regionY",context,region)).doubleValue()+((Number)call(bounds,"y")).doubleValue()-((Number)call(source,"y")).doubleValue()+((Number)call(bounds,"height")).doubleValue()/2;
                int button=regressionButton(c);h("mouse","mouseClicked",x,y,button,0D,0D);h("mouse","mouseReleased",x,y,button,0D,0D);return true;
            }
            throw new IllegalStateException("Publisher widget is not retained in an interactive extraction region");
        }
        if(kind.equals("regression-reset-hand")){
            int emptySlot=0;
            mc.player.getInventory().setSelectedSlot(emptySlot);
            mc.player.setShiftKeyDown(false);
            work=server(()->{
                player().getInventory().setSelectedSlot(0);
                player().setShiftKeyDown(false);
                return null;
            });return true;
        }
        if(kind.equals("regression-select-empty-hand")){
            int emptySlot=-1;for(int i=0;i<9;i++)if(mc.player.getInventory().getItem(i).isEmpty()){emptySlot=i;break;}
            if(emptySlot<0)throw new IllegalStateException("Native block use needs an empty hotbar selection");
            // Keep the original item in its bound inventory slot. The next
            // ordinary useItemOn synchronizes selection through gameMode.
            mc.player.getInventory().setSelectedSlot(emptySlot);
            return true;
        }
        if(kind.equals("regression-native-settings")){
            work=server(()->{
                Object ps=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());
                var active=(Map<?,?>)field(ps,"sessions");Object owned=active.get(num(c,"session",-1));
                if(owned==null)throw new IllegalArgumentException("Unknown owned source");
                Object menu=field(owned,"menu");var values=new LinkedHashMap<String,Object>();
                for(var entry:c.getAsJsonArray("fields")){
                    String key=entry.getAsString();Object value;
                    if(menu.getClass().getName().startsWith("com.tom.storagemod.")&&menu.getClass().getSimpleName().contains("LevelEmitter")&&(key.equals("count")||key.equals("lessThan"))){
                        // Native menu fields are client data-slot caches. The
                        // block entity is authoritative for settings mutations.
                        Object host=field(menu,"te");value=call(host,key.equals("count")?"getCount":"isLessThan");
                    }else value=field(menu,key);
                    if(!(value instanceof Number||value instanceof Boolean||value instanceof String))throw new IllegalArgumentException("Primitive native setting required");
                    values.put(key,value);
                }
                write("regression-native-settings.json",values);return null;
            });return true;
        }
        if(kind.equals("regression-ae2-repository-tap")){
            Object w=regressionManagedWindow(num(c,"session",-1)),desktop=h("screen");call(desktop,"bringToFront",w);call(desktop,"setFocusedWindow",w);
            Object definition=field(w,"apiDefinition"),context=call(desktop,"apiContext",w,null,0,0);call(definition,"layout",context);
            Object content=field(field(w,"apiState"),"content");Slot chosen=null;String item=str(c,"item","");var observed=new ArrayList<Object>();
            for(Object value:(Iterable<?>)call(content,"desktopSlots")){
                Slot slot=(Slot)value;boolean visible=Boolean.TRUE.equals(call(content,"desktopSlotVisible",slot));
                var details=new LinkedHashMap<String,Object>(regressionStack(slot.getItem()));details.put("type",slot.getClass().getName());details.put("visible",visible);details.put("x",slot.x);details.put("y",slot.y);observed.add(details);
                if(!slot.getClass().getName().endsWith(".RepoSlot")||!visible)continue;
                if(!item.isEmpty()&&!BuiltInRegistries.ITEM.getKey(slot.getItem().getItem()).toString().equals(item))continue;chosen=slot;break;
            }
            var repositoryState=new LinkedHashMap<String,Object>();repositoryState.put("wanted",item);repositoryState.put("source",content.getClass().getName());repositoryState.put("slots",observed);
            try{
                Object repo=field(content,"repo"),partition=field(repo,"partitionList"),search=field(repo,"search");var entries=new ArrayList<Object>();
                for(Object entry:(Iterable<?>)call(repo,"getAllEntries"))entries.add(Map.of("key",String.valueOf(call(entry,"getWhat")),"stored",call(entry,"getStoredAmount"),"craftable",call(entry,"isCraftable"),
                    "keyType",String.valueOf(call(call(entry,"getWhat"),"getType")),"partitionAllows",partition==null||Boolean.TRUE.equals(call(partition,"isListed",call(entry,"getWhat"))),"searchMatches",call(search,"matches",entry)));
                repositoryState.put("native",Map.of("enabled",call(repo,"isEnabled"),"paused",call(repo,"isPaused"),"search",call(repo,"getSearchString"),"viewSize",call(repo,"size"),"display",String.valueOf(call(content,"getSortDisplay")),
                    "keyTypes",String.valueOf(call(content,"getSortKeyTypes")),"partitionType",partition==null?"none":partition.getClass().getName(),"entries",entries));
            }catch(IllegalArgumentException unsupportedRead){repositoryState.put("nativeReadError",unsupportedRead.toString());}
            write("regression-ae2-repository-state.json",repositoryState);
            if(chosen==null)throw new IllegalStateException("Visible native repository cell missing");
            double x=((Number)call(content,"getGuiLeft")).doubleValue()+chosen.x+8,y=((Number)call(content,"getGuiTop")).doubleValue()+chosen.y+8;int b=regressionButton(c);
            h("mouse","mouseClicked",x,y,b,0D,0D);h("mouse","mouseReleased",x,y,b,0D,0D);return true;
        }
        if(kind.equals("regression-ssn-repository-point")){
            Object w=regressionManagedWindow(num(c,"session",-1)),desktop=h("screen");call(desktop,"bringToFront",w);call(desktop,"setFocusedWindow",w);
            Object definition=field(w,"apiDefinition"),context=call(desktop,"apiContext",w,null,0,0);
            Object nativeScreen=field(field(definition,"initial"),"screen"),network=call(nativeScreen,"getNetwork");
            double sx,sy;
            try{sx=((Number)call(network,"getX")).doubleValue()+8;sy=((Number)call(network,"getY")).doubleValue()+8;}
            catch(IllegalArgumentException legacyPosition){
                // SSN 1.20.1 exposes the same native local origin as public
                // xNetwork/yNetwork fields, without modern getX/getY methods.
                if(!network.getClass().getName().equals("com.lothrazar.storagenetwork.gui.NetworkWidget"))throw legacyPosition;
                sx=((Number)field(network,"xNetwork")).doubleValue()+8;sy=((Number)field(network,"yNetwork")).doubleValue()+8;
            }
            String item=str(c,"item","");
            if(!item.isEmpty()){
                Object chosen=null;for(Object slot:(List<?>)field(network,"slots"))if(BuiltInRegistries.ITEM.getKey(((ItemStack)call(slot,"getStack")).getItem()).toString().equals(item)){chosen=slot;break;}
                if(chosen==null)throw new IllegalStateException("Native SSN repository item is not visible: "+item);
                sx=((Number)field(chosen,"x")).doubleValue()-((Number)field(chosen,"guiLeft")).doubleValue()+8;
                sy=((Number)field(chosen,"y")).doubleValue()-((Number)field(chosen,"guiTop")).doubleValue()+8;
            }
            for(Object region:(List<?>)call(call(definition,"layout"),"regions"))if(Boolean.TRUE.equals(call(region,"interactive"))){
                Object source=call(region,"source");double rx=((Number)call(source,"x")).doubleValue(),ry=((Number)call(source,"y")).doubleValue();
                if(sx<rx||sy<ry||sx>=rx+((Number)call(source,"width")).doubleValue()||sy>=ry+((Number)call(source,"height")).doubleValue())continue;
                double x=((Number)call(definition,"regionX",context,region)).doubleValue()+sx-rx,y=((Number)call(definition,"regionY",context,region)).doubleValue()+sy-ry;
                call(desktop,"mouseMoved",x,y);write("regression-point.json",Map.of("x",x,"y",y));return true;
            }
            throw new IllegalStateException("SSN repository point is outside its interactive extraction");
        }
        if(kind.equals("regression-native-container-ledger")){
            work=server(()->{
                var pos=origin().offset(num(c,"dx",3),num(c,"dy",0),num(c,"dz",0));
                Object host=((net.minecraft.server.level.ServerLevel)h("serverLevel")).getBlockEntity(pos);
                if(!(host instanceof net.minecraft.world.Container container))throw new IllegalStateException("Native storage fixture missing");
                var stacks=new ArrayList<Object>();for(int i=0;i<container.getContainerSize();i++)stacks.add(regressionStack(container.getItem(i)));
                write("regression-native-container-ledger.json",Map.of("type",host.getClass().getName(),"stacks",stacks));return null;
            });return true;
        }
        if(kind.equals("regression-ae2-cell-ledger")){
            work=server(()->{
                var pos=origin().offset(num(c,"dx",-1),0,num(c,"dz",0));Object host=((net.minecraft.server.level.ServerLevel)h("serverLevel")).getBlockEntity(pos);
                if(host==null||!host.getClass().getName().endsWith(".DriveBlockEntity"))throw new IllegalStateException("Native drive fixture missing");
                Object storage=call(host,"getCellInventory",num(c,"cell",0));if(storage==null)throw new IllegalStateException("Native storage cell missing");
                Class<?> keys;try{keys=Class.forName("appeng.api.stacks.AEItemKey");}catch(ClassNotFoundException e){throw new IllegalStateException(e);}
                Object key=call(keys,"of",regressionInputStack(c)),counter;
                // MEStorage's zero-argument enumeration is an inherited public
                // interface default, absent from getDeclaredMethods().
                try{var read=storage.getClass().getMethod("getAvailableStacks");read.setAccessible(true);counter=read.invoke(storage);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
                write("regression-ae2-cell-ledger.json",Map.of("stored",call(counter,"get",key),"powered",call(host,"isPowered"),"key",String.valueOf(key)));return null;
            });return true;
        }
        if(kind.equals("regression-content-tap")){
            Object w=regressionManagedWindow(num(c,"session",-1)),desktop=h("screen");
            call(desktop,"bringToFront",w);call(desktop,"setFocusedWindow",w);
            Object context=call(desktop,"apiContext",w,null,0,0);
            boolean window=str(c,"space","content").equals("window");
            double x=((Number)(window?field(w,"x"):call(context,"contentX"))).doubleValue()+c.get("x").getAsDouble();
            double y=((Number)(window?field(w,"y"):call(context,"contentY"))).doubleValue()+c.get("y").getAsDouble();
            int button=regressionButton(c);h("mouse","mouseClicked",x,y,button,0D,0D);h("mouse","mouseReleased",x,y,button,0D,0D);return true;
        }
        if(kind.equals("regression-catalog")){
            var entities=new ArrayList<String>();for(var type:BuiltInRegistries.ENTITY_TYPE)entities.add(BuiltInRegistries.ENTITY_TYPE.getKey(type).toString());
            var pos=origin();write("regression-catalog.json",Map.of("origin",List.of(pos.getX(),pos.getY(),pos.getZ()),"entities",entities));return true;
        }
        if(kind.equals("regression-management")){
            Object w=regressionManagedWindow(num(c,"session",-1));
            Object handle=call(h("screen"),"apiWindowHandle",w);var available=new LinkedHashMap<String,Boolean>();
            for(var action:com.salts_inventory_update.api.client.desktop.DesktopWindowAction.values())available.put(action.name(),Boolean.TRUE.equals(call(handle,"available",action)));
            write("regression-management.json",Map.of("available",available,"locked",field(w,"locked"),"bounds",Map.of("x",field(w,"x"),"y",field(w,"y"),"width",field(w,"width"),"height",field(w,"height"))));return true;
        }
        if(kind.equals("regression-tap")) {
            if(c.has("session")){
                Object w=regressionManagedWindow(num(c,"session",-1));call(h("screen"),"bringToFront",w);call(h("screen"),"setFocusedWindow",w);
            }
            double x=c.get("x").getAsDouble(),y=c.get("y").getAsDouble();int button=regressionButton(c);
            h("mouse","mouseClicked",x,y,button,0D,0D);h("mouse","mouseReleased",x,y,button,0D,0D);return true;
        }
        if(kind.equals("regression-seed-player")) {
            work=server(()->{for(var el:c.getAsJsonArray("items")){var v=el.getAsJsonObject();player().getInventory().setItem(num(v,"slot",9),regressionInputStack(v));}player().inventoryMenu.broadcastFullState();return null;});return true;
        }
        if(kind.equals("regression-state")||kind.equals("regression-policy")) {
            if(!regressionClickInputs.isEmpty())write("regression-click-inputs.json",regressionClickInputs);
            // Read on the client thread, before the separate server snapshot.
            // An unfinished or discarded intent must remain distinguishable
            // from an executed click with an incorrect authoritative result.
            write("regression-mutation-state.json",regressionMutationState(c));
            var ray=new LinkedHashMap<String,Object>();ray.put("kind",mc.hitResult==null?"none":mc.hitResult.getType().toString());
            ray.put("yaw",mc.player.getYRot());ray.put("pitch",mc.player.getXRot());
            if(mc.hitResult instanceof net.minecraft.world.phys.BlockHitResult hit){ray.put("block",hit.getBlockPos().toString());ray.put("blockState",mc.level.getBlockState(hit.getBlockPos()).toString());ray.put("face",hit.getDirection().toString());ray.put("location",hit.getLocation().toString());}
            write("regression-ray-state.json",ray);
            var visible=regressionNativeVisibility();
            work=server(()->{regressionSnapshot(c,visible);return null;});return true;
        }
        if(kind.equals("regression-bountiful-decree-ledger")){
            work=server(()->{
                var level=(net.minecraft.server.level.ServerLevel)h("serverLevel");
                var board=level.getBlockEntity(origin());
                if(board==null||!board.getClass().getName().equals("io.ejekta.bountiful.content.board.BoardBlockEntity"))
                    throw new IllegalStateException("Native Bountiful board is not at the fixture origin");
                var inventory=(net.minecraft.world.Container)field(board,"decrees");
                var cells=new ArrayList<Object>();
                for(int i=0;i<inventory.getContainerSize();i++){
                    var cell=new LinkedHashMap<String,Object>(regressionStack(inventory.getItem(i)));cell.put("index",i);cells.add(cell);
                }
                write("regression-bountiful-decree-ledger.json",Map.of("blockEntity",board.getClass().getName(),
                    "position",origin().toShortString(),"inventory",inventory.getClass().getName(),"cells",cells));
                return null;
            });return true;
        }
        if(kind.equals("regression-conditioning-button")){
            int button=num(c,"button",-1);
            if(button<0||button>2)throw new IllegalArgumentException("Conditioning module index must be 0..2");
            Object w=regressionManagedWindow(num(c,"session",-1)),desktop=h("screen");
            call(desktop,"bringToFront",w);call(desktop,"setFocusedWindow",w);
            Object definition=field(w,"apiDefinition"),context=call(desktop,"apiContext",w,null,0,0);
            // Semantic native switch bounds from AirConditioningScreen's
            // mouse callback, mapped through the current extraction regions.
            double sx=105.5+18*button,sy=62.5;
            for(Object region:(List<?>)call(call(definition,"layout"),"regions"))if(Boolean.TRUE.equals(call(region,"interactive"))){
                Object source=call(region,"source");double rx=((Number)call(source,"x")).doubleValue(),ry=((Number)call(source,"y")).doubleValue();
                if(sx<rx||sy<ry||sx>=rx+((Number)call(source,"width")).doubleValue()||sy>=ry+((Number)call(source,"height")).doubleValue())continue;
                double x=((Number)call(definition,"regionX",context,region)).doubleValue()+sx-rx,y=((Number)call(definition,"regionY",context,region)).doubleValue()+sy-ry;
                int left=((Number)h("left")).intValue();
                h("mouse","mouseClicked",x,y,left,0D,0D);h("mouse","mouseReleased",x,y,left,0D,0D);return true;
            }
            throw new IllegalStateException("Native conditioning switch lies outside its interactive extraction");
        }
        if(kind.equals("regression-discover-input")) {
            var visible=regressionNativeVisibility();
            work=server(()->{
                Object ps=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());
                var active=(Map<?,?>)field(ps,"sessions");
                var beforeQueries=regressionContentsFingerprint(active);
                try{
                for(var entry:active.entrySet()) {
                    var menu=(AbstractContainerMenu)field(entry.getValue(),"menu");
                    for(int i=0;i<com.salts_inventory_update.api.desktop.DesktopMenuBindings.size(menu);i++) {
                        Slot slot=com.salts_inventory_update.api.desktop.DesktopMenuBindings.slot(menu,i);
                        if(slot==null)continue;
                        if(visible.containsKey(entry.getKey())&&!visible.get(entry.getKey()).contains(i))continue;
                        if(!slot.isActive()||slot.hasItem()||regressionVirtual(menu,slot)
                            ||com.salts_inventory_update.api.desktop.DesktopMenuBindings.isPlayerInventorySlot(menu,slot,player().getInventory()))continue;
                        for(var item:BuiltInRegistries.ITEM) {
                            var request=c.deepCopy();request.addProperty("item",BuiltInRegistries.ITEM.getKey(item).toString());request.addProperty("count",1);
                            if(regressionTomFilter(slot)&&!regressionTomFilterItem(item))continue;
                            ItemStack stack=regressionInputStack(request);if(stack.isEmpty()||!slot.mayPlace(stack))continue;
                            int max=regressionReadOnlyCapacity(menu,slot,stack);
                            if(max<1)continue;
                            write("regression-candidate.json",Map.of("session",entry.getKey(),"slot",i,"item",BuiltInRegistries.ITEM.getKey(item).toString(),"amount",Math.min(7,max)));
                            return null;
                        }
                    }
                }
                write("regression-candidate.json",Map.of("absent",true));return null;
                }finally{
                    var afterQueries=regressionContentsFingerprint(active);
                    if(!beforeQueries.equals(afterQueries)){
                        write("observer-policy-side-effects.json",Map.of("operation","discover-input","sequence",num(c,"sequence",-1),"before",beforeQueries,"after",afterQueries));
                        throw new IllegalStateException("Read-only input discovery changed menu contents; see observer-policy-side-effects.json");
                    }
                }
            });return true;
        }
        if(kind.equals("regression-player-click")) {
            regressionDismissPopup();
            Object w=h("windowKind","INVENTORY");
            if(w==null&&h("windowKind","CREATIVE")==null){InventoryDesktopScreen.openOrToggleInventory(mc);w=h("windowKind","INVENTORY");}
            if(w==null){
                // Creative tools retain their publisher's creative-only policy.
                // Open the real player-inventory tab through its visible tab.
                w=h("windowKind","CREATIVE");if(w==null)throw new IllegalStateException("Player inventory window missing");
                Object desktop=h("screen");call(desktop,"bringToFront",w);call(desktop,"setFocusedWindow",w);
                boolean selected=false;
                for(Object tab:(List<?>)call(desktop,"creativeTabsForPage",w))if(Boolean.TRUE.equals(call(desktop,"isCreativeInventoryTab",tab))){
                    Object rect=call(desktop,"creativeTabRect",w,tab);double x=((Number)call(rect,"x")).doubleValue()+((Number)call(rect,"width")).doubleValue()/2,y=((Number)call(rect,"y")).doubleValue()+((Number)call(rect,"height")).doubleValue()/2;int b=((Number)h("left")).intValue();
                    h("mouse","mouseClicked",x,y,b,0D,0D);h("mouse","mouseReleased",x,y,b,0D,0D);selected=true;break;
                }
                if(!selected)throw new IllegalStateException("Creative player inventory tab missing");
            }
            call(h("screen"),"bringToFront",w);call(h("screen"),"setFocusedWindow",w);
            double[] p=regressionInputPoint(0,num(c,"slot",9),w);
            int button=regressionButton(c);
            regressionRecordClick(c,0,num(c,"slot",9),p);
            h("mouse","mouseClicked",p[0],p[1],button,0D,0D);h("mouse","mouseReleased",p[0],p[1],button,0D,0D);
            return true;
        }
        if(kind.equals("regression-slot-click")||kind.equals("regression-close")||kind.equals("regression-control")) {
            regressionDismissPopup();
            Object session=sessions().stream().filter(s->((Number)call(s,"sessionId")).intValue()==num(c,"session",-1)).findFirst().orElse(null);
            if(session!=null)set(H,"activeSession",session);Object w=kind.equals("regression-slot-click")?h("windowSession",num(c,"session",-1)):regressionManagedWindow(num(c,"session",-1));
            if(w==null&&kind.equals("regression-slot-click"))w=h("windowKind","CHARACTER");
            call(h("screen"),"bringToFront",w);call(h("screen"),"setFocusedWindow",w);
            if(kind.equals("regression-slot-click")){
                double[] p=regressionInputPoint(num(c,"session",-1),num(c,"slot",0),w);int button=regressionButton(c);
                regressionRecordClick(c,num(c,"session",-1),num(c,"slot",0),p);
                h("mouse","mouseClicked",p[0],p[1],button,0D,0D);h("mouse","mouseReleased",p[0],p[1],button,0D,0D);
            }
            else {h("clickWindowControl",w,kind.equals("regression-close")?"CLOSE":str(c,"control","SORT_INVENTORY"));regressionDismissPopup();}
            return true;
        }
        if(kind.equals("regression-slot-drag")){
            regressionDismissPopup();
            int sid=num(c,"session",-1);Object session=sessions().stream().filter(s->((Number)call(s,"sessionId")).intValue()==sid).findFirst().orElseThrow();
            set(H,"activeSession",session);Object w=h("windowSession",sid),desktop=h("screen");call(desktop,"bringToFront",w);call(desktop,"setFocusedWindow",w);
            var cells=c.getAsJsonArray("slots");if(cells.size()<2)throw new IllegalArgumentException("Drag requires at least two actual cells");
            double[] first=regressionInputPoint(sid,cells.get(0).getAsInt(),w),last=first;int b=regressionButton(c);
            h("mouse","mouseClicked",first[0],first[1],b,0D,0D);
            for(int i=1;i<cells.size();i++){
                double[] point=regressionInputPoint(sid,cells.get(i).getAsInt(),w);
                h("mouse","mouseDragged",point[0],point[1],b,point[0]-last[0],point[1]-last[1]);last=point;
            }
            h("mouse","mouseReleased",last[0],last[1],b,0D,0D);return true;
        }
        if(kind.equals("regression-move")) {
            regressionDismissPopup();
            Object w=regressionManagedWindow(num(c,"session",-1));
            call(h("screen"),"bringToFront",w);call(h("screen"),"setFocusedWindow",w);
            double x=((Number)field(w,"x")).doubleValue()+8,y=((Number)field(w,"y")).doubleValue()+7;
            double dx=num(c,"dx",-50),dy=num(c,"dy",30);int button=((Number)h("left")).intValue();
            h("mouse","mouseClicked",x,y,button,0D,0D);h("mouse","mouseDragged",x+dx,y+dy,button,dx,dy);h("mouse","mouseReleased",x+dx,y+dy,button,0D,0D);
            return true;
        }
        if(kind.equals("regression-place")) {
            var pos=regressionBlockPosition(c);
            work=server(()->{
                Object block=call(BuiltInRegistries.BLOCK,"get",regressionIdentifier(str(c,"block","minecraft:chest")));
                if(block instanceof java.util.Optional<?> entry)block=entry.orElseThrow();
                if(block instanceof net.minecraft.core.Holder<?> holder)block=holder.value();
                var state=((net.minecraft.world.level.block.Block)block).defaultBlockState();
                var world=(net.minecraft.server.level.ServerLevel)h("serverLevel");world.removeBlock(pos,false);world.setBlock(pos,state,3);
                return null;
            });return true;
        }
        if(kind.equals("regression-open")) {
            var hand=net.minecraft.world.InteractionHand.MAIN_HAND;
            if(c.has("emptyHand")&&c.get("emptyHand").getAsBoolean()){
                // Native held menus can require the exact host stack to stay
                // in a hand. Use the already empty off hand for a block.
                if(!mc.player.getMainHandItem().isEmpty()&&!(c.has("allowHeldMainHand")&&c.get("allowHeldMainHand").getAsBoolean())){
                    if(!mc.player.getOffhandItem().isEmpty())throw new IllegalStateException("The second block interaction requires an empty hand");
                    hand=net.minecraft.world.InteractionHand.OFF_HAND;
                }
            }
            var pos=regressionBlockPosition(c);
            boolean shift=c.has("shift")&&c.get("shift").getAsBoolean();mc.player.setShiftKeyDown(shift);
            work=server(()->{player().setShiftKeyDown(shift);return null;});
            var result=mc.gameMode.useItemOn(mc.player,hand,
                new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos).add(0,0.5,0),net.minecraft.core.Direction.UP,pos,false));
            write("regression-block-use.json",Map.of("result",String.valueOf(result),"hand",hand.toString(),"held",regressionStack(mc.player.getItemInHand(hand)),
                "position",pos.toShortString(),"clientBlock",mc.level.getBlockState(pos).toString(),"clientAbove",mc.level.getBlockState(pos.above()).toString()));
            work=server(()->{
                var level=(net.minecraft.server.level.ServerLevel)h("serverLevel");
                write("regression-server-block-use.json",Map.of("position",pos.toShortString(),"block",level.getBlockState(pos).toString(),"above",level.getBlockState(pos.above()).toString(),
                    "held",regressionStack(player().getMainHandItem()),"sneaking",player().isShiftKeyDown(),"menuClass",player().containerMenu.getClass().getName()));return null;
            });
            return true;
        }
        throw new IllegalArgumentException("Unknown regression command "+kind);
    }
    static Object regressionIdentifier(String text) {
        try{return call(Class.forName("net.minecraft.resources.Identifier"),"parse",text);}
        catch(ClassNotFoundException absent){try{
            Class<?> type=Class.forName("net.minecraft.resources.ResourceLocation");
            try{return call(type,"parse",text);}catch(IllegalArgumentException legacy){return type.getConstructor(String.class).newInstance(text);}
        }catch(ReflectiveOperationException error){throw new IllegalStateException(error);}}
    }
    static void regressionCharacterInfo(){
        Object w=h("windowKind","CHARACTER"),desktop=h("screen");if(w==null)throw new IllegalStateException("Character did not open");
        Object context=call(desktop,"apiContext",w,null,0,0);var attachments=new ArrayList<Object>();
        for(Object a:(List<?>)field(w,"builtinAttachments"))attachments.add(Map.of("type",a.getClass().getName(),"regions",String.valueOf(call(a,"regions",context))));
        write("regression-character.json",Map.of("instance",String.valueOf(field(w,"apiInstanceId")),"bounds",Map.of("x",field(w,"x"),"y",field(w,"y"),"width",field(w,"width"),"height",field(w,"height")),"attachments",attachments));
    }
    static int regressionButton(JsonObject c) {
        int left=((Number)h("left")).intValue();return num(c,"button",0)==1?(left==1?3:1):left;
    }
    static net.minecraft.core.BlockPos regressionBlockPosition(JsonObject c){
        // Entity fixtures need not be near the block-atlas origin. Keep the
        // second source within normal interaction reach without moving away
        // from the first menu's host or bypassing the native use packet.
        var base=c.has("nearPlayer")&&c.get("nearPlayer").getAsBoolean()?mc.player.blockPosition():origin();
        return base.offset(num(c,"dx",2),num(c,"dy",0),num(c,"dz",0));
    }
    static void regressionDismissPopup(){
        Object desktop=h("screen");if(desktop==null||field(desktop,"popupWindow")==null)return;
        // Sorting/locking from the compact controls intentionally leaves this
        // menu open. Dismiss it with real Escape before testing underlying cells.
        call(desktop,"keyPressed",new net.minecraft.client.input.KeyEvent(41,0,0));
        if(field(desktop,"popupWindow")!=null)throw new IllegalStateException("Core control popup did not dismiss on Escape");
    }
    static final List<Object> regressionClickInputs=new ArrayList<>();
    static Map<String,Object> regressionMutationState(JsonObject command){
        var row=new LinkedHashMap<String,Object>();
        row.put("sequence",num(command,"sequence",-1));
        row.put("pending",com.salts_inventory_update.client.DesktopContainerClient.hasPendingSlotMutations());
        row.put("discardEpoch",com.salts_inventory_update.client.DesktopContainerClient.slotMutationDiscardEpoch());
        row.put("clientCarried",regressionStack(mc.player.inventoryMenu.getCarried()));
        return row;
    }
    static void regressionRecordClick(JsonObject command,int session,int slot,double[] point){
        // Buffer only; disk I/O happens at the later observation barrier.
        Object hit=call(h("screen"),"slotAt",point[0],point[1]);var row=new LinkedHashMap<String,Object>();
        row.put("sequence",num(command,"sequence",-1));row.put("requestedSession",session);row.put("requestedSlot",slot);
        row.put("x",point[0]);row.put("y",point[1]);row.put("clientPlayerStateId",mc.player.inventoryMenu.getStateId());
        row.put("mutationState",regressionMutationState(command));
        row.put("hitSession",hit==null?-1:call(hit,"sessionId"));row.put("hitSlot",hit==null?-1:call(hit,"slotId"));
        regressionClickInputs.add(row);if(regressionClickInputs.size()>16)regressionClickInputs.remove(0);
    }
    static final Map<String,double[]> regressionInputPoints=new HashMap<>();
    static double[] regressionRememberPoint(int id,int index,Object window,double[] point){
        regressionInputPoints.put(id+":"+index,new double[]{point[0],point[1],((Number)field(window,"x")).doubleValue(),((Number)field(window,"y")).doubleValue(),((Number)field(window,"width")).doubleValue(),((Number)field(window,"height")).doubleValue()});return point;
    }
    static double[] regressionInputPoint(int id,int index,Object window) {
        double[] remembered=regressionInputPoints.get(id+":"+index);
        if(remembered!=null&&remembered[4]==((Number)field(window,"width")).doubleValue()&&remembered[5]==((Number)field(window,"height")).doubleValue()){
            double x=remembered[0]+((Number)field(window,"x")).doubleValue()-remembered[2],y=remembered[1]+((Number)field(window,"y")).doubleValue()-remembered[3];
            Object hit=call(h("screen"),"slotAt",x,y);
            if(hit!=null&&((Number)call(hit,"sessionId")).intValue()==id&&((Number)call(hit,"slotId")).intValue()==index)return new double[]{x,y};
        }
        // Player-menu slots 36..44 are the detached HUD hotbar, not slots
        // 0..8 (which are crafting/armor). Use its actual rendered geometry.
        if (id == 0 && index >= 36 && index <= 44) {
            Object layout=call(h("screen"),"detachedHudLayout");
            for(Object geometry:(List<?>)call(layout,"slots")) {
                if(!call(geometry,"kind").toString().equals("HOTBAR")
                        || ((Number)call(geometry,"index")).intValue()!=index-36
                        || !Boolean.TRUE.equals(call(geometry,"visible"))) continue;
                Object transform=call(geometry,"transform");
                double scale=((Number)call(transform,"scale")).doubleValue();
                double x=((Number)call(transform,"x")).doubleValue()+8*scale;
                double y=((Number)call(transform,"y")).doubleValue()+8*scale;
                Object hit=call(h("screen"),"slotAt",x,y);
                if(hit!=null&&((Number)call(hit,"sessionId")).intValue()==id
                        &&((Number)call(hit,"slotId")).intValue()==index)
                    return regressionRememberPoint(id,index,window,new double[]{x,y});
            }
        }
        // Start at the complete native rectangle used for painting, and verify
        // the desktop hit agrees. Keep the legacy search for custom surfaces.
        try {
            Object definition=field(window,"apiDefinition");
            if(definition!=null&&definition.getClass().getName().endsWith(".AEWindow")){
                Object context=call(h("screen"),"apiContext",window,null,0,0);call(definition,"layout",context);
                Object content=field(field(window,"apiState"),"content");var menu=(AbstractContainerMenu)call(field(window,"session"),"menu");
                if(index>=0&&index<menu.slots.size()){
                    Slot cell=menu.slots.get(index);
                    if(Boolean.TRUE.equals(call(content,"desktopSlotVisible",cell))){
                        double x=((Number)call(content,"getGuiLeft")).doubleValue()+cell.x+8,y=((Number)call(content,"getGuiTop")).doubleValue()+cell.y+8;
                        Object hit=call(h("screen"),"slotAt",x,y);
                        if(hit!=null&&((Number)call(hit,"sessionId")).intValue()==id&&((Number)call(hit,"slotId")).intValue()==index)return regressionRememberPoint(id,index,window,new double[]{x,y});
                    }
                }
            }
            if(definition!=null&&definition.getClass().getName().endsWith(".NativeTemplateWindow")){
                Object context=call(h("screen"),"apiContext",window,null,0,0);
                Object layout=call(definition,"layout"),view=field(definition,"view");
                for(Object cell:(List<?>)call(view,"slots"))if(((Number)call(cell,"menuSlotId")).intValue()==index){
                    Object bounds=call(cell,"bounds");
                    for(Object region:(List<?>)call(layout,"regions"))if(Boolean.TRUE.equals(call(region,"interactive"))&&Boolean.TRUE.equals(call(region,"covers",bounds))){
                        Object source=call(region,"source");
                        double x=((Number)call(definition,"regionX",context,region)).doubleValue()+((Number)call(bounds,"x")).doubleValue()-((Number)call(source,"x")).doubleValue()+8;
                        double y=((Number)call(definition,"regionY",context,region)).doubleValue()+((Number)call(bounds,"y")).doubleValue()-((Number)call(source,"y")).doubleValue()+8;
                        Object hit=call(h("screen"),"slotAt",x,y);
                        if(hit!=null&&((Number)call(hit,"sessionId")).intValue()==id&&((Number)call(hit,"slotId")).intValue()==index)return regressionRememberPoint(id,index,window,new double[]{x,y});
                    }
                }
            }
        }catch(IllegalArgumentException|IllegalStateException unavailable){ /* Custom ABI: use normal hit lookup. */ }
        try{return regressionRememberPoint(id,index,window,(double[])h("slotPoint",id,index,window));}catch(IllegalStateException missing){
            Object definition=field(window,"apiDefinition");
            if(definition!=null&&definition.getClass().getName().endsWith(".SophisticatedHostedScreenDefinition")){
                Object state=field(window,"apiState");
                for(int step=0;step<2048;step++){
                    Object layout=field(state,"layout");if(layout==null)break;
                    int maximum=((Number)call(layout,"maxScroll")).intValue();if(maximum<=0||step>maximum+1)break;
                    int min=Integer.MAX_VALUE,max=-1;boolean visible=false;
                    for(Object placed:(List<?>)call(layout,"slots")){
                        if(!call(placed,"group").toString().equals("MAIN"))continue;
                        int slot=((Slot)call(placed,"slot")).index;min=Math.min(min,slot);max=Math.max(max,slot);visible|=slot==index;
                    }
                    if(visible){
                        double[] point=(double[])h("slotPoint",id,index,window);
                        write("regression-scroll-evidence.json",Map.of("session",id,"slot",index,"wheel_events",step,"input","Real mouse wheel callbacks; authoritative slot hit verified after scrolling"));
                        return regressionRememberPoint(id,index,window,point);
                    }
                    if(max<0)break;
                    Object grid=call(layout,"grid");double x=((Number)call(grid,"getX")).doubleValue()+((Number)call(grid,"getWidth")).doubleValue()/2;
                    double y=((Number)call(grid,"getY")).doubleValue()+((Number)call(grid,"getHeight")).doubleValue()/2;
                    h("scroll",x,y,index<min?1D:-1D);
                }
            }
            for(Object other:(List<?>)field(h("screen"),"windows")){
                Object session=field(other,"session");if(session==null||((Number)call(session,"sessionId")).intValue()!=id||other==window)continue;
                try{return regressionRememberPoint(id,index,window,(double[])h("slotPoint",id,index,other));}catch(IllegalStateException absent){}
            }
            throw missing;
        }
    }
    static Object regressionManagedWindow(int id){
        Object desktop=h("screen");
        Object source=h("windowSession",id),nativeMenu=call(field(source,"session"),"menu");
        // Aircraft wings deliberately own separate sessions but no movement or
        // close controls. Its public native vehicle identity joins the dock;
        // exercise management through the MAIN pane as the player must.
        if(nativeMenu.getClass().getName().equals("com.salts_inventory_update.compat.aircraft.AircraftDesktopMenu")){
            Object vehicle=field(nativeMenu,"vehicle");
            for(Object candidate:(List<?>)field(desktop,"windows")){
                Object s=field(candidate,"session");if(s==null)continue;Object model=call(s,"menu");
                if(model.getClass()==nativeMenu.getClass()&&field(model,"vehicle")==vehicle&&field(model,"zone").toString().equals("MAIN"))return candidate;
            }
        }
        for(Object w:(List<?>)field(desktop,"windows")){
            Object session=field(w,"session");if(session==null||((Number)call(session,"sessionId")).intValue()!=id)continue;
            Object handle=call(desktop,"apiWindowHandle",w);
            for(var action:List.of(com.salts_inventory_update.api.client.desktop.DesktopWindowAction.LOCK,
                com.salts_inventory_update.api.client.desktop.DesktopWindowAction.UNLOCK,com.salts_inventory_update.api.client.desktop.DesktopWindowAction.USER_CLOSE))
                if(Boolean.TRUE.equals(call(handle,"available",action)))return w;
        }
        return h("windowSession",id);
    }
    static Map<Integer,Set<Integer>> regressionNativeVisibility(){
        var result=new HashMap<Integer,Set<Integer>>();Object desktop=h("screen");if(desktop==null)return result;
        for(Object window:(List<?>)field(desktop,"windows")){
            Object definition=field(window,"apiDefinition"),session=field(window,"session");
            if(session==null||definition==null)continue;
            if(definition.getClass().getName().endsWith(".NativeTemplateWindow")){
                int id=((Number)call(session,"sessionId")).intValue();var visible=result.computeIfAbsent(id,k->new HashSet<>());
                Object context=call(desktop,"apiContext",window,null,0,0),layout=call(definition,"layout"),view=field(definition,"view");
                for(Object cell:(List<?>)call(view,"slots")){
                    int index=((Number)call(cell,"menuSlotId")).intValue();Object bounds=call(cell,"bounds");
                    for(Object region:(List<?>)call(layout,"regions"))if(Boolean.TRUE.equals(call(region,"interactive"))&&Boolean.TRUE.equals(call(region,"covers",bounds))){
                        Object source=call(region,"source");
                        double x=((Number)call(definition,"regionX",context,region)).doubleValue()+((Number)call(bounds,"x")).doubleValue()-((Number)call(source,"x")).doubleValue()+8;
                        double y=((Number)call(definition,"regionY",context,region)).doubleValue()+((Number)call(bounds,"y")).doubleValue()-((Number)call(source,"y")).doubleValue()+8;
                        Object hit=call(definition,"slotAt",context,x,y);
                        // Create toolbox compartments can have four physical
                        // backing slots at one interactive summary position.
                        if(hit!=null&&((Number)call(hit,"menuSlotId")).intValue()==index)visible.add(index);
                    }
                }
                continue;
            }
            if(!definition.getClass().getName().endsWith(".AEWindow"))continue;
            int id=((Number)call(session,"sessionId")).intValue();var visible=result.computeIfAbsent(id,k->new HashSet<>());
            Object content=field(field(window,"apiState"),"content");var menu=(AbstractContainerMenu)call(session,"menu");
            for(Object value:(Iterable<?>)call(content,"desktopSlots")){
                // Repository display cells reuse logical indices such as zero;
                // only actual publisher-menu Slot objects are physical inputs.
                Slot slot=(Slot)value;int index=-1;for(int i=0;i<menu.slots.size();i++)if(menu.slots.get(i)==slot){index=i;break;}
                if(index>=0&&Boolean.TRUE.equals(call(content,"desktopSlotVisible",slot)))visible.add(index);
            }
        }
        return result;
    }
    // Read the actual vanilla synchronization caches. Never refresh, replace or
    // acknowledge them: this diagnostic must retain the observed mismatch.
    static Map<String,Object> regressionMenuSynchronization(AbstractContainerMenu menu){
        var caches=new ArrayList<Object>();
        for(var f:AbstractContainerMenu.class.getDeclaredFields()){
            if(java.lang.reflect.Modifier.isStatic(f.getModifiers())||!java.util.List.class.isAssignableFrom(f.getType()))continue;
            try{
                f.setAccessible(true);Object value=f.get(menu);
                if(!(value instanceof List<?> cache)||cache.size()!=menu.slots.size()||cache.stream().anyMatch(v->!(v instanceof ItemStack)))continue;
                var different=new ArrayList<Object>();
                for(int i=0;i<cache.size();i++){
                    ItemStack live=menu.slots.get(i).getItem(),saved=(ItemStack)cache.get(i);
                    if(!ItemStack.matches(live,saved))different.add(Map.of("index",i,"live",regressionStack(live),"cached",regressionStack(saved),
                        "liveEmpty",live.isEmpty(),"cachedEmpty",saved.isEmpty(),"liveIsEmptySingleton",live==ItemStack.EMPTY,"cachedIsEmptySingleton",saved==ItemStack.EMPTY));
                }
                caches.add(Map.of("field",f.getName(),"different",different));
            }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        return Map.of("stateId",menu.getStateId(),"caches",caches,"emptySingleton",regressionStack(ItemStack.EMPTY));
    }
    static void regressionSnapshot(JsonObject c,Map<Integer,Set<Integer>> visible) {
        Object ps=call(com.salts_inventory_update.server.DesktopContainerSessions.class,"sessions",player());
        var active=(Map<?,?>)field(ps,"sessions");
        var beforeQueries=regressionContentsFingerprint(active);
        var menus=new ArrayList<Object>();var physical=new LinkedHashMap<String,Object>();var owners=new java.util.IdentityHashMap<Object,Set<Integer>>();
        for(var entry:active.entrySet()) {
            var menu=(AbstractContainerMenu)field(entry.getValue(),"menu");
            var cells=new ArrayList<Object>();
            for(int i=0;i<com.salts_inventory_update.api.desktop.DesktopMenuBindings.size(menu);i++) {
                Slot s=com.salts_inventory_update.api.desktop.DesktopMenuBindings.slot(menu,i);
                if(s==null){
                    var missing=new LinkedHashMap<String,Object>(regressionStack(ItemStack.EMPTY));
                    missing.put("index",i);missing.put("player",false);missing.put("virtual",true);missing.put("active",false);missing.put("visible",false);
                    missing.put("policyQueried",false);missing.put("mayPickup",false);missing.put("mayPlace",false);missing.put("type","unbound");missing.put("requiresTomFilter",false);missing.put("max",0);
                    cells.add(missing);continue;
                }
                boolean isPlayer=com.salts_inventory_update.api.desktop.DesktopMenuBindings.isPlayerInventorySlot(menu,s,player().getInventory());
                boolean virtual=regressionVirtual(menu,s);
                var row=new LinkedHashMap<String,Object>(regressionStack(s.getItem()));
                row.put("index",i);row.put("player",isPlayer);row.put("virtual",virtual);row.put("active",s.isActive());
                row.put("visible",!visible.containsKey(entry.getKey())||visible.get(entry.getKey()).contains(i));
                row.put("policyQueried",!virtual);
                row.put("mayPickup",!virtual&&s.isActive()&&s.mayPickup(player()));row.put("mayPlace",!virtual&&s.isActive()&&s.mayPlace(regressionInputStack(c)));row.put("type",s.getClass().getName());row.put("requiresTomFilter",regressionTomFilter(s));
                row.put("max",!virtual&&s.isActive()?regressionReadOnlyCapacity(menu,s,regressionInputStack(c)):0);
                cells.add(row);
                if(!isPlayer&&!virtual&&s.hasItem()){Object owner=com.salts_inventory_update.api.desktop.DesktopMenuBindings.physicalOwner(menu,s);int position=com.salts_inventory_update.api.desktop.DesktopMenuBindings.physicalIndex(menu,s);if(owners.computeIfAbsent(owner,k->new HashSet<>()).add(position))physical.put("cell/"+physical.size(),regressionStack(s.getItem()));}
            }
            var menuRow=new LinkedHashMap<String,Object>();menuRow.put("session",entry.getKey());menuRow.put("type",menu.getClass().getName());menuRow.put("valid",menu.stillValid(player()));menuRow.put("slots",cells);
            menuRow.put("sourceKey",field(entry.getValue(),"sourceKey"));
            if(c.has("syncDiagnostics")&&c.get("syncDiagnostics").getAsBoolean())menuRow.put("synchronization",regressionMenuSynchronization(menu));
            try{var menuType=menu.getType();var name=menuType==null?null:BuiltInRegistries.MENU.getKey(menuType);menuRow.put("menuType",name==null?"none":name.toString());}
            catch(UnsupportedOperationException unregistered){menuRow.put("menuType","none");}
            if(menu.getClass().getName().equals("com.salts_inventory_update.compat.aircraft.AircraftDesktopMenu")){
                var indices=new ArrayList<Integer>();for(Object cell:(List<?>)field(menu,"cells"))indices.add(((Number)field(cell,"nativeIndex")).intValue());
                var original=(AbstractContainerMenu)field(menu,"original");var vehicle=(net.minecraft.world.entity.Entity)field(menu,"vehicle");
                menuRow.put("nativeAdapter",Map.of("vehicle",vehicle.getUUID().toString(),"zone",field(menu,"zone").toString(),
                    "originalMenuClass",original.getClass().getName(),"originalNonplayerSlots",original.slots.size()-36,"nativeSlotIndices",indices));
            }
              if(menu.getClass().getName().equals("net.sweenus.simplyswords.screen.RunicForgeScreenHandler"))
                  menuRow.put("nativeEditingOriginal",regressionStack((ItemStack)field(menu,"originalStack")));
            if(menu.getClass().getName().equals("net.nicguzzo.wands.menues.MagicBagMenu")){
                Object bag=field(menu,"bag");Class<?> item;
                try{item=Class.forName("net.nicguzzo.wands.items.MagicBagItem");}catch(ClassNotFoundException e){throw new IllegalStateException(e);}
                ItemStack sample=(ItemStack)call(item,"getItem",bag,player().level());
                int amount=((Number)call(item,"getTotal",bag)).intValue();
                var balance=new LinkedHashMap<String,Object>(regressionStack(sample));balance.put("count",amount);menuRow.put("nativeBalance",balance);
                if(owners.computeIfAbsent(bag,k->new HashSet<>()).add(-1))physical.put("native-bag/"+entry.getKey(),balance);
            }
            menus.add(menuRow);
        }
        var inventory=new ArrayList<Object>();
        for(int i=0;i<player().getInventory().getContainerSize();i++){
            var row=new LinkedHashMap<String,Object>(regressionStack(player().getInventory().getItem(i)));row.put("index",i);inventory.add(row);
        }
        var state=new LinkedHashMap<String,Object>(Map.of("menus",menus,"physical",physical,"inventory",inventory,
            "cursor",regressionStack(player().inventoryMenu.getCarried()),"candidate",str(c,"item","minecraft:diamond"),"nativeHost",regressionHost(),
            "player",Map.of("x",player().getX(),"y",player().getY(),"z",player().getZ(),"health",player().getHealth(),"alive",player().isAlive()),"gameTime",player().level().getGameTime(),"itemDataFormat","native-item-codec",
            "inputPolicy",Map.of("selectedSourceLocked",com.salts_inventory_update.api.desktop.DesktopItemSources.isSelectedSlotLocked(player()),"mainHand",regressionStack(player().getMainHandItem()),
                "pendingBlockTarget",regressionPendingBlockTarget(),"serverRejectsLockedBlockUse",Arrays.stream(player().connection.getClass().getDeclaredMethods()).anyMatch(m->m.getName().contains("rejectLockedUseOn")))));
        state.put("slotAccessContract","public-desktop-menu-bindings");
        var nativeMenu=player().containerMenu;String nativeMenuType="none";
        try{var type=nativeMenu.getType();if(type!=null)nativeMenuType=String.valueOf(BuiltInRegistries.MENU.getKey(type));}
        catch(UnsupportedOperationException unregistered){}
        state.put("nativePlayerMenu",Map.of("class",nativeMenu.getClass().getName(),"menuType",nativeMenuType,
            "slotCount",nativeMenu.slots.size(),"id",nativeMenu.containerId));
        if(c.has("syncDiagnostics")&&c.get("syncDiagnostics").getAsBoolean())state.put("playerSynchronization",regressionMenuSynchronization(player().inventoryMenu));
        var afterQueries=regressionContentsFingerprint(active);
        if(!beforeQueries.equals(afterQueries)){
            write("observer-policy-side-effects.json",Map.of("sequence",num(c,"sequence",-1),"before",beforeQueries,"after",afterQueries));
            throw new IllegalStateException("Read-only native policy query changed menu contents; see observer-policy-side-effects.json");
        }
        write("regression-state.json",state);
        try(var out=new java.util.zip.GZIPOutputStream(Files.newOutputStream(OUT.resolve("regression-state-history.jsonl.gz"),StandardOpenOption.CREATE,StandardOpenOption.APPEND))){
            out.write((new Gson().toJson(Map.of("sequence",num(c,"sequence",-1),"state",state))+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
    }
    static void regressionSendShift(boolean down){
        mc.player.connection.send(new net.minecraft.network.protocol.game.ServerboundPlayerInputPacket(new net.minecraft.world.entity.player.Input(false,false,false,false,false,down,false)));
    }
    static String regressionPendingBlockTarget(){
        // Production NativeUiReference's field helper only accepts instances.
        // Read this SIU-owned static field directly; its name is not obfuscated.
        try {
            var pending=com.salts_inventory_update.server.DesktopContainerSessions.class.getDeclaredField("PENDING_USE_TARGETS");
            pending.setAccessible(true);var targets=(Map<?,?>)pending.get(null);
            Object value=targets.get(player());if(value==null)value=targets.get(player().connection);return value==null?"none":String.valueOf(value);
        } catch(ReflectiveOperationException failure){throw new IllegalStateException("Cannot inspect the pending native block target",failure);}
    }
    static int regressionReadOnlyCapacity(AbstractContainerMenu menu,Slot slot,ItemStack sample){
        // Traveler's Fabric copy and its Forge SlotItemHandler base clear and restore
        // its backing cell even for a query. That initializes Inventory/Upgrades
        // tags on the held backpack. Read the handler's actual capacity instead;
        // acceptance is still enforced by its native mayPlace and real clicks.
        for(Class<?> type=slot.getClass();type!=null;type=type.getSuperclass()){
            if(menu.getClass().getName().startsWith("com.tiviacz.travelersbackpack.")&&Set.of(
                "com.tiviacz.travelersbackpack.inventory.menu.slot.SlotItemHandler",
                "net.minecraftforge.items.SlotItemHandler","net.neoforged.neoforge.items.SlotItemHandler").contains(type.getName())){
                Object handler=call(slot,"getItemHandler");int index=((Number)field(slot,"index")).intValue();
                return Math.min(sample.getMaxStackSize(),((Number)call(handler,"getSlotLimit",index)).intValue());
            }
        }
        return Math.min(sample.getMaxStackSize(),com.salts_inventory_update.api.desktop.DesktopMenuBindings.quickCraftMaxStackSize(menu,slot,sample));
    }
    static List<Object> regressionContentsFingerprint(Map<?,?> active){
        var values=new ArrayList<Object>();
        for(var entry:active.entrySet()){
            var menu=(AbstractContainerMenu)field(entry.getValue(),"menu");
            for(int i=0;i<com.salts_inventory_update.api.desktop.DesktopMenuBindings.size(menu);i++){
                Slot slot=com.salts_inventory_update.api.desktop.DesktopMenuBindings.slot(menu,i);
                values.add(List.of(entry.getKey(),i,slot==null?"unbound":regressionStack(slot.getItem())));
            }
            values.add(List.of(entry.getKey(),"cursor",regressionStack(menu.getCarried())));
        }
        for(int i=0;i<player().getInventory().getContainerSize();i++)values.add(List.of("player",i,regressionStack(player().getInventory().getItem(i))));
        values.add(List.of("player","cursor",regressionStack(player().inventoryMenu.getCarried())));
        return values;
    }
    static Map<String,Object> regressionHost() {
        var out=new LinkedHashMap<String,Object>();
        var world=(net.minecraft.server.level.ServerLevel)h("serverLevel");
        Object host=world.getBlockEntity(origin());
        out.put("block",world.getBlockState(origin()).toString());
        out.put("blockClass",world.getBlockState(origin()).getBlock().getClass().getName());
        out.put("blockEntityClass",host==null?"none":host.getClass().getName());
        out.put("blockEntityIsMenuProvider",host instanceof net.minecraft.world.MenuProvider);
        if(host!=null&&host.getClass().getName().startsWith("com.tom.trading.tile.VendingMachine")){
            var stock=new LinkedHashMap<String,Object>();
            for(String name:List.of("Config","Inputs","Outputs")){
                var inventory=(net.minecraft.world.Container)call(host,"get"+name);var entries=new ArrayList<Object>();
                for(int i=0;i<inventory.getContainerSize();i++)entries.add(regressionStack(inventory.getItem(i)));
                stock.put(name.toLowerCase(java.util.Locale.ROOT),entries);
            }
            stock.put("creative",call(host,"isCreativeMode"));stock.put("matchNBT",call(host,"getMatchNBT"));
            out.put("tomsVending",stock);
        }
        if(host!=null&&host.getClass().getName().equals("appeng.blockentity.networking.CableBusBlockEntity")){
            var parts=new LinkedHashMap<String,String>();
            for(var side:net.minecraft.core.Direction.values()){
                Object part=call(host,"getPart",side);parts.put(side.toString(),part==null?"none":part.getClass().getName());
            }
            out.put("ae2Parts",parts);
        }
        if(host!=null&&host.getClass().getName().startsWith("moze_intel.projecte.")&&host.getClass().getSimpleName().startsWith("CollectorMK"))
            out.put("projecteCollector",Map.of("storedEmc",call(host,"getStoredEmc"),"itemCharge",call(host,"getItemCharge"),"sunLevel",call(host,"getSunLevel")));
        if(host!=null&&host.getClass().getName().startsWith("moze_intel.projecte.")&&host.getClass().getSimpleName().startsWith("RelayMK"))
            out.put("projecteRelay",Map.of("storedEmc",call(host,"getStoredEmc"),"chargeProportion",call(host,"getItemChargeProportion")));
        if(host!=null&&host.getClass().getName().endsWith(".CrateBlockEntity")&&host.getClass().getName().contains("quark."))
            out.put("quarkCrateTotal",call(host,"getTotal"));
        if(host!=null&&host.getClass().getName().equals("techreborn.blockentity.storage.item.StorageUnitBaseBlockEntity"))
            out.put("techrebornStorageAmount",call(host,"getCurrentCapacity"));
        if(host!=null&&host.getClass().getName().equals("techreborn.blockentity.generator.basic.SolidFuelGeneratorBlockEntity"))
            out.put("techrebornBurn",Map.of("remaining",field(host,"burnTime"),"total",field(host,"totalBurnTime"),"energy",call(host,"getEnergy")));
        if(host!=null&&host.getClass().getName().equals("net.additionz.block.entity.ChunkLoaderEntity"))
            out.put("additionzBurn",Map.of("remaining",call(host,"getBurnTime"),"active",call(host,"isActive"),
                "capacity",call(host,"getMaxChunksLoaded"),"chunks",new ArrayList<>((List<?>)call(host,"getChunkList"))));
        if(host!=null&&List.of("owmii.powah.block.furnator.FurnatorTile","owmii.powah.block.furnator.FurnatorBlockEntity").contains(host.getClass().getName())){
            Object carbon=call(host,"getCarbon"),energy=call(host,"getEnergy");
            out.put("powahBurn",Map.of("remaining",call(carbon,"getTicks"),"total",call(carbon,"getMax"),"energy",call(energy,"getStored"),"burning",call(host,"isBurning")));
        }
        if(host!=null&&List.of("owmii.powah.block.reactor.ReactorTile","owmii.powah.block.reactor.ReactorBlockEntity").contains(host.getClass().getName())){
            Object carbon=field(host,"carbon"),fuel=field(host,"fuel");
            out.put("powahReactor",Map.of("carbon",call(carbon,"getTicks"),"carbonCapacity",call(carbon,"getMax"),
                "fuel",call(fuel,"getTicks"),"running",call(host,"isRunning"),"assembled",field(field(host,"builder"),"built")));
        }
        if(host!=null&&host.getClass().getName().equals("io.github.lucaargolo.seasonsextras.blockentities.AirConditioningBlockEntity")){
            var burns=new ArrayList<Object>();
            for(Object burn:(Object[])field(host,"burnSlots"))burns.add(Map.of("remaining",field(burn,"burnTime"),"total",field(burn,"burnTimeTotal"),"enabled",field(burn,"enabled")));
            out.put("airConditioningBurns",burns);
        }
        // These fallback metrics belong to AE2. Reflecting over unrelated
        // publishers can eagerly link optional APIs (for example Create's
        // ComputerCraft method signatures) that normal gameplay never loads.
        if(host!=null&&host.getClass().getName().startsWith("appeng."))for(String name:List.of("getStoredPower","getStorage","getRequiredPower","getAECurrentPower","getRemainingFuelTicks","getFuelItemFuelTicks","getCurrentFuelTicksPerTick"))try{out.put(name,call(host,name));}catch(IllegalArgumentException absent){}
        return out;
    }

    static Map<String,Object> regressionWidgetRow(int session,Object definition,String type,String message,int x,int y,int width,int height){
        var row=new LinkedHashMap<String,Object>();row.put("source_session",session);row.put("source_definition",definition.getClass().getName());row.put("type",type);row.put("message",message);
        row.put("x",x);row.put("y",y);row.put("width",width);row.put("height",height);row.put("visible",true);row.put("active",true);return row;
    }
    static Map<String,Object> regressionStack(ItemStack s) {return Map.of("item",BuiltInRegistries.ITEM.getKey(s.getItem()).toString(),"count",s.getCount(),"signature",regressionItemSignature(s));}
    static String regressionItemSignature(ItemStack source){
        // Compare persisted values, not component-map iteration order, debug
        // class names or object identities. Normalizing count keeps quantity
        // conservation independent from exact native component preservation.
        if(source.isEmpty())return "empty";
        var stack=source.copy();stack.setCount(1);
        var ops=net.minecraft.resources.RegistryOps.create(com.mojang.serialization.JsonOps.INSTANCE,((net.minecraft.server.level.ServerLevel)h("serverLevel")).registryAccess());
        var value=ItemStack.CODEC.encodeStart(ops,stack).result().orElseThrow(()->new IllegalStateException("Native item codec could not serialize regression item"));
        return new Gson().toJson(regressionCanonicalJson(value));
    }
    static JsonElement regressionCanonicalJson(JsonElement value){
        if(value.isJsonObject()){
            var sorted=new JsonObject();var object=value.getAsJsonObject();
            for(String key:new TreeSet<>(object.keySet()))sorted.add(key,regressionCanonicalJson(object.get(key)));
            return sorted;
        }
        if(value.isJsonArray()){
            var array=new JsonArray();for(JsonElement entry:value.getAsJsonArray())array.add(regressionCanonicalJson(entry));return array;
        }
        return value;
    }

    static void regressionObserve(Map<String,Object> result) {
        try(var out=new java.util.zip.GZIPOutputStream(Files.newOutputStream(OUT.resolve("observations.jsonl.gz"),StandardOpenOption.CREATE,StandardOpenOption.APPEND))){
            out.write((new Gson().toJson(result)+"\n").getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }catch(java.io.IOException e){throw new IllegalStateException(e);}
        history.add(result);if(history.size()>16)history.remove(0);write("observations.json",history);
    }

    // Legacy Tom's phantom entries are native configuration copies, not owned
    // items. FilterSlot, in contrast, owns an IItemFilter but only configures a
    // ghost example when clicked with an ordinary item. Keep those oracles apart.
    static boolean regressionTomClass(Slot slot,String name){for(Class<?> type=slot.getClass();type!=null;type=type.getSuperclass())if(type.getName().startsWith("com.tom.storagemod.")&&type.getSimpleName().equals(name))return true;return false;}
    static boolean regressionTomFilter(Slot slot){return regressionTomClass(slot,"FilterSlot")||regressionTomClass(slot,"ItemFilterSlot");}
    static boolean regressionTomFilterItem(Object item){try{return Class.forName("com.tom.storagemod.item.IItemFilter").isInstance(item);}catch(ClassNotFoundException unavailable){return false;}}
    static boolean regressionVirtual(AbstractContainerMenu menu,Slot slot){return com.salts_inventory_update.api.desktop.DesktopMenuBindings.isVirtualSlot(menu,slot)
        ||menu.getClass().getName().equals("eu.pb4.polymer.core.impl.ui.MicroUi$InternalScreenHandler")
        ||slot.getClass().getName().equals("moze_intel.projecte.gameObjs.container.slots.SlotGhost")
        ||slot.getClass().getName().equals("net.nicguzzo.wands.menues.BagSlot")
        ||(menu.getClass().getName().equals("net.nicguzzo.wands.menues.PaletteMenu")&&!com.salts_inventory_update.api.desktop.DesktopMenuBindings.isPlayerInventorySlot(menu,slot,player().getInventory()))
        ||(menu.getClass().getName().equals("com.ninni.spawn.client.inventory.PigmentShifterMenu")&&field(menu,"resultSlot")==slot)
        ||(menu.getClass().getName().equals("dev.latvian.mods.itemfilters.gui.InventoryFilterMenu")&&menu.slots.indexOf(slot)<((List<?>)field(menu,"filterSlots")).size())
        ||regressionTomClass(slot,"PhantomSlot")||regressionFake(slot);}
    static boolean regressionFake(Slot slot){try{return Boolean.TRUE.equals(call(slot,"isFake"));}catch(IllegalArgumentException unavailable){return false;}}

    static ItemStack regressionInputStack(JsonObject c) {try {
        ItemStack s=stack(c);
        if(c.has("native_decree")&&c.get("native_decree").getAsBoolean()){
            // A blank /give decree has no native decree identity. Construct a
            // valid player-fixture item through the publisher's public factory.
            // This never writes a publisher menu or bypasses a menu callback.
            s=(ItemStack)call(Class.forName("io.ejekta.bountiful.content.item.DecreeItem").getField("Companion").get(null),"createWithAllDecrees");
            s.setCount(num(c,"count",1));
        }
        if(c.has("snbt")){s.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(net.minecraft.nbt.TagParser.parseCompoundFully(c.get("snbt").getAsString())));}return s;
    }catch(Exception e){throw new IllegalArgumentException("Invalid regression stack",e);}}



    // Excluded network fixture: delay the real local connection's outbound
    // packets in FIFO order, through Netty's ordinary event loop.
    static final QaPacketDelay qaPacketDelay=new QaPacketDelay();
    static final class QaPacketDelay extends io.netty.channel.ChannelDuplexHandler {
        volatile int millis,jitter;
        final java.util.concurrent.atomic.AtomicInteger pending=new java.util.concurrent.atomic.AtomicInteger();
        final java.util.concurrent.atomic.AtomicLong submitted=new java.util.concurrent.atomic.AtomicLong();
        final java.util.concurrent.atomic.AtomicLong delivered=new java.util.concurrent.atomic.AtomicLong();
        long lastDue;
        final java.util.ArrayDeque<Delayed> packets=new java.util.ArrayDeque<>();
        static final class Delayed {final Object message;final io.netty.channel.ChannelPromise promise;boolean ready;
            Delayed(Object message,io.netty.channel.ChannelPromise promise){this.message=message;this.promise=promise;}}
        public void write(io.netty.channel.ChannelHandlerContext ctx,Object message,io.netty.channel.ChannelPromise promise){
            long ordinal=submitted.getAndIncrement();
            long delay=Math.max(0,millis+(jitter==0?0:(ordinal%3-1)*jitter));
            if(delay==0&&pending.get()==0){ctx.write(message,promise);delivered.incrementAndGet();return;}
            long now=System.nanoTime(),due=Math.max(now+delay*1_000_000L,lastDue+1);lastDue=due;pending.incrementAndGet();
            Delayed packet=new Delayed(message,promise);packets.addLast(packet);
            ctx.executor().schedule(()->{
                packet.ready=true;
                // Timer invocation overhead can reverse near-equal deadlines.
                // Readiness may arrive out of order; native writes must not.
                while(!packets.isEmpty()&&packets.peekFirst().ready){
                    Delayed next=packets.removeFirst();
                    try{
                        if(ctx.channel().isActive())ctx.writeAndFlush(next.message,next.promise);
                        else {io.netty.util.ReferenceCountUtil.release(next.message);next.promise.tryFailure(new IllegalStateException("QA connection closed before delayed packet delivery"));}
                    }finally{pending.decrementAndGet();delivered.incrementAndGet();}
                }
            },Math.max(0,due-now),java.util.concurrent.TimeUnit.NANOSECONDS);
        }
    }
    static void qaNetworkDelay(JsonObject c){
        int delay=num(c,"millis",0),jitter=num(c,"jitter",0);
        if(delay<0||delay>200||jitter<0||jitter>100)throw new IllegalArgumentException("Bounded QA latency is 0..200ms plus 0..100ms jitter");
        Object connection=mc.getConnection().getConnection();io.netty.channel.Channel channel=null;
        for(Class<?> type=connection.getClass();type!=null;type=type.getSuperclass())for(var f:type.getDeclaredFields())if(io.netty.channel.Channel.class.isAssignableFrom(f.getType())){
            try{f.setAccessible(true);channel=(io.netty.channel.Channel)f.get(connection);}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        }
        if(channel==null)throw new IllegalStateException("Native connection has no live Netty channel");
        io.netty.channel.Channel live=channel;
        live.eventLoop().submit(()->{
            if(live.pipeline().get("siu_qa_packet_delay")==null)live.pipeline().addBefore("packet_handler","siu_qa_packet_delay",qaPacketDelay);
            if(qaPacketDelay.pending.get()!=0)throw new IllegalStateException("Do not reconfigure a nonempty delay queue");
            qaPacketDelay.millis=delay;qaPacketDelay.jitter=jitter;
        }).syncUninterruptibly();
        write("regression-network-delay.json",Map.of("millis",delay,"jitter",jitter,"scope","Actual native client outbound packets, FIFO Netty scheduling. No packet contents, server tick rate or UI logic changed."));
    }
    static void qaNetworkStatus(){
        Map<?,?> probe;
        try{probe=(Map<?,?>)Class.forName("siu.qa.clickprobe.UiClickProbeRuntime",true,null).getMethod("fence").invoke(null);}
        catch(ReflectiveOperationException error){throw new IllegalStateException("Latency tests require the excluded click probe",error);}
        write("regression-network-status.json",Map.of("outboundPending",qaPacketDelay.pending.get(),"outboundSubmitted",qaPacketDelay.submitted.get(),"outboundDelivered",qaPacketDelay.delivered.get(),
            "clicks",probe,"pendingGesture",com.salts_inventory_update.client.DesktopContainerClient.hasPendingSlotMutations()));
    }

}
