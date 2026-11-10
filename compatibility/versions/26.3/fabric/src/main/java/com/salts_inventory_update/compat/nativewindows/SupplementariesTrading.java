package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.*;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Red Merchant uses the same offers, scrolling, slots and authenticated selection as vanilla trading. */
public final class SupplementariesTrading {
    private static boolean serverInstalled,clientInstalled;
    private SupplementariesTrading() { }
    public static void initializeCommon() {
        if(!DesktopEnvironment.isModLoaded("supplementaries"))return;
        DesktopEnvironment.onServerStarting(server->DesktopEnvironment.registerIntegration("supplementaries",()->{
            if(serverInstalled)return;
            var type=NativeMenuSupport.type("supplementaries:red_merchant");if(type==null)return;
            NativeMenuSupport.bindNative(type,true);
            // A custom menu factory is authenticated by the normal opening-data
            // envelope even though this native constructor needs no extra fields.
            DesktopServerApi.registerOpeningData(type,(player,menu,buffer)->buffer.writeByte(0));
            DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>() {
                @Override public boolean supports(net.minecraft.server.level.ServerPlayer player,AbstractContainerMenu menu) {
                    return menu instanceof com.salts_inventory_update.api.desktop.DesktopMerchantAccess;
                }
                @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu menu){return true;}
            });
            serverInstalled=true;
        }));
    }
    public static void initializeClient() {
        if(!DesktopEnvironment.isModLoaded("supplementaries"))return;
        DesktopClientApi.onClientStarted(client->DesktopEnvironment.registerIntegration("supplementaries",()->{
            if(clientInstalled)return;
            var type=NativeMenuSupport.type("supplementaries:red_merchant");if(type==null)return;
            DesktopClientApi.registerBuiltinMerchant(type);
            DesktopMenuFactories.register(type,(minecraft,player,id,data)->{
                if(data.length!=1||data[0]!=0)throw new IllegalArgumentException("Unexpected merchant opening data");
                try{return (AbstractContainerMenu)Class.forName("net.mehvahdjukaar.supplementaries.common.inventories.RedMerchantMenu")
                    .getConstructor(int.class,Inventory.class).newInstance(id,player.getInventory());}
                catch(ReflectiveOperationException error){throw new IllegalStateException("Unsupported Red Merchant constructor",error);}
            });
            clientInstalled=true;
        }));
    }
}
