package com.salts_inventory_update.compat.modernindustrialization;

import com.salts_inventory_update.api.server.desktop.*;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;

/** Exact per-menu native action allow-lists, still decoded by MI's own receivers. */
public final class ModernIndustrializationPackets {
    private static final ThreadLocal<Boolean> DESKTOP_ACTION=ThreadLocal.withInitial(()->false);
    private ModernIndustrializationPackets() { }
    /** Native Fabric receivers normally enqueue work. Already on the server thread,
     * execute that work before the owning desktop menu scope is restored. */
    public static boolean inDesktopAction(){return DESKTOP_ACTION.get();}
    static Set<String> actions(String menu){
        return switch(menu){
            case "machine"->Set.of("lock_all","set_locking_mode","do_slot_dragging","adjust_slot_capacity","set_auto_extract","rei_lock_slots","change_shape");
            case "forge_hammer"->Set.of("forge_hammer_move_recipe");
            case "item_pipe"->Set.of("set_item_whitelist","set_connection_type","increment_priority");
            case "fluid_pipe"->Set.of("set_network_fluid","set_connection_type","increment_priority");
            default->Set.of();
        };
    }
    static <T extends AbstractContainerMenu> void register(MenuType<T> type,String menu){
        for(String path:actions(menu)){
            var channel=new ResourceLocation("modern_industrialization",path);
            DesktopServerApi.registerNativePayload(type,channel,c->{
                var check=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
                try{if(check.readInt()!=c.menu().containerId){c.reject("MI action belongs to a different menu");return;}}
                catch(RuntimeException malformed){c.reject("Invalid MI action");return;}finally{check.release();}
                DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->{
                    var bytes=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
                    DESKTOP_ACTION.set(true);
                    try{c.player().connection.handleCustomPayload(new ServerboundCustomPayloadPacket(channel,bytes));}
                    finally{DESKTOP_ACTION.remove();if(bytes.refCnt()>0)bytes.release();}
                });c.broadcastChanges();
            });
        }
    }
}
