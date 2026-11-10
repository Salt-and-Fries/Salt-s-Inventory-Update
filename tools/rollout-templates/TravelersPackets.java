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
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

/** Modern publisher packet codecs and component snapshots; native slots remain authoritative. */
public final class TravelersPackets {
    static final ResourceLocation PRESENTATION=ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","travelers/presentation");
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
        c.sendCustomPayload(payload.type().id(),NativePayloads.bytes(payload,c.minecraft().level.registryAccess()));return true;
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
    static final class Sync {byte[] previous;AutoCloseable sourceLease;}
    static void lock(DesktopServerSessionContext<AbstractContainerMenu,Sync> c){
        try {c.state().sourceLease=(AutoCloseable)Class.forName("com.salts_inventory_update.internal.desktop.DesktopItemSourceLocks")
            .getMethod("acquire",net.minecraft.world.entity.player.Player.class,AbstractContainerMenu.class).invoke(null,c.player(),c.menu());}
        catch(ReflectiveOperationException failure){throw new IllegalStateException("Backpack source lock",failure);}
    }
    static void unlock(Sync state){if(state.sourceLease!=null)try{state.sourceLease.close();state.sourceLease=null;}catch(Exception failure){throw new IllegalStateException(failure);}}
    static void sync(DesktopServerSessionContext<AbstractContainerMenu,Sync> c){
        TravelersAccess.call(TravelersAccess.wrapper(c.menu()),"addUser",c.player());
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),c.player().registryAccess());
        try {
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer,TravelersAccess.stack(c.menu()));
            int result=TravelersAccess.settings(c.menu())?-1:TravelersAccess.integer(c.menu(),"CRAFTING_RESULT");
            buffer.writeVarInt(result);
            ItemStack.OPTIONAL_STREAM_CODEC.encode(buffer,result>=0&&result<c.menu().slots.size()?c.menu().getSlot(result).getItem():ItemStack.EMPTY);
            byte[] data=new byte[buffer.readableBytes()];buffer.readBytes(data);
            if(Arrays.equals(data,c.state().previous))return;c.state().previous=data;c.sendToClient(PRESENTATION,data);
        }finally{buffer.release();}
    }
}
