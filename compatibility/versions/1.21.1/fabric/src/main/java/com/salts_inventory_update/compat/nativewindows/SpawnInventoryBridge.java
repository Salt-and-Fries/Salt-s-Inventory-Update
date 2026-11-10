package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import java.util.*;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.*;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlags;
import net.minecraft.world.inventory.*;

/** Preserve Spawn's real pet inventory and renderer behind an authenticated SIU session. */
public final class SpawnInventoryBridge {
    private static MenuType<AbstractContainerMenu> type;
    private static final Map<AbstractContainerMenu,Entity> ENTITIES=Collections.synchronizedMap(new WeakHashMap<>());
    private SpawnInventoryBridge() { }
    public static MenuType<?> type(){return type;}
    public static void rememberOwner(AbstractContainerMenu menu,Entity owner){ENTITIES.put(menu,owner);}
    public static void initializeCommon() {
        if(!DesktopEnvironment.isModLoaded("spawn"))return;
        if(!DesktopEnvironment.isModLoaded("fabricloader")){
            // Preserve Spawn 4's registered type and native screen. Desktop
            // snapshots need the same entity ID as its ordinary open packet.
            DesktopEnvironment.onServerStarting(server->{
                NativeMenuSupport.support(NativeMenuSupport.type("spawn:pigment_shifter"),
                    "com.ninni.spawn.client.inventory.PigmentShifterMenu",false);
                var nativeType=NativeMenuSupport.type("spawn:hamster_inventory");
                NativeMenuSupport.support(nativeType,"com.ninni.spawn.client.inventory.HamsterInventoryMenu",true);
                if(nativeType!=null&&DesktopServerApi.findOpeningDataWriter(nativeType)==null)
                    DesktopServerApi.registerOpeningData(nativeType,(player,menu,buffer)->buffer.writeInt(Objects.requireNonNull(ENTITIES.get(menu)).getId()));
            });
            return;
        }
        type=Registry.register(BuiltInRegistries.MENU,ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","hamster"),
            new MenuType<AbstractContainerMenu>((id,inventory)->{throw new IllegalStateException("Hamster opening data required");},FeatureFlags.DEFAULT_FLAGS));
        DesktopEnvironment.onServerStarting(server->{
            if(DesktopServerApi.findWindowHandler(type)!=null)return;
            NativeMenuSupport.bindNative(type,true);
            DesktopServerApi.registerOpeningData(type,(player,menu,buffer)->buffer.writeInt(Objects.requireNonNull(ENTITIES.get(menu)).getId()));
            DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>() {
                @Override public boolean supports(ServerPlayer player,AbstractContainerMenu menu){return valid(menu,player);}
                @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu menu){return true;}
                @Override public void closed(DesktopServerSessionContext<AbstractContainerMenu,Void> c){ENTITIES.remove(c.menu());}
            });
        });
    }
    public static boolean valid(AbstractContainerMenu menu,Player player) {
        var entity=ENTITIES.get(menu);return entity==null||entity.isAlive()&&entity.level()==player.level()&&entity.distanceToSqr(player)<=64;
    }
    private static AbstractContainerMenu menu(int id,Inventory inventory,Container cargo,Entity entity) {
        try {
            var result=(AbstractContainerMenu)Class.forName("com.ninni.spawn.client.inventory.HamsterInventoryMenu")
                .getConstructor(int.class,Inventory.class,Container.class,entity.getClass()).newInstance(id,inventory,cargo,entity);
            ENTITIES.put(result,entity);return result;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Unsupported native hamster inventory",failure);}
    }
    public static boolean open(Entity hamster,Player owner) {
        if(type==null||!(owner instanceof ServerPlayer player)||!DesktopSessions.shouldCapture(player))return false;
        try {
            var cargo=(Container)hamster.getClass().getField("inventory").get(hamster);
            var provider=new SimpleMenuProvider((id,inventory,p)->menu(id,inventory,cargo,hamster),hamster.getDisplayName());
            return DesktopSessions.openMenuSession(player,provider,buffer->buffer.writeInt(hamster.getId()))!=null;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
    }
    public static void initializeClient() {
        if(!DesktopEnvironment.isModLoaded("spawn"))return;
        DesktopClientApi.onClientStarted(mc->{
            var clientType=type!=null?type:NativeMenuSupport.type("spawn:hamster_inventory");
            if(clientType==null)return;
            DesktopMenuFactories.register(clientType,(minecraft,player,id,data)->{
                if(data.length!=4||minecraft.level==null)throw new IllegalArgumentException("Invalid pet opening data");
                int entityId=java.nio.ByteBuffer.wrap(data).getInt();var entity=minecraft.level.getEntity(entityId);
                if(entity==null||!entity.getClass().getName().equals("com.ninni.spawn.server.entity.mob.mammal.Hamster"))throw new IllegalArgumentException("Missing hamster");
                return menu(id,player.getInventory(),new SimpleContainer(12),entity);
            });
            if(type==null)return; // NeoForge already registers its own screen factory.
            DesktopClientApi.registerNativeScreen(type,(menu,inventory,title)->{
                var entity=Objects.requireNonNull(ENTITIES.get(menu));
                try{return (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)Class.forName("com.ninni.spawn.client.inventory.HamsterInventoryScreen")
                    .getConstructor(menu.getClass(),Inventory.class,entity.getClass()).newInstance(menu,inventory,entity);}
                catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            });
        });
    }
}
