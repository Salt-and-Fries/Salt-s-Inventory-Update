package com.salts_inventory_update.compat.travelersbackpack;

import com.salts_inventory_update.api.client.desktop.DesktopMenuFactories;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.compat.nativewindows.NativePayloads;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import static com.salts_inventory_update.compat.travelersbackpack.TravelersAccess.*;

/** Exact publisher opening schemas, independent of Core's structural fallback. */
final class TravelersOpening {
    private TravelersOpening() { }
    private static boolean fabric(){return DesktopEnvironment.isModLoaded("fabricloader");}
    private static String suffix(String name){return switch(name){case "backpack_block"->"BlockEntityScreenData";case "backpack_settings"->"SettingsScreenData";default->"ItemScreenData";};}
    private static Class<?> dataType(String name)throws ClassNotFoundException{return Class.forName("com.tiviacz.travelersbackpack.init.ModScreenHandlerTypes$"+suffix(name));}
    @SuppressWarnings("rawtypes")
    private static StreamCodec codec(Class<?> type)throws ReflectiveOperationException{return (StreamCodec)type.getField("PACKET_CODEC").get(null);}
    @SuppressWarnings({"rawtypes","unchecked"})
    static void server(MenuType<AbstractContainerMenu> type,String name){
        DesktopServerApi.registerOpeningData(type,(player,menu,buffer)->{
            Object w=wrapper(menu);int kind=number(w,"getScreenID");
            BlockPos pos=kind==3?(BlockPos)call(w,"getBackpackPos"):BlockPos.ZERO;
            int index=number(w,"getBackpackSlotIndex");
            Object owner=call(w,"getBackpackOwner");int source=kind==2&&owner instanceof Player p?p.getId():index;
            try{
                if(fabric()){
                    Class<?> data=dataType(name);Object value;
                    if(name.equals("backpack_block"))value=data.getConstructor(int.class,BlockPos.class).newInstance(-1,pos);
                    else if(name.equals("backpack_settings"))value=data.getConstructor(boolean.class,int.class,BlockPos.class,int.class).newInstance(kind==3,kind,pos,index);
                    else value=data.getConstructor(int.class,int.class).newInstance(kind,source);
                    codec(data).encode(buffer,value);
                }else if(name.equals("backpack_settings")){
                    buffer.writeBoolean(kind==3);
                    if(kind==3)buffer.writeBlockPos(pos);
                    else{buffer.writeInt(kind);buffer.writeBlockPos(BlockPos.ZERO);buffer.writeInt(index);}
                }else if(name.equals("backpack_block")){buffer.writeInt(-1);buffer.writeBlockPos(pos);}
                else{buffer.writeInt(kind);buffer.writeInt(source);}
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Traveler's native opening schema",failure);}
        });
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    static void client(MenuType<AbstractContainerMenu> type,String name){
        DesktopMenuFactories.register(type,(mc,player,id,bytes)->{
            var buffer=NativePayloads.buffer(bytes,player.registryAccess());
            String suffix=switch(name){case "backpack_block"->"BackpackBlockEntityMenu";case "backpack_settings"->"BackpackSettingsMenu";default->"BackpackItemMenu";};
            try{
                Class<?> menu=Class.forName("com.tiviacz.travelersbackpack.inventory.menu."+suffix);
                AbstractContainerMenu result;
                if(fabric()){
                    Class<?> data=dataType(name);Object value=codec(data).decode(buffer);
                    result=(AbstractContainerMenu)menu.getConstructor(int.class,Inventory.class,data).newInstance(id,player.getInventory(),value);
                }else result=(AbstractContainerMenu)menu.getConstructor(int.class,Inventory.class,RegistryFriendlyByteBuf.class).newInstance(id,player.getInventory(),buffer);
                if(buffer.isReadable())throw new IllegalArgumentException("Trailing Traveler's opening data");
                return result;
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Traveler's native menu factory",failure);}
            finally{buffer.release();}
        });
    }
}
