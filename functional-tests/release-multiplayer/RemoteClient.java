package com.siu_release_remote;

import com.google.gson.*;
import com.mojang.serialization.JsonOps;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.client.DesktopContainerClient;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.client.SaltsInventoryConfig;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import com.salts_inventory_update.network.DesktopPackets;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.*;
import java.util.*;

/** Excluded client-only driver. All world setup/authoritative reads use vanilla RCON. */
public final class RemoteClient implements ClientModInitializer {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path OUT = Path.of(System.getenv("SIU_REMOTE_OUTPUT"));
    private static Minecraft mc;
    private static int age, stage, settle;
    private static long last = -1;
    private static JsonObject pending;
    private static boolean stopped;
    @Override public void onInitializeClient() { ClientTickEvents.END_CLIENT_TICK.register(client -> tick()); }
    private static void write(String name, Object value) {
        try { AtomicObservation.write(OUT.resolve(name),JSON.toJson(value));
        } catch(Exception error) { throw new IllegalStateException(error); }
    }
    private static int number(JsonObject value, String name, int fallback) { return value.has(name)?value.get(name).getAsInt():fallback; }
    private static String text(JsonObject value, String name, String fallback) { return value.has(name)?value.get(name).getAsString():fallback; }
    private static InventoryDesktopScreen desktop() { return InventoryDesktopScreen.current(mc); }
    private static Object field(Object value,String name) {
        for(Class<?> type=value instanceof Class<?> c?c:value.getClass();type!=null;type=type.getSuperclass())try{
            var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(value instanceof Class<?>?null:value);
        }catch(NoSuchFieldException absent){}catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        throw new IllegalStateException("Missing observed field "+name);
    }
    private static Object call(Object value,String name,Object...args) {
        for(Class<?> type=value.getClass();type!=null;type=type.getSuperclass())for(var method:type.getDeclaredMethods())
            if(method.getName().equals(name)&&method.getParameterCount()==args.length)try{
                method.setAccessible(true);return method.invoke(value,args);
            }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
        throw new IllegalStateException("Missing observed method "+name);
    }
    private static DesktopWorkspace workspace() { return Objects.requireNonNull(desktop(), "No desktop").workspaceAccess(); }
    private static DesktopWorkspaceWindow window(JsonObject command) {
        UUID id = UUID.fromString(command.get("window").getAsString());
        return workspace().windows().stream().filter(w -> w.handle().instanceId().equals(id)).findFirst().orElseThrow();
    }
    private static Map<String,Object> stack(ItemStack value) {
        return Map.of("item",BuiltInRegistries.ITEM.getKey(value.getItem()).toString(),"count",value.getCount(),
            "signature",value.isEmpty()?"empty":ItemStack.CODEC.encodeStart(RegistryOps.create(JsonOps.INSTANCE,mc.level.registryAccess()),value.copyWithCount(1)).getOrThrow().toString());
    }
    private static Object state() {
        var result = new LinkedHashMap<String,Object>();
        result.put("dedicated",mc.getSingleplayerServer()==null);
        result.put("connected",mc.player!=null && mc.level!=null);
        result.put("serverSessions",DesktopContainerClient.canUseServerSessions());
        result.put("connectionNonce",((com.salts_inventory_update.protocol.DesktopConnectionState)field(DesktopContainerClient.class,"CONNECTION")).connectionNonce());
        result.put("screen",mc.gui.screen()==null?"none":mc.gui.screen().getClass().getName());
        if(mc.player==null || mc.level==null)return result;
        var inventory = new ArrayList<Object>();
        for(int i=0;i<36;i++){var row=new LinkedHashMap<String,Object>(stack(mc.player.getInventory().getItem(i)));row.put("index",i);inventory.add(row);}
        result.put("inventory",inventory);
        result.put("cursor",stack(desktop()==null?mc.player.inventoryMenu.getCarried():workspace().carriedStack()));
        result.put("mutationReady",DesktopClientApi.canDispatchOptimisticMutationNow());
        var windows = new ArrayList<Object>();
        if(desktop()!=null) for(var value:workspace().windows()) {
            var row = new LinkedHashMap<String,Object>();
            row.put("id",value.handle().instanceId().toString());row.put("kind",value.kind().toString());row.put("title",value.title());
            row.put("state",value.handle().state());
            for(Object nativeWindow:(List<?>)field(desktop(),"windows"))if(value.handle().instanceId().equals(field(nativeWindow,"apiInstanceId"))){
                var session=(com.salts_inventory_update.client.DesktopContainerSession)field(nativeWindow,"session");
                if(session!=null){row.put("session",session.sessionId());row.put("sessionToken",session.sessionToken());}
                var controls=new ArrayList<Object>();
                var rectangles=new ArrayList<Object>((List<?>)call(call(desktop(),"titleBarLayout",nativeWindow),"controls"));
                if(field(desktop(),"popupWindow")==nativeWindow)rectangles.addAll((List<?>)call(desktop(),"popupControlRects",nativeWindow,call(desktop(),"popupRect",nativeWindow)));
                for(Object rect:rectangles)
                    controls.add(Map.of("control",call(rect,"control").toString(),"x",call(rect,"x"),"y",call(rect,"y"),"width",call(rect,"width"),"height",call(rect,"height")));
                row.put("controls",controls);
            }
            var cells = new ArrayList<Object>();
            for(var hit:workspace().visibleSlots(value.handle().instanceId())) {
                var cell = new LinkedHashMap<String,Object>();cell.put("menuSlot",hit.menuSlotId());cell.put("x",hit.x());cell.put("y",hit.y());
                if(value.kind()==DesktopWorkspaceWindow.Kind.PLAYER_INVENTORY && hit.menuSlotId()>=0)
                    cell.put("inventorySlot",mc.player.inventoryMenu.getSlot(hit.menuSlotId()).getContainerSlot());
                var nativeSlot=workspace().hoveredSlot(value.handle().instanceId(),hit.x()+8,hit.y()+8);
                if(nativeSlot!=null)cell.put("stack",stack(nativeSlot.getItem()));
                cells.add(cell);
            }
            row.put("slots",cells);windows.add(row);
        }
        result.put("windows",windows);return result;
    }
    private static void action(JsonObject command) {
        switch(text(command,"type","state")) {
            case "state" -> { }
            case "inventory" -> InventoryDesktopScreen.openOrToggleInventory(mc);
            case "close-all" -> InventoryDesktopScreen.permanentlyCloseAllOpenWindows(mc);
            case "use-block" -> {
                mc.gui.setScreen(null);
                var pos=new BlockPos(number(command,"x",8),number(command,"y",65),number(command,"z",8));
                mc.gameMode.useItemOn(mc.player,InteractionHand.MAIN_HAND,
                    new BlockHitResult(new Vec3(pos.getX()+.5,pos.getY()+1,pos.getZ()+.5),Direction.UP,pos,false));
            }
            case "tap" -> {
                var value=window(command);value.handle().focus();double x=command.get("x").getAsDouble(), y=command.get("y").getAsDouble();
                int button=switch(number(command,"button",0)) {
                    case 0 -> com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT;
                    case 1 -> com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_RIGHT;
                    default -> throw new IllegalArgumentException("Unsupported logical mouse button");
                };
                workspace().pointer(value.handle().instanceId(),DesktopWorkspace.PointerAction.MOVE,x,y,0,0,0);
                workspace().pointer(value.handle().instanceId(),DesktopWorkspace.PointerAction.PRESS,x,y,button,0,0);
                workspace().pointer(value.handle().instanceId(),DesktopWorkspace.PointerAction.RELEASE,x,y,button,0,0);
            }
            case "move" -> {
                var handle=window(command).handle();if(handle.state().locked())handle.request(DesktopWindowAction.UNLOCK);
                if(!handle.moveTo(number(command,"x",50),number(command,"y",50)))throw new IllegalStateException("Native move refused");
            }
            case "close" -> {if(!window(command).handle().close())throw new IllegalStateException("Close refused");}
            case "forged-close" -> ClientPlayNetworking.send(new DesktopPackets.DesktopCloseSessionPayload(
                command.get("connectionNonce").getAsLong(),number(command,"session",1),command.get("sessionToken").getAsLong()));
            case "finish" -> {stopped=true;mc.stop();}
            default -> throw new IllegalArgumentException("Unsupported remote QA action");
        }
    }
    private static void tick() {
        if(stopped)return;
        try {
            mc=Minecraft.getInstance();age++;
            if(stage==0) {
                if(age<60 || !(mc.gui.screen() instanceof TitleScreen) || mc.gui.overlay()!=null)return;
                mc.options.pauseOnLostFocus=false;mc.options.renderDistance().set(2);mc.options.simulationDistance().set(5);
                // This test starts after normal first-run onboarding. Set the
                // ordinary profile preferences before connection, since the
                // welcome window can otherwise occupy the initial UI.
                SaltsInventoryConfig.update(c->{c.enableMod=true;c.hasSeenInstructionsWindow=true;c.openUnlocked=true;c.expandableInventory=false;c.returnHotbarToInventory=true;});
                String address=System.getenv("SIU_REMOTE_SERVER");
                ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),
                    new ServerData("Local release QA",address,ServerData.Type.OTHER),false,null);
                stage=1;return;
            }
            if(stage==1) {
                if(age%20==0)write("login-state.json",Map.of("player",mc.player!=null,"level",mc.level!=null,
                    "screen",mc.gui.screen()==null?"none":mc.gui.screen().getClass().getName(),"serverSessions",DesktopContainerClient.canUseServerSessions()));
                if(mc.gui.screen()!=null && mc.gui.screen().getClass().getSimpleName().contains("Disconnected"))throw new IllegalStateException("Remote login rejected");
                if(mc.player==null || mc.level==null || mc.gui.screen()!=null || !DesktopContainerClient.canUseServerSessions())return;
                if(mc.getSingleplayerServer()!=null)throw new IllegalStateException("Expected a real dedicated connection");
                SaltsInventoryConfig.update(c->{c.hasSeenInstructionsWindow=true;c.openUnlocked=true;c.expandableInventory=false;c.returnHotbarToInventory=true;});
                write("ready.json",Map.of("username",mc.player.getGameProfile().name(),"dedicated",true));stage=2;return;
            }
            if(pending==null) {
                Path path=OUT.resolve("command.json");if(!Files.isRegularFile(path))return;
                var command=JsonParser.parseString(Files.readString(path)).getAsJsonObject();long sequence=command.get("sequence").getAsLong();
                if(sequence<=last)return;last=sequence;pending=command;settle=number(command,"ticks",12);action(command);return;
            }
            if(settle-->0)return;
            write("reply.json",Map.of("sequence",last,"state",state()));pending=null;
        } catch(Throwable error) {
            error.printStackTrace();write("failure.json",Map.of("sequence",last,"error",error.toString()));stopped=true;mc.stop();
        }
    }
}
