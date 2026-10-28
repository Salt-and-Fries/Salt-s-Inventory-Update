package com.salts_inventory_update.compat.extendeddrawersaddon;

import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.compat.nativewindows.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Upgrade controls retain the addon's native permissions, callbacks and extended opening data. */
public final class ExtendedDrawersAddonIntegration {
    private static boolean clientFactory;
    private ExtendedDrawersAddonIntegration() { }
    public static void initializeCommon() {
        DesktopEnvironment.onServerStarting(server->DesktopEnvironment.registerIntegration("extendeddrawersaddon",()->{
            var type=NativeMenuSupport.type("minecraft:drawer");
            if(type==null)return;
            NativeMenuSupport.support(type,"net.extendeddrawersaddon.screen.DrawerScreenHandler",true);
            if(DesktopServerApi.findOpeningDataWriter(type)==null)DesktopServerApi.registerOpeningData(type,(player,menu,buffer)->{
                long count=menu.slots.stream().filter(s->s.container!=player.getInventory()).count();
                buffer.writeInt((int)(count-4)/2);
            });
        }));
    }
    public static void initializeClient() {
        NativeWindowPolish.installTemplate("extendeddrawersaddon","minecraft:drawer",()->new MockupNativeTemplate<>(NativeBatchLayouts::drawers));
        registerFactory();DesktopClientApi.onClientStarted(client->registerFactory());
    }
    private static synchronized void registerFactory() {
        if(clientFactory)return;var type=NativeMenuSupport.type("minecraft:drawer");if(type==null)return;
        NativeMenuSupport.bindNative(type,true);
        DesktopMenuFactories.register(type,(client,player,id,data)->{
            if(data.length!=4)throw new IllegalArgumentException("Invalid native drawer opening data");
            var buffer=new FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data));
            try {
                int count=buffer.getInt(0);if(count!=1&&count!=2&&count!=4)throw new IllegalArgumentException("Unsupported drawer compartment count");
                return (AbstractContainerMenu)Class.forName("net.extendeddrawersaddon.screen.DrawerScreenHandler")
                    .getConstructor(int.class,Inventory.class,int.class).newInstance(id,player.getInventory(),count);
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native drawer reconstruction failed",failure);}
            finally{buffer.release();}
        });clientFactory=true;
    }
}
