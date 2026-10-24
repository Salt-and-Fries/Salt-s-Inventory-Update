package com.salts_inventory_update.functionaltest;

import com.google.gson.GsonBuilder;
import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.api.client.desktop.DesktopWorkspaceWindow;
import com.salts_inventory_update.client.DesktopContainerClient;
import com.salts_inventory_update.client.InventoryDesktopScreen;
import com.salts_inventory_update.client.SaltsInventoryConfig;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.item.Items;
import java.nio.file.*;
import java.util.*;

/** Disposable remote-client probe. The dedicated server contains no QA mod or Spatial GUI. */
public final class SpatialServerClientQa {
    private static int tick, phase, entered;
    private static boolean failed;
    private static final List<String> passed=new ArrayList<>();
    private static final Path output=Path.of(System.getenv("SIU_QA_OUTPUT"));
    private static final Minecraft mc=Minecraft.getInstance();
    private static boolean spatial;
    public static void initialize() {
        try { Files.createDirectories(output.resolve("screenshots")); } catch (java.io.IOException e) { throw new IllegalStateException(e); }
        spatial=FabricLoader.getInstance().isModLoaded("spatial-gui");
        SaltsInventoryConfig.update(c->{c.hasSeenInstructionsWindow=true;c.expandableInventory=false;c.returnHotbarToInventory=true;});
        try {
            var cls=InWorldGameplayHarness.class;
            for(var item:Map.of("mc",mc,"output",output).entrySet()) {var f=cls.getDeclaredField(item.getKey());f.setAccessible(true);f.set(null,item.getValue());}
        }catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
        DesktopClientApi.onClientTick(client->run());
    }
    private static Object helper(String name,Class<?>[] types,Object...args) {
        try {var method=InWorldGameplayHarness.class.getDeclaredMethod(name,types);method.setAccessible(true);return method.invoke(null,args);}
        catch(ReflectiveOperationException e){throw new IllegalStateException(e);}
    }
    private static void check(String name,boolean condition) {if(!condition)throw new IllegalStateException(name);passed.add(name);}
    private static void write(String name,Object data) {
        try {Files.createDirectories(output);Files.writeString(output.resolve(name),new GsonBuilder().setPrettyPrinting().create().toJson(data));}
        catch(Exception e){throw new IllegalStateException(e);}
    }
    private static void next() {phase++;entered=tick;}
    private static void click(int slot) {
        Object window=helper("windowKind",new Class<?>[]{String.class},"INVENTORY");
        var point=(double[])helper("slotPoint",new Class<?>[]{int.class,int.class,Object.class},0,slot,window);
        if(spatial)helper("spatialPoint",new Class<?>[]{double.class,double.class},point[0],point[1]);
        else mc.mouseHandler.onMove(mc.getWindow().handle(),point[0]*mc.getWindow().getGuiScale(),point[1]*mc.getWindow().getGuiScale(),0,0);
        mc.mouseHandler.onButton(mc.getWindow().handle(),new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0),1);
        mc.mouseHandler.onButton(mc.getWindow().handle(),new MouseButtonInfo(com.mojang.blaze3d.platform.InputConstants.MOUSE_BUTTON_LEFT,0),0);
    }
    private static void run() {
        if(failed)return;
        tick++;
        try {
            if(tick-entered>1800)throw new IllegalStateException("Remote phase timeout "+phase);
            if(phase==0) {
                if(tick<50 || mc.gui.overlay()!=null)return;
                mc.options.pauseOnLostFocus=false;
                mc.options.renderDistance().set(2);
                String address=System.getenv("SIU_QA_SPATIAL_SERVER");
                ConnectScreen.startConnecting(new TitleScreen(),mc,ServerAddress.parseString(address),
                    new ServerData("Local Core-only QA",address,ServerData.Type.OTHER),false,null);
                next();
            } else if(phase==1) {
                if(mc.gui.screen()!=null&&mc.gui.screen().getClass().getSimpleName().contains("Disconnected"))throw new IllegalStateException("Disconnected before Core handshake");
                if(mc.player==null||!DesktopContainerClient.canUseServerSessions())return;
                check("remote-core-handshake",true);check("dedicated-not-integrated",mc.getSingleplayerServer()==null);
                SaltsInventoryConfig.update(c->{c.enableMod=true;c.hasSeenInstructionsWindow=true;c.openUnlocked=true;c.expandableInventory=false;c.returnHotbarToInventory=true;});
                InventoryDesktopScreen.permanentlyCloseAllOpenWindows(mc);
                write("connected.json",Map.of("username",mc.player.getGameProfile().name(),"spatial",spatial));next();
            } else if(phase==2) {
                // Login inventory/handshake packets precede the receiving-level screen's dismissal.
                if(tick-entered<50||mc.gui.screen()!=null||mc.gui.overlay()!=null)return;
                if(!mc.player.getInventory().getItem(9).is(Items.DIRT)||mc.player.getInventory().getItem(9).getCount()!=32)return;
                InventoryDesktopScreen.openOrToggleInventory(mc);next();
            } else if(phase==3) {
                if(tick-entered<35)return;
                var desktop=InventoryDesktopScreen.current(mc);
                if(desktop==null||!desktop.hasWindows())throw new IllegalStateException("remote-desktop-opens: native="+mc.gui.screen()+" desktop="+desktop+" enabled="+com.salts_inventory_update.SaltsInventoryRuntime.isEnabled()+" alive="+mc.player.isAlive());
                check("remote-desktop-opens",true);
                if(spatial)check("remote-spatial-panel-active",(Boolean)helper("spatialActive",new Class<?>[]{}));
                click(9);next();
            } else if(phase==4) {
                var desktop=InventoryDesktopScreen.current(mc);
                if(desktop.workspaceAccess().carriedStack().getCount()!=32||!DesktopClientApi.canDispatchOptimisticMutationNow())return;
                check("remote-pickup-acknowledged",true);click(10);next();
            } else if(phase==5) {
                if(tick-entered<20||!DesktopClientApi.canDispatchOptimisticMutationNow())return;
                var desktop=InventoryDesktopScreen.current(mc);
                check("remote-cursor-empty",desktop.workspaceAccess().carriedStack().isEmpty());
                check("remote-source-empty",mc.player.getInventory().getItem(9).isEmpty());
                check("remote-destination-32",mc.player.getInventory().getItem(10).is(Items.DIRT)&&mc.player.getInventory().getItem(10).getCount()==32);
                helper("capture",new Class<?>[]{String.class},spatial?"remote-spatial-desktop":"remote-core-desktop");
                write("client-results.json",Map.of("passed",passed,"spatial",spatial,"core",com.salts_inventory_update.SaltsInventoryUpdate.class.getProtectionDomain().getCodeSource().getLocation().toString()));next();
            } else if(phase==6 && Files.exists(output.resolve("server-verified"))) {
                InventoryDesktopScreen.permanentlyCloseAllOpenWindows(mc);next();
            } else if(phase==7 && tick-entered>20) {mc.stop();failed=true;}
        } catch(Throwable error) {
            write("client-failure.json",Map.of("phase",phase,"error",error.toString(),"passed",passed));
            com.salts_inventory_update.SaltsInventoryUpdate.LOGGER.error("Spatial remote QA failed",error);failed=true;mc.stop();
        }
    }
}
