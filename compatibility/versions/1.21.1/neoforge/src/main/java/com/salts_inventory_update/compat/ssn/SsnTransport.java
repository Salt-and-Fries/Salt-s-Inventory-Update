package com.salts_inventory_update.compat.ssn;
import io.netty.buffer.Unpooled;
import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.entity.player.Player;
/** Publisher registry-aware codecs, with the exact authenticated native context. */
public final class SsnTransport {
 private SsnTransport(){}
 private static List<String> names(){var n=new ArrayList<>(SsnPackets.SERVER);n.addAll(SsnPackets.CLIENT);return n;}
 @SuppressWarnings({"rawtypes","unchecked"})public static byte[] encode(Object message,Player player){
  var b=new RegistryFriendlyByteBuf(Unpooled.buffer(),player.level().registryAccess());
  try{int id=names().indexOf(message.getClass().getSimpleName());if(id<0)throw new IllegalArgumentException("Unknown SSN payload");b.writeVarInt(id);
   ((StreamCodec)message.getClass().getField("STREAM_CODEC").get(null)).encode(b,message);
   byte[] result=new byte[b.readableBytes()];b.readBytes(result);return result;
  }catch(ReflectiveOperationException e){throw new IllegalStateException("SSN encoder",e);}finally{b.release();}
 }
 @SuppressWarnings("rawtypes")public static Object decode(byte[] bytes,Player player){
  var b=new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(bytes),player.level().registryAccess());
  try{int id=b.readVarInt();if(id<0||id>=names().size())throw new IllegalArgumentException("Unknown SSN payload");
   Object message=((StreamCodec)Class.forName(SsnPackets.PREFIX+names().get(id)).getField("STREAM_CODEC").get(null)).decode(b);
   if(b.isReadable())throw new IllegalArgumentException("Trailing SSN payload bytes");return message;
  }catch(ReflectiveOperationException e){throw new IllegalStateException("SSN decoder",e);}finally{b.release();}
 }
 public static void receive(Object message,Player player,boolean server){
  try{Method handler=Arrays.stream(message.getClass().getDeclaredMethods()).filter(m->m.getName().equals("handle")&&Modifier.isStatic(m.getModifiers())&&m.getParameterCount()==2).findFirst().orElseThrow();
   Class<?> type=handler.getParameterTypes()[1];Object context=Proxy.newProxyInstance(type.getClassLoader(),new Class<?>[]{type},(proxy,method,args)->switch(method.getName()){
    case "player"->player;
    case "enqueueWork"->{Object value=null;if(args[0]instanceof Runnable task)task.run();else if(args[0]instanceof java.util.function.Supplier<?> task)value=task.get();else throw new IllegalArgumentException("Unknown native task");yield CompletableFuture.completedFuture(value);}
    case "toString"->"SIU owning SSN session";case "hashCode"->System.identityHashCode(proxy);case "equals"->proxy==args[0];
    default->throw new IllegalStateException("Unexpected SSN context operation: "+method.getName());
   });handler.invoke(null,message,context);
  }catch(InvocationTargetException e){if(e.getCause()instanceof RuntimeException r)throw r;throw new IllegalStateException("SSN receiver",e.getCause());}
  catch(ReflectiveOperationException e){throw new IllegalStateException("SSN receiver",e);}
 }
}
