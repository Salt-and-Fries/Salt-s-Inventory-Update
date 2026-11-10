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
    private static java.util.function.Supplier<MenuType<AbstractContainerMenu>> registration;
    private static final Map<AbstractContainerMenu,Entity> ENTITIES=Collections.synchronizedMap(new WeakHashMap<>());
    private SpawnInventoryBridge() { }
    public static MenuType<?> type(){return registeredType();}
    private static MenuType<AbstractContainerMenu> registeredType(){return type!=null?type:registration==null?null:registration.get();}
    private static boolean hamster(Entity entity){return entity!=null&&Set.of("com.ninni.spawn.entity.Hamster","com.ninni.spawn.server.entity.mob.Hamster").contains(entity.getClass().getName());}
    @SuppressWarnings("unchecked")
    public static void initializeCommon() {
        if(!DesktopEnvironment.isModLoaded("spawn")||type!=null||registration!=null)return;
        java.util.function.Supplier<MenuType<AbstractContainerMenu>> factory=()->new MenuType<>((id,inventory)->{throw new IllegalStateException("Hamster opening data required");},FeatureFlags.DEFAULT_FLAGS);
        if(DesktopEnvironment.isModLoaded("fabricloader"))type=Registry.register(BuiltInRegistries.MENU,new ResourceLocation("salts_inventory_compat","hamster"),factory.get());
        else if(DesktopEnvironment.isModLoaded("forge"))try{
            registration=(java.util.function.Supplier<MenuType<AbstractContainerMenu>>)Class.forName("com.salts_inventory_update.compat.forge.ReviewedEntityMenuRegistration")
                .getMethod("register",String.class,java.util.function.Supplier.class).invoke(null,"hamster",factory);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Forge hamster menu registration failed",failure);}
        else return;
        DesktopEnvironment.onServerStarting(server->{
            var type=registeredType();
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
        if(registeredType()==null||!(owner instanceof ServerPlayer player)||!DesktopSessions.shouldCapture(player))return false;
        try {
            var cargo=(Container)hamster.getClass().getField("inventory").get(hamster);
            var provider=new SimpleMenuProvider((id,inventory,p)->menu(id,inventory,cargo,hamster),hamster.getDisplayName());
            return DesktopSessions.openMenuSession(player,provider,buffer->buffer.writeInt(hamster.getId()))!=null;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
    }
    public static void initializeClient() {
        var type=registeredType();
        if(type==null)return;
        DesktopClientApi.onClientStarted(mc->{
            DesktopMenuFactories.register(type,(minecraft,player,id,data)->{
                if(data.length!=4||minecraft.level==null)throw new IllegalArgumentException("Invalid pet opening data");
                int entityId=java.nio.ByteBuffer.wrap(data).getInt();var entity=minecraft.level.getEntity(entityId);
                if(!hamster(entity))throw new IllegalArgumentException("Missing hamster");
                return menu(id,player.getInventory(),new SimpleContainer(12),entity);
            });
            DesktopClientApi.registerNativeScreen(type,(menu,inventory,title)->{
                var entity=Objects.requireNonNull(ENTITIES.get(menu));
                try{return (net.minecraft.client.gui.screens.inventory.AbstractContainerScreen<?>)Class.forName("com.ninni.spawn.client.inventory.HamsterInventoryScreen")
                    .getConstructor(menu.getClass(),Inventory.class,entity.getClass()).newInstance(menu,inventory,entity);}
                catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            });
        });
    }
}
