package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.api.client.desktop.DesktopMenuFactories;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Address each native trade window by its entity instead of the global proxy mob. */
public final class MowzieInventories {
    private record Kind(String id,String name) {
        String menu(){return "com.bobmowzie.mowziesmobs.server.inventory.Container"+name+"Trade";}
    }
    private static final java.util.List<Kind> KINDS=java.util.List.of(new Kind("sculptor_trade","Sculptor"),new Kind("umvuthi_trade","Umvuthi"),new Kind("umvuthana_trade","Umvuthana"));
    private MowzieInventories() { }
    public static void initializeCommon(){
        if(!DesktopEnvironment.isModLoaded("mowziesmobs"))return;
        DesktopEnvironment.onServerStarting(server->DesktopEnvironment.registerIntegration("mowziesmobs",()->{
            for(var kind:KINDS){var type=NativeMenuSupport.type("mowziesmobs:"+kind.id);if(type==null||DesktopServerApi.findOpeningDataWriter(type)!=null)continue;
                NativeMenuSupport.support(type,kind.menu(),true);
                DesktopServerApi.registerOpeningData(type,(player,menu,buf)->{
                    var owner=(Entity)ForeignMenuAccess.invoke(menu,"get"+kind.name);buf.writeInt(owner.getId());
                    if(kind.name.equals("Umvuthi"))buf.writeBoolean((boolean)ForeignMenuAccess.invoke(owner,"hasTradedWith",new Class<?>[]{net.minecraft.world.entity.player.Player.class},player));
                });
            }
        }));
    }
    public static void initializeClient(){
        if(!DesktopEnvironment.isModLoaded("mowziesmobs"))return;
        DesktopClientApi.onClientStarted(mc->{for(var kind:KINDS){var type=NativeMenuSupport.type("mowziesmobs:"+kind.id);if(type==null)continue;
            NativeMenuSupport.bindNative(type,true);
            DesktopMenuFactories.register(type,(minecraft,player,id,data)->{
                boolean umvuthi=kind.name.equals("Umvuthi");
                if(data.length!=(umvuthi?5:4)||minecraft.level==null)throw new IllegalArgumentException("Invalid native trade opening");
                int entityId=java.nio.ByteBuffer.wrap(data).getInt();var entity=minecraft.level.getEntity(entityId);
                if(entity==null||!entity.isAlive())throw new IllegalArgumentException("Missing native trade owner");
                if(umvuthi&&data[4]!=0)ForeignMenuAccess.invoke(entity,"rememberTrade",new Class<?>[]{net.minecraft.world.entity.player.Player.class},player);
                try{
                    var ctor=java.util.Arrays.stream(Class.forName(kind.menu()).getConstructors()).filter(c->{var p=c.getParameterTypes();return p.length==3&&p[0]==int.class&&p[1].isInstance(entity)&&p[2]==Inventory.class;}).findFirst().orElseThrow();
                    return (AbstractContainerMenu)ctor.newInstance(id,entity,player.getInventory());
                }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native trade constructor changed",failure);}
            });
        }});
    }
}
