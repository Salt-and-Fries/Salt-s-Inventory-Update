package com.salts_inventory_update.compat.prominent;

import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import io.netty.buffer.Unpooled;
import java.util.function.Function;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;

/** The original two owo messages, authenticated to their own live menu. */
final class ProminentPackets {
    private static final ResourceLocation CHANNEL=new ResourceLocation("owo","local_packet");
    private ProminentPackets() { }
    @SuppressWarnings("unchecked")
    static void register(MenuType<AbstractContainerMenu> type,ProminentWindows.Kind kind){
        DesktopServerApi.registerNativePayload(type,CHANNEL,c->{
            if(c.data().length>512){c.reject("Oversized native forge action");return;}
            var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
            try{
                if(buffer.readVarInt()!=0){c.reject("Unsupported native forge action");return;}
                Class<?> expected=Class.forName(kind.menu+"$"+(kind==ProminentWindows.Kind.UPGRADE?"UpgradePrimordialForge":"TryCraft"));
                Object serializer=Class.forName("io.wispforest.owo.network.serialization.PacketBufSerializer").getMethod("get",Class.class).invoke(null,expected);
                Object record=((Function<FriendlyByteBuf,?>)ForeignMenuAccess.invoke(serializer,"deserializer")).apply(buffer);
                if(!expected.isInstance(record)||buffer.isReadable()){c.reject("Invalid native forge message");return;}
                if(kind==ProminentWindows.Kind.ARMOR){
                    int piece=(int)ForeignMenuAccess.invoke(record,"pieceIdx");String set=(String)ForeignMenuAccess.invoke(record,"setId");
                    if(piece<0||piece>3||set.length()>128){c.reject("Invalid native armor choice");return;}
                }
            }catch(ReflectiveOperationException|RuntimeException failure){c.reject("Invalid native forge message");return;}
            finally{buffer.release();}
            DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->{
                var nativeData=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
                try{
                    ProminentAccess.attach(c.menu(),c.player());
                    ForeignMenuAccess.invoke(c.menu(),"owo$handlePacket",new Class<?>[]{FriendlyByteBuf.class,boolean.class},nativeData,false);
                }finally{nativeData.release();}
            });
            c.broadcastChanges();
        });
    }
}
