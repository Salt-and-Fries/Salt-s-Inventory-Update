"""Carry the approved SSN native host to its two published modern SIU targets."""
from pathlib import Path
import json
ROOT=Path(__file__).resolve().parents[1]
legacy=ROOT/'compatibility/versions/1.20.1/fabric/src/main/java/com/salts_inventory_update/compat'
transport='''package com.salts_inventory_update.compat.ssn;
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
'''
mix='''package com.salts_inventory_update.compat.ssn.mixin;
import com.salts_inventory_update.compat.ssn.*;
import java.util.*;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Mixin(value=PacketDistributor.class,remap=false)
public abstract class SsnTransportMixin {
 @Inject(method="sendToServer",at=@At("HEAD"),cancellable=true,remap=false)
 private static void salts$ownedInput(CustomPacketPayload first,CustomPacketPayload[] rest,CallbackInfo ci){
  var pending=new ArrayList<CustomPacketPayload>();boolean routed=SsnClient.send(first);if(!routed)pending.add(first);
  for(var p:rest)if(SsnClient.send(p))routed=true;else pending.add(p);
  if(routed){for(var p:pending)PacketDistributor.sendToServer(p);ci.cancel();}
 }
 @Inject(method="sendToPlayer",at=@At("HEAD"),cancellable=true,remap=false)
 private static void salts$ownedUpdate(ServerPlayer player,CustomPacketPayload first,CustomPacketPayload[] rest,CallbackInfo ci){
  var pending=new ArrayList<CustomPacketPayload>();boolean routed=SsnWindows.send(player,first);if(!routed)pending.add(first);
  for(var p:rest)if(SsnWindows.send(player,p))routed=true;else pending.add(p);
  if(routed){for(var p:pending)PacketDistributor.sendToPlayer(player,p);ci.cancel();}
 }
}
'''

for version in ['1.21.1','26.1.2']:
    base=ROOT/f'compatibility/versions/{version}/fabric/src/main/java/com/salts_inventory_update/compat'
    for p in (legacy/'ssn').rglob('*.java'):
        if p.name=='SsnItemCountMixin.java':continue # Modern publisher already scales the local GUI pose.
        target=base/'ssn'/p.relative_to(legacy/'ssn');target.parent.mkdir(parents=True,exist_ok=True);s=p.read_text()
        s=s.replace('new ResourceLocation("salts_inventory_compat","ssn/"+path)','ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","ssn/"+path)')
        if p.name=='SsnMouseScopeMixin.java':
            s=s.replace('double x,double y,double amount','double x,double y,double horizontal,double amount').replace('original.call(x,y,amount)','original.call(x,y,horizontal,amount)')
        if version.startswith('26.'):
            s=s.replace('ResourceLocation','Identifier').replace('net.minecraft.client.gui.components.EditBox','net.minecraft.client.gui.components.EditBox')
            # Minecraft now sends structured input events to native screens.
            if p.name=='SsnMouseScopeMixin.java':
                a=s.index('    @WrapMethod');s=s[:a]+'''    @WrapMethod(method="mouseClicked",remap=false,require=0)
    private boolean salts$nativeOwner(net.minecraft.client.input.MouseButtonEvent event,boolean twice,Operation<Boolean> original){return SsnClient.nativeCall(this,()->original.call(event,twice));}
    @WrapMethod(method="mouseScrolled",remap=false,require=0)
    private boolean salts$nativeScroll(double x,double y,double horizontal,double amount,Operation<Boolean> original){return SsnClient.nativeCall(this,()->original.call(x,y,horizontal,amount));}
}
'''
            if p.name=='SsnKeyboardScopeMixin.java':
                a=s.index('    @WrapMethod');s=s[:a]+'''    @WrapMethod(method="keyPressed",remap=false,require=0)
    private boolean salts$nativeKey(net.minecraft.client.input.KeyEvent event,Operation<Boolean> original){return SsnClient.nativeCall(this,()->original.call(event));}
    @WrapMethod(method="charTyped",remap=false,require=0)
    private boolean salts$nativeText(net.minecraft.client.input.CharacterEvent event,Operation<Boolean> original){return SsnClient.nativeCall(this,()->original.call(event));}
}
'''
            if p.name=='SsnIntegerInputMixin.java':
                s=s.replace('int key,int scan,int mods','net.minecraft.client.input.KeyEvent event').replace('original.call(key,scan,mods)','original.call(event)')
                s=s.replace('char ch,int mods','net.minecraft.client.input.CharacterEvent event').replace('original.call(ch,mods)','original.call(event)')
        target.write_text(s)
    native=ROOT/f'compatibility/versions/{version}/neoforge/src/main'
    server_mix=mix
    client_mix=None
    if version.startswith('26.'):
        start=mix.index(' @Inject(method="sendToServer"')
        end=mix.index(' @Inject(method="sendToPlayer"')
        client_mix=mix[:start].replace('net.neoforged.neoforge.network.PacketDistributor','net.neoforged.neoforge.client.network.ClientPacketDistributor').replace('PacketDistributor.class','ClientPacketDistributor.class').replace('class SsnTransportMixin','class SsnClientTransportMixin')+mix[start:end].replace('PacketDistributor.sendToServer','ClientPacketDistributor.sendToServer')+'}\n'
        server_mix=mix[:start]+mix[end:]
    variants=[('SsnTransport.java',transport),('mixin/SsnTransportMixin.java',server_mix)]
    if client_mix:variants.append(('mixin/SsnClientTransportMixin.java',client_mix))
    for name,s in variants:
        p=native/'java/com/salts_inventory_update/compat/ssn'/name;p.parent.mkdir(parents=True,exist_ok=True);p.write_text(s)
    boot=base/'CompatibilityBootstrap.java';s=boot.read_text()
    for side in ['Common','Client']:
        cls='Windows'if side=='Common'else'Client';line=f'        initialize("storagenetwork", "com.salts_inventory_update.compat.ssn.Ssn{cls}", "initialize{side}");'
        if line not in s:s=s.replace(f'public static void initialize{side}() {{',f'public static void initialize{side}() {{\n'+line,1)
    boot.write_text(s)
    res=ROOT/f'compatibility/versions/{version}/common/src/main/resources'
    d=json.loads((ROOT/'compatibility/versions/1.20.1/common/src/main/resources/salts_inventory_compat.ssn.mixins.json').read_text());d['compatibilityLevel']='JAVA_21'if version=='1.21.1'else'JAVA_25';d.pop('refmap',None)
    d['client']=[name for name in d['client']if name!='SsnItemCountMixin']
    (res/'salts_inventory_compat.ssn.mixins.json').write_text(json.dumps(d,indent=2))
    d=dict(required=True,package='com.salts_inventory_update.compat.ssn.mixin',compatibilityLevel=d['compatibilityLevel'],plugin=d['plugin'],mixins=['SsnTransportMixin'],injectors={'defaultRequire':1})
    if client_mix:d['client']=['SsnClientTransportMixin']
    (native/'resources/salts_inventory_compat.ssn-transport.mixins.json').write_text(json.dumps(d,indent=2))
    meta=native/'resources/META-INF/neoforge.mods.toml';s=meta.read_text();needle='[[dependencies.${mod_id}]]'
    for config in ['ssn','ssn-transport']:
        value='${mod_id}.'+config+'.mixins.json'
        if value not in s:s=s.replace(needle,'[[mixins]]\nconfig = "'+value+'"\n\n'+needle,1)
    meta.write_text(s)
print('Prepared modern SSN sources, loader transports and optional mixin configs')
