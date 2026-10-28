package com.salts_inventory_update.compat.simplyswords;

import io.netty.buffer.Unpooled;
import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.packs.resources.ReloadableResourceManager;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;

public final class SimplySwordsClient {
    private static boolean registered;
    private SimplySwordsClient() { }
    public static void initialize(){
        register();DesktopClientApi.onClientStarted(client->{
            register();
            if(client.getResourceManager() instanceof ReloadableResourceManager resources)
                resources.registerReloadListener((ResourceManagerReloadListener)manager->SimplySwordsWindow.reloadArt());
        });
    }
    private static synchronized void register(){
        if(registered)return;var type=NativeMenuSupport.type("simplyswords:runic_forge");if(type==null)return;
        SimplySwordsIntegration.bindings();
        DesktopMenuFactories.register(type,(client,player,id,data)->{
            if(data.length!=8)throw new IllegalArgumentException("Invalid Runic Forge source data");
            var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            try{return SimplySwordsIntegration.create(id,player.getInventory(),buffer.readBlockPos());}finally{buffer.release();}
        });
        DesktopClientApi.registerWindow(type,new SimplySwordsWindow());registered=true;
    }
}
