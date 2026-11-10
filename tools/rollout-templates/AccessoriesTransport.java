package com.salts_inventory_update.compat.accessories;
import java.util.UUID;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import net.minecraft.server.level.ServerPlayer;
/** Only opens the sender's equipment. All subsequent mutations use Core sessions. */
public final class AccessoriesTransport {
    private AccessoriesTransport() { }
    private static String implementation(){
        String loader=DesktopEnvironment.isModLoaded("fabricloader")?"fabric":DesktopEnvironment.isModLoaded("neoforge")?"neoforge":"forge";
        return "com.salts_inventory_update.compat.accessories."+loader+".Accessories"+switch(loader){case "fabric"->"Fabric";case "neoforge"->"NeoForge";default->"Forge";}+"Transport";
    }
    public static boolean supported(){try{Class.forName("io.wispforest.accessories.api.AccessoriesCapability");return true;}catch(ClassNotFoundException absent){return false;}}
    public static void initialize(){invoke("initialize",new Class<?>[0]);}
    public static void open(UUID owner){invoke("open",new Class<?>[]{UUID.class},owner);}
    private static void invoke(String name,Class<?>[] signature,Object...args){
        try{Class.forName(implementation()).getMethod(name,signature).invoke(null,args);}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Accessories loader transport",failure);}
    }
    public static void receive(ServerPlayer player,UUID owner){
        if(DesktopSessions.shouldCapture(player)&&player.isAlive()&&!player.isSpectator()
            &&!DesktopSessions.hasOpenSessionMatching(player,m->m instanceof AccessoriesDesktopMenu))AccessoriesIntegration.open(player,owner);
    }
}
