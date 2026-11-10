package com.salts_inventory_update.compat.travelersbackpack;

import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativePayloads;
import io.netty.buffer.Unpooled;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Modern publisher packet codecs and component snapshots; native slots remain authoritative. */
public final class TravelersPackets {
    static final Identifier PRESENTATION=Identifier.fromNamespaceAndPath("salts_inventory_compat","travelers/presentation");
    private static final List<String> TYPES=List.of("ServerboundActionTagPacket","ServerboundSlotPacket","ServerboundFilterSettingsPacket","ServerboundFilterTagsPacket");
    private static final ThreadLocal<DesktopInputContext<?,?>> INPUT=new ThreadLocal<>();
    private TravelersPackets() { }
    static boolean supported(){
        try {for(String name:TYPES){var cls=Class.forName("com.tiviacz.travelersbackpack.network."+name);cls.getField("STREAM_CODEC");cls.getField("TYPE");}return true;}
        catch(ReflectiveOperationException unsupported){return false;}
    }
    static <R> R input(DesktopInputContext<?,?> c,Supplier<R> action){var old=INPUT.get();INPUT.set(c);try{return action.get();}finally{if(old==null)INPUT.remove();else INPUT.set(old);}}
    public static boolean send(Object packet){
        var c=INPUT.get();if(c==null||!(packet instanceof CustomPacketPayload payload))return false;
        if(!TYPES.contains(packet.getClass().getSimpleName()))return false;
        c.sendPayload(payload.type().id(),NativePayloads.bytes(payload,c.minecraft().level.registryAccess()));return true;
    }
    static <T extends AbstractContainerMenu> void register(MenuType<T> type){
        for(String name:TYPES)try {
            var cls=Class.forName("com.tiviacz.travelersbackpack.network."+name);
            var channel=((CustomPacketPayload.Type<?>)cls.getField("TYPE").get(null)).id();
            var codec=(StreamCodec<?,?>)cls.getField("STREAM_CODEC").get(null);
            DesktopServerApi.registerNativePayload(type,channel,c->{
                Object packet=NativePayloads.decode(codec,c.data(),c.player());
                boolean carried=name.equals("ServerboundActionTagPacket");
                if(carried)c.menu().setCarried(c.carriedStack().copy());
                try {DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->{
                    TravelersAccess.call(TravelersAccess.wrapper(c.menu()),"addUser",c.player());
                    NativePayloads.dispatch(packet,c.player());
                });} finally {// A native settings return can retire this menu; Core retains the canonical cursor.
                    if(carried&&DesktopSessions.hasOpenSessionMatching(c.player(),menu->menu==c.menu()))
                        c.setCarriedStack(c.menu().getCarried());}
                c.broadcastChanges();
            });
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Traveler's native packet ABI",failure);}
    }
    static final class Sync {byte[] previous;}
    // Core owns registered source leases for every detached session.
    static void sync(DesktopServerSessionContext<AbstractContainerMenu,Sync> c){
        TravelersAccess.call(TravelersAccess.wrapper(c.menu()),"addUser",c.player());
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),c.player().registryAccess());
        try {
            // Owned item/tool contents already use Core's authoritative slot sync.
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer,TravelersAccess.presentation(TravelersAccess.stack(c.menu())));
            int result=TravelersAccess.settings(c.menu())?-1:TravelersAccess.integer(c.menu(),"CRAFTING_RESULT");
            buffer.writeVarInt(result);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer,result>=0&&result<c.menu().slots.size()?c.menu().getSlot(result).getItem():ItemStack.EMPTY);
            byte[] data=new byte[buffer.readableBytes()];buffer.readBytes(data);
            if(Arrays.equals(data,c.state().previous))return;c.state().previous=data;c.sendToClient(PRESENTATION,data);
        }finally{buffer.release();}
    }
}
