package com.salts_inventory_update.compat.nativewindows;

import java.lang.reflect.*;
import java.util.concurrent.CompletableFuture;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.ServerboundCustomPayloadPacket;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

/** Adapts publisher codecs and receivers to an already authenticated native session.
 * Callers must register an explicit per-menu action allow-list. */
public final class NativePayloads {
    private NativePayloads() { }
    public static RegistryFriendlyByteBuf buffer(byte[] bytes,RegistryAccess registries) {
        return new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes),registries);
    }
    public static byte[] bytes(CustomPacketPayload payload,RegistryAccess registries) {
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),registries);
        try {
            ServerboundCustomPayloadPacket.STREAM_CODEC.encode(buffer,new ServerboundCustomPayloadPacket(payload));
            buffer.readResourceLocation();
            byte[] data=new byte[buffer.readableBytes()];buffer.readBytes(data);return data;
        } finally {buffer.release();}
    }
    public static void receive(ServerPlayer player,ResourceLocation channel,byte[] bytes) {
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),player.registryAccess());
        try {
            buffer.writeResourceLocation(channel);buffer.writeBytes(bytes);
            var packet=ServerboundCustomPayloadPacket.STREAM_CODEC.decode(buffer);
            if(buffer.isReadable())throw new IllegalArgumentException("Trailing native payload bytes");
            dispatch(packet.payload(),player);
        } finally {buffer.release();}
    }
    @SuppressWarnings({"rawtypes","unchecked"})
    public static Object decode(StreamCodec codec,byte[] bytes,ServerPlayer player) {
        var buffer=buffer(bytes,player.registryAccess());
        try {Object packet=codec.decode(buffer);if(buffer.isReadable())throw new IllegalArgumentException("Trailing native action");return packet;}
        finally {buffer.release();}
    }
    public static void dispatch(Object packet,ServerPlayer player) {
        try {
            // Catnip/Create and other native handlers already take the owner directly.
            try {packet.getClass().getMethod("handle",ServerPlayer.class).invoke(packet,player);return;}
            catch(NoSuchMethodException contextual) { }
            Method handler=null;
            for(var method:packet.getClass().getDeclaredMethods())
                if(method.getName().equals("lambda$handle$0")&&Modifier.isStatic(method.getModifiers())&&method.getParameterCount()==2
                    &&java.util.Arrays.asList(method.getParameterTypes()).contains(packet.getClass())){handler=method;break;}
            if(handler==null)for(var method:packet.getClass().getDeclaredMethods())
                if(method.getName().equals("handle")&&Modifier.isStatic(method.getModifiers())&&method.getParameterCount()==2
                    &&java.util.Arrays.asList(method.getParameterTypes()).contains(packet.getClass())){handler=method;break;}
            if(handler==null)for(var method:packet.getClass().getDeclaredMethods())
                if(method.getName().equals("handleServer")&&Modifier.isStatic(method.getModifiers())&&method.getParameterCount()==2
                    &&java.util.Arrays.asList(method.getParameterTypes()).contains(packet.getClass())){handler=method;break;}
            if(handler==null)throw new NoSuchMethodException("No native authenticated receiver: "+packet.getClass());
            Object[] arguments=new Object[2];
            for(int i=0;i<arguments.length;i++)arguments[i]=handler.getParameterTypes()[i]==packet.getClass()?packet:context(handler.getParameterTypes()[i],player);
            handler.setAccessible(true);handler.invoke(null,arguments);
        } catch(ReflectiveOperationException failure) {
            Throwable cause=failure instanceof InvocationTargetException invocation?invocation.getCause():failure;
            throw new IllegalStateException("Native session receiver: "+packet.getClass().getName(),cause);
        }
    }
    private static Object context(Class<?> type,ServerPlayer player) {
        if(type.isInstance(player))return player;
        if(!type.isInterface())throw new IllegalStateException("Unsupported native context: "+type);
        return Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(proxy,method,args)->switch(method.getName()) {
            case "player" -> player;
            case "server" -> player.level().getServer();
            case "connection" -> player.connection;
            case "enqueueWork" -> {
                Object value=null;if(args[0] instanceof Runnable task)task.run();
                else if(args[0] instanceof java.util.function.Supplier<?> task)value=task.get();
                else throw new IllegalArgumentException("Unsupported native task");
                yield CompletableFuture.completedFuture(value);
            }
            case "toString" -> "SIU authenticated native payload context";
            case "hashCode" -> System.identityHashCode(proxy);
            case "equals" -> proxy==args[0];
            default -> throw new IllegalStateException("Unsupported native context operation: "+method);
        });
    }
}
