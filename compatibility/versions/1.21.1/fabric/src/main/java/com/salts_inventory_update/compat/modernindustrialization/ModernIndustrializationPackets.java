package com.salts_inventory_update.compat.modernindustrialization;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativePayloads;
import java.util.*;
import java.lang.reflect.Proxy;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
public final class ModernIndustrializationPackets {
    private ModernIndustrializationPackets() { }
    private static Map<String,String> actions(String menu){return switch(menu){
        case "machine"->Map.ofEntries(Map.entry("lock_all","machines.LockAllPacket"),Map.entry("set_locking_mode","machines.SetLockingModePacket"),
            Map.entry("do_slot_dragging","machines.DoSlotDraggingPacket"),Map.entry("adjust_slot_capacity","machines.AdjustSlotCapacityPacket"),
            Map.entry("set_auto_extract","machines.SetAutoExtractPacket"),Map.entry("rei_lock_slots","machines.ReiLockSlotsPacket"),Map.entry("change_shape","machines.ChangeShapePacket"));
        case "forge_hammer"->Map.of("move_recipe","machines.MoveRecipePacket");
        case "item_pipe"->Map.of("set_item_whitelist","pipes.SetItemWhitelistPacket","set_connection_type","pipes.SetConnectionTypePacket","increment_priority","pipes.IncrementPriorityPacket");
        case "fluid_pipe"->Map.of("set_network_fluid","pipes.SetNetworkFluidPacket","set_connection_type","pipes.SetConnectionTypePacket","increment_priority","pipes.IncrementPriorityPacket");
        default->Map.of();};}
    static <T extends AbstractContainerMenu> void register(MenuType<T> type,String menu){
        for(var action:actions(menu).entrySet())try{
            Class<?> packetType=Class.forName("aztech.modern_industrialization.network."+action.getValue());
            var codec=(StreamCodec<?,?>)packetType.getField("STREAM_CODEC").get(null);
            DesktopServerApi.registerNativePayload(type,ResourceLocation.fromNamespaceAndPath("modern_industrialization",action.getKey()),c->{
                Object packet;
                try{packet=NativePayloads.decode(codec,c.data(),c.player());
                    if((Integer)packet.getClass().getMethod("syncId").invoke(packet)!=c.menu().containerId){c.reject("MI action belongs to a different menu");return;}}
                catch(ReflectiveOperationException|RuntimeException invalid){c.reject("Invalid MI action");return;}
                DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->{
                    try{
                        Class<?> api=Class.forName("net.neoforged.neoforge.network.handling.IPayloadContext");
                        Object inner=Proxy.newProxyInstance(api.getClassLoader(),new Class<?>[]{api},(proxy,method,args)->switch(method.getName()){
                            case "player"->c.player();case "flow"->PacketFlow.SERVERBOUND;
                            case "toString"->"SIU authenticated MI action";
                            default->throw new UnsupportedOperationException("Unexpected MI payload context operation: "+method.getName());});
                        Class<?> context=Class.forName("aztech.modern_industrialization.network.BasePacket$Context");
                        Object owner=context.getConstructor(Class.class,api).newInstance(packetType,inner);
                        packetType.getMethod("handle",context).invoke(packet,owner);
                    }catch(ReflectiveOperationException failure){throw new IllegalStateException("MI original receiver",failure);}
                });c.broadcastChanges();
            });
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("MI packet codec",failure);}
    }
}
