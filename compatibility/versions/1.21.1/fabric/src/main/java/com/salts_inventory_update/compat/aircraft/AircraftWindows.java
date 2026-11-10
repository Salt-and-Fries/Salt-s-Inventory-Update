package com.salts_inventory_update.compat.aircraft;

import java.util.*;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.MenuType;
import static com.salts_inventory_update.compat.aircraft.AircraftAccess.*;
import static com.salts_inventory_update.compat.aircraft.AircraftDesktopMenu.Zone;

/** Native equipment rules and native vehicle storage behind the approved three-window layout. */
public final class AircraftWindows {
    static MenuType<AircraftDesktopMenu> MENU;
    private AircraftWindows() { }
    public static void initializeCommon(){
        if(!supported())return;
        com.salts_inventory_update.compat.nativewindows.NativeMenuRegistration.register("aircraft_inventory",()->new MenuType<>((id,inventory)->{throw new IllegalStateException("Aircraft opening data required");},FeatureFlags.DEFAULT_FLAGS),AircraftWindows::bindMenu);
    }
    private static void bindMenu(MenuType<AircraftDesktopMenu> type){
        MENU=type;
        DesktopMenuBindings.register(MENU,m->m.slots.size(),true,(p,m,d)->{var a=(AircraftDesktopMenu)m;return "aircraft:"+a.vehicle.getUUID()+":"+a.zone.name();},null,null);
        DesktopMenuBindings.registerStorage(MENU,(m,s)->{var a=(AircraftDesktopMenu)m;return s.index<a.cells.size()&&a.cells.get(s.index).kind.equals("inventory");});
        DesktopServerApi.registerOpeningData(MENU,(p,m,b)->m.opening(b));
        DesktopServerApi.registerWindow(MENU,new DesktopServerWindowHandler<AircraftDesktopMenu,Void>(){
            @Override public boolean supports(ServerPlayer p,AircraftDesktopMenu m){return m.stillValid(p);}
        });
    }
    public static boolean open(Entity vehicle,ServerPlayer player){
        if(MENU==null||!DesktopSessions.shouldCapture(player)||!vehicle.isAlive()||vehicle.level()!=player.level()||vehicle.distanceToSqr(player)>=64)return false;
        var first=DesktopSessions.openMenuSession(player,provider(vehicle,Zone.MAIN),null);if(first==null)return false;
        for(Zone z:List.of(Zone.PORT,Zone.STARBOARD))if(descriptions(vehicle).stream().anyMatch(d->AircraftDesktopMenu.zone(d)==z))DesktopSessions.openMenuSession(player,provider(vehicle,z),null);
        return true;
    }
    private static SimpleMenuProvider provider(Entity vehicle,Zone zone){return new SimpleMenuProvider((id,inventory,p)->new AircraftDesktopMenu(id,inventory,vehicle,zone),zone==Zone.MAIN?vehicle.getDisplayName():Component.literal(zone==Zone.PORT?"Port":"Starboard"));}
    public static void initializeClient(){if(MENU!=null)AircraftClient.initialize();}
}
