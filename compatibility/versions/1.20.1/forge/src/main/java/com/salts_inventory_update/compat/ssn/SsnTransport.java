package com.salts_inventory_update.compat.ssn;

import io.netty.buffer.Unpooled;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkEvent;

/** Invoke original Forge codecs and the already-enqueued native receiver body. */
public final class SsnTransport {
    private SsnTransport() { }
    private static List<String> names(){var names=new ArrayList<>(SsnPackets.SERVER);names.addAll(SsnPackets.CLIENT);return names;}
    public static byte[] encode(Object message,Player player){
        var b=new FriendlyByteBuf(Unpooled.buffer());
        try{
            int id=names().indexOf(message.getClass().getSimpleName());if(id<0)throw new IllegalArgumentException("Unknown SSN message");b.writeVarInt(id);
            message.getClass().getMethod("encode",message.getClass(),FriendlyByteBuf.class).invoke(null,message,b);
            byte[] bytes=new byte[b.readableBytes()];b.readBytes(bytes);return bytes;
        }catch(ReflectiveOperationException e){throw new IllegalStateException("SSN encoder",e);}finally{b.release();}
    }
    public static Object decode(byte[] bytes,Player player){
        var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try{
            int id=b.readVarInt();if(id<0||id>=names().size())throw new IllegalArgumentException("Unknown SSN message");
            var type=Class.forName(SsnPackets.PREFIX+names().get(id));Object message=type.getMethod("decode",FriendlyByteBuf.class).invoke(null,b);
            if(b.isReadable())throw new IllegalArgumentException("Trailing SSN message bytes");return message;
        }catch(ReflectiveOperationException e){throw new IllegalStateException("SSN decoder",e);}finally{b.release();}
    }
    public static void receive(Object message,Player player,boolean server){
        try{
            var connection=server?((net.minecraft.server.level.ServerPlayer)player).connection.connection:Minecraft.getInstance().getConnection().getConnection();
            var ctor=NetworkEvent.Context.class.getDeclaredConstructor(net.minecraft.network.Connection.class,NetworkDirection.class,int.class);ctor.setAccessible(true);
            var context=ctor.newInstance(connection,server?NetworkDirection.PLAY_TO_SERVER:NetworkDirection.PLAY_TO_CLIENT,0);
            Supplier<NetworkEvent.Context> supplier=()->context;
            Method handler=Arrays.stream(message.getClass().getDeclaredMethods()).filter(m->m.getName().equals("handleInternal")).findFirst().orElse(null);
            if(handler==null)handler=Arrays.stream(message.getClass().getDeclaredMethods()).filter(m->m.getName().equals("lambda$handle$0")).findFirst().orElseThrow();
            Object[] args=new Object[handler.getParameterCount()];for(int i=0;i<args.length;i++)args[i]=Supplier.class.isAssignableFrom(handler.getParameterTypes()[i])?supplier:message;
            handler.setAccessible(true);handler.invoke(null,args);
        }catch(InvocationTargetException e){if(e.getCause()instanceof RuntimeException r)throw r;throw new IllegalStateException("SSN receiver",e.getCause());}
        catch(ReflectiveOperationException e){throw new IllegalStateException("SSN receiver",e);}
    }
}
