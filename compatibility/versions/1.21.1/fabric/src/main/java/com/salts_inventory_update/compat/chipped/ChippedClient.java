package com.salts_inventory_update.compat.chipped;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

public final class ChippedClient {
    private static boolean registered;
    private ChippedClient() { }
    public static void initialize() { register();DesktopClientApi.onClientStarted(client->register()); }
    private static synchronized void register() {
        if(registered)return;var type=NativeMenuSupport.type("chipped:workbench");if(type==null)return;
        ChippedIntegration.bindings();
        DesktopMenuFactories.register(type,(client,player,id,data)->{
            if(data.length!=8)throw new IllegalArgumentException("Invalid Chipped opening data");
            var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            try { return (AbstractContainerMenu)Class.forName(ChippedNative.MENU)
                .getConstructor(int.class,Inventory.class).newInstance(id,player.getInventory()); }
            catch(ReflectiveOperationException e){throw new IllegalStateException("Chipped menu reconstruction failed",e);}
            finally { b.release(); }
        });
        DesktopClientApi.registerWindow(type,new ChippedWindow());registered=true;
    }
}
