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
    static int settle;
    static boolean probing,ready,completed;
    static String failure="";
    static final List<Map<String,Object>> history=new ArrayList<>();
    public static void initialize() {
        InWorldGameplayHarness.initialize();
        Deque<?> steps=(Deque<?>)field(H,"STEPS");while(steps.size()>3)steps.removeLast();
        h("add","native-port-observation",(Runnable)()->{
            mc=Minecraft.getInstance();
            SaltsInventoryConfig.update(c->{c.hasSeenInstructionsWindow=true;c.expandableInventory=false;c.enableDetailedConsoleLogs=false;c.forcedContainerWindows.clear();});
            write("ready.json",Map.of("ready",true));ready=true;
        },(BooleanSupplier)EverythingAuditQa::tick,(Runnable)()->{},1);
    }
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
    static Object identifier(String value){
        try{return call(Class.forName("net.minecraft.resources.Identifier"),"parse",value);}
        catch(ClassNotFoundException legacy){try{
            var type=Class.forName("net.minecraft.resources.ResourceLocation");
            try{return call(type,"parse",value);}catch(IllegalArgumentException old){return type.getConstructor(String.class).newInstance(value);}
        }catch(Exception e){throw new IllegalStateException(e);}}
    }
    static Object registry(Object registry,String id){try{return call(registry,"getValue",identifier(id));}catch(IllegalArgumentException old){return call(registry,"get",identifier(id));}}
    static ItemStack stack(JsonObject c){return new ItemStack((net.minecraft.world.item.Item)registry(BuiltInRegistries.ITEM,str(c,"item","minecraft:diamond")),num(c,"count",32));}
    static Map<String,Object> item(ItemStack stack){return Map.of("item",BuiltInRegistries.ITEM.getKey(stack.getItem()).toString(),"count",stack.getCount());}
    static List<Map<String,Object>> contents(AbstractContainerMenu menu){
        var result=new ArrayList<Map<String,Object>>();
        for(int i=0;i<menu.slots.size();i++){var s=menu.getSlot(i);var row=new LinkedHashMap<String,Object>(item(s.getItem()));row.put("index",i);row.put("x",s.x);row.put("y",s.y);row.put("type",s.getClass().getName());result.add(row);}return result;
    }
    static Map<String,Object> state(){
        var out=new LinkedHashMap<String,Object>();Object nativeScreen=h("nativeScreen");out.put("screen",nativeScreen==null?"none":nativeScreen.getClass().getName());out.put("sessions",sessions().size());
        out.put("guiWidth",mc.getWindow().getGuiScaledWidth());out.put("guiHeight",mc.getWindow().getGuiScaledHeight());
        out.put("automaticReady",field(DesktopContainerClient.class,"automaticNativeReady"));
        out.put("advertisedMenus",com.salts_inventory_update.client.automatic.AutomaticNativeWindow.advertisedMenuIds(com.salts_inventory_update.api.client.desktop.DesktopClientApi.supportedMenuIds()));
        out.put("profileMenus",com.salts_inventory_update.client.automatic.NativeProfileFiles.menuIds());
        var windows=new ArrayList<Map<String,Object>>();
        for(var session:sessions()){
            var row=new LinkedHashMap<String,Object>();row.put("sessionId",session.sessionId());row.put("source",session.sourceKey());row.put("slots",contents(session.menu()));
            Object window=h("windowSession",session.sessionId());
            row.put("bounds",Map.of("x",field(window,"x"),"y",field(window,"y"),"width",field(window,"width"),"height",field(window,"height")));
            Object definition=field(window,"apiDefinition");row.put("definition",definition==null?"none":definition.getClass().getName());
            if(definition!=null&&definition.getClass().getSimpleName().equals("NativeTemplateWindow")){row.put("layout",call(definition,"layout").toString());row.put("layoutError",field(definition,"layoutError"));row.put("inspecting",field(definition,"inspecting"));}
            windows.add(row);
        }out.put("windows",windows);out.put("cursor",item(mc.player.inventoryMenu.getCarried()));return out;
    }
    static Map<String,Object> authoritative(){
        var out=new LinkedHashMap<String,Object>();out.put("slots",contents(serverMenu()));out.put("cursor",item(player().inventoryMenu.getCarried()));out.put("menu",serverMenu().getClass().getName());
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
        var hit=new net.minecraft.world.phys.BlockHitResult(net.minecraft.world.phys.Vec3.atCenterOf(pos),net.minecraft.core.Direction.UP,pos,false);
        call(p.gameMode,"useItemOn",p,level,p.getMainHandItem(),net.minecraft.world.InteractionHand.MAIN_HAND,hit);
    }
    static void execute(JsonObject c){
        work=null;failure="";settle=num(c,"ticks",16);select();
        switch(str(c,"type","inspect")){
            case "fixture"->{close();work=server(()->{
                var p=player();p.inventoryMenu.setCarried(ItemStack.EMPTY);p.getInventory().clearContent();
                var block=(net.minecraft.world.level.block.Block)registry(BuiltInRegistries.BLOCK,str(c,"block","netherchested:nether_chest"));
                if(block==net.minecraft.world.level.block.Blocks.AIR)throw new IllegalArgumentException("Missing fixture block");
                level().setBlock(origin(),block.defaultBlockState(),3);return null;
            });}
            case "open"->work=server(()->{open(origin(),c.has("realUse")&&c.get("realUse").getAsBoolean());return null;});
            case "seed"->work=server(()->{serverMenu().getSlot(num(c,"slot",0)).set(stack(c));serverMenu().broadcastFullState();return null;});
            case "click"->{select();h("clickSlot",num(c,"slot",0),button(c));}
            case "store"->h("storeInPlayer");
            case "sort"->{var w=h("windowSession",sessions().get(sessions().size()-1).sessionId());h("clickWindowControl",w,"SORT_INVENTORY");}
            case "move"->{var w=h("windowSession",sessions().get(sessions().size()-1).sessionId());double x=((Number)field(w,"x")).doubleValue()+25,y=((Number)field(w,"y")).doubleValue()+5,dx=num(c,"dx",60),dy=num(c,"dy",40);h("mouse","mouseClicked",x,y,left(),0D,0D);h("mouse","mouseDragged",x+dx,y+dy,left(),dx,dy);h("mouse","mouseReleased",x+dx,y+dy,left(),0D,0D);}
            case "item-use"->work=server(()->{var p=player();p.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,stack(c));p.getMainHandItem().use(level(),p,net.minecraft.world.InteractionHand.MAIN_HAND);return null;});
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
            case "xy-click"->{double x=c.get("x").getAsDouble(),y=c.get("y").getAsDouble();h("mouse","mouseClicked",x,y,left(),0D,0D);h("mouse","mouseReleased",x,y,left(),0D,0D);}
            case "close"->close();
            case "capture","wait","inspect","finish"->{}
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
                pending=c;last=c.get("sequence").getAsLong();probing=false;
                try{execute(c);}catch(Throwable e){failure=e.toString();work=null;settle=3;}return false;
            }
            if(work!=null&&!work.isDone())return false;
            if(work!=null&&work.isCompletedExceptionally()){try{work.join();}catch(Throwable e){failure=e.toString();}work=null;}
            if(settle-->0)return false;
            if(!probing){probing=true;select();work=server(EverythingAuditQa::authoritative);return false;}
            var result=new LinkedHashMap<String,Object>();result.put("sequence",last);result.put("command",pending);result.put("error",failure);result.put("client",state());
            try{result.put("server",work.join());}catch(Throwable e){result.put("probeError",e.toString());}
            if(pending.has("capture"))h("capture",pending.get("capture").getAsString());
            history.add(result);write("observations.json",history);write("reply.json",result);
            completed=str(pending,"type","").equals("finish");pending=null;work=null;return completed;
        }catch(Throwable e){write("driver-error.json",Map.of("sequence",last,"error",e.toString()));pending=null;work=null;return false;}
    }
}
