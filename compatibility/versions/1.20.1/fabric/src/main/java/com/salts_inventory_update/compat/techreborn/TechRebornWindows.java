package com.salts_inventory_update.compat.techreborn;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import io.netty.buffer.Unpooled;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Original menus, slots and codecs, scoped to each authenticated SIU session. */
public final class TechRebornWindows {
    public static final ResourceLocation UPDATE=new ResourceLocation("salts_inventory_compat","techreborn/update");
    private static final Set<String> BOUND=new HashSet<>();
    private static final Map<AbstractContainerMenu,DesktopServerSessionContext<AbstractContainerMenu,Void>> OWNERS=new IdentityHashMap<>();
    private static final Map<AbstractContainerMenu,ContainerListener> LISTENERS=new IdentityHashMap<>();
    private TechRebornWindows() { }
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(s->bind());}
    @SuppressWarnings("unchecked") static synchronized void bind(){
        for(var raw:BuiltInRegistries.MENU){
            var id=BuiltInRegistries.MENU.getKey(raw);
            if(id==null||!id.getNamespace().equals("techreborn")||!BOUND.add(id.toString()))continue;
            var type=(MenuType<AbstractContainerMenu>)raw;
            NativeMenuSupport.bindNative(type,true);
            DesktopMenuBindings.registerStorage(type,(menu,slot)->false);
            DesktopServerApi.registerOpeningData(type,(p,m,b)->b.writeBlockPos(position(m)));
            DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){
                @Override public boolean supports(ServerPlayer p,AbstractContainerMenu m){return m.getClass().getName().equals("reborncore.common.screen.BuiltScreenHandler")&&block(m)==p.level().getBlockEntity(position(m));}
                @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu m){return true;}
                @Override public void opened(DesktopServerSessionContext<AbstractContainerMenu,Void> c){OWNERS.put(c.menu(),c);
                    Object cache=RebornAccess.field(c.menu(),"syncPairCache");if(cache instanceof Map map)map.replaceAll((k,v)->null);
                    var listener=new ContainerListener(){
                        @Override public void slotChanged(AbstractContainerMenu menu,int slot,net.minecraft.world.item.ItemStack stack){}
                        @Override public void dataChanged(AbstractContainerMenu menu,int property,int value){}
                    };
                    LISTENERS.put(c.menu(),listener);c.menu().addSlotListener(listener);c.menu().broadcastChanges();}
                @Override public void tick(DesktopServerSessionContext<AbstractContainerMenu,Void> c){c.menu().broadcastChanges();}
                @Override public void closed(DesktopServerSessionContext<AbstractContainerMenu,Void> c){var listener=LISTENERS.remove(c.menu());if(listener!=null)c.menu().removeSlotListener(listener);OWNERS.remove(c.menu());}
            });
            for(String path:List.of("fluid_config_save","config_save","fluid_io_save","io_save","slot_save","chunk_loader_request","set_redstone_state"))register(type,new ResourceLocation("reborncore",path));
            for(String path:switch(id.getPath()){
                case "aesu"->List.of("aesu");case "auto_crafting_table"->List.of("auto_crafting_lock");
                case "rolling_machine"->List.of("rolling_machine_lock");case "storage_unit"->List.of("storage_unit_lock");
                case "fusion_control_computer","fusion_controller"->List.of("fusion_control_size");case "chunk_loader"->List.of("chunkloader");
                case "iron_furnace"->List.of("experience");case "player_detector"->List.of("detector_radius");
                case "launchpad"->List.of("launch_speed");case "pump"->List.of("pump_depth","pump_range");default->List.<String>of();})register(type,new ResourceLocation("techreborn",path));
        }
    }
    private static void register(MenuType<AbstractContainerMenu> type,ResourceLocation channel){
        DesktopServerApi.registerNativePayload(type,channel,c->{
            var check=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
            try{
                if(channel.getPath().equals("fusion_control_size"))check.readInt();
                if(!check.readBlockPos().equals(position(c.menu()))||block(c.menu())!=c.player().level().getBlockEntity(position(c.menu()))||!c.menu().stillValid(c.player())){c.reject("Reborn action targets another machine");return;}
                if(c.data().length>24576){c.reject("Reborn configuration exceeds bound");return;}
                switch(channel.getPath()){
                    case "io_save"->{int slot=check.readInt();if(slot<0||!(block(c.menu()) instanceof net.minecraft.world.Container inventory)||slot>=inventory.getContainerSize())throw new IllegalArgumentException("slot");check.readBoolean();check.readBoolean();check.readBoolean();check.readInt();}
                    case "fluid_io_save"->{check.readBoolean();check.readBoolean();}
                    case "config_save","slot_save","fluid_config_save"->{if(check.readNbt()==null)throw new IllegalArgumentException("nbt");}
                    case "set_redstone_state"->{int length=check.readInt();if(length<1||length>64)throw new IllegalArgumentException("element");check.readUtf(length);int state=check.readInt();if(state<0||state>=3)throw new IllegalArgumentException("state");}
                    case "auto_crafting_lock","rolling_machine_lock","storage_unit_lock"->check.readBoolean();
                    case "aesu"->{int button=check.readInt();if(!Set.of(-256,-64,64,256).contains(button))throw new IllegalArgumentException("button");check.readBoolean();check.readBoolean();}
                    case "chunkloader"->{check.readInt();check.readBoolean();}
                    case "detector_radius","launch_speed","pump_depth","pump_range"->check.readInt();
                }
                if(check.isReadable())throw new IllegalArgumentException("trailing bytes");
                var bytes=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
                try{c.player().connection.handleCustomPayload(new ServerboundCustomPayloadPacket(channel,bytes));}finally{if(bytes.refCnt()>0)bytes.release();}
                c.broadcastChanges();
            }catch(RuntimeException malformed){c.reject("Invalid Reborn action");}finally{check.release();}
        });
    }
    public static BlockEntity block(AbstractContainerMenu menu){return(BlockEntity)RebornAccess.call(menu,"getBlockEntity");}
    public static BlockPos position(AbstractContainerMenu menu){return block(menu).getBlockPos();}
    /** Reborn's native packet lacks a menu ID. Attach the real SIU session here. */
    public static boolean send(Object listener,AbstractContainerMenu menu,Int2ObjectMap<?> values){
        var owner=OWNERS.get(menu);if(owner==null)return false;
        // The publisher recognizes only vanilla's private listener class.
        // SIU's detached menu has its own listener; OWNERS is the authoritative
        // player/session association for this exact menu instance instead.
        if(values.isEmpty())return true;
        Object packet=RebornAccess.call(RebornAccess.type("reborncore.common.network.ClientBoundPackets"),"createPacketSendObject",menu,values);
        var bytes=(FriendlyByteBuf)RebornAccess.call(packet,"packetByteBuf");
        try{byte[] payload=new byte[bytes.readableBytes()];bytes.getBytes(bytes.readerIndex(),payload);DesktopSessions.sendSessionPayload(owner.player(),owner.sessionId(),UPDATE,payload);}finally{bytes.release();}
        return true;
    }
}
