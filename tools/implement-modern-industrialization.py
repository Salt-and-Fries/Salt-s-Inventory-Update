"""Wire the optional MI adapter on its two published SIU targets."""
from pathlib import Path
import json,hashlib
ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'build/modern-industrialization-implementation'
def backup(path):
    target=WORK/'before'/path.relative_to(ROOT);target.parent.mkdir(parents=True,exist_ok=True)
    if path.exists() and not target.exists():target.write_bytes(path.read_bytes())
def write(path,text):
    backup(path);path.parent.mkdir(parents=True,exist_ok=True);path.write_text(text)
legacy=ROOT/'compatibility/versions/1.20.1/fabric/src/main/java/com/salts_inventory_update/compat'
modern=ROOT/'compatibility/versions/1.21.1/fabric/src/main/java/com/salts_inventory_update/compat'
prefix='com.salts_inventory_update.compat.modernindustrialization'
imports='''package com.salts_inventory_update.compat.nativewindows.mixin;
import com.salts_inventory_update.compat.modernindustrialization.ModernIndustrializationClient;
import net.minecraft.client.Minecraft;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
'''
specs=[
 ('ModernComponentPacketMixin','aztech.modern_industrialization.machines.ClientMachinePackets',[
  ('lambda$static$0','Minecraft client,int sync,int index,FriendlyByteBuf data','client,sync,index,data')]),
 ('ModernInventoryPacketMixin','aztech.modern_industrialization.inventory.ConfigurableInventoryS2CPacketHandlers',[
  ('lambda$static$0','Minecraft client,int sync,int index,CompoundTag data','client,sync,index,data'),
  ('lambda$static$2','Minecraft client,int sync,int index,CompoundTag data','client,sync,index,data')]),
 ('ModernPriorityPacketMixin','aztech.modern_industrialization.pipes.impl.ClientPipePackets',[
  ('lambda$static$0','Minecraft client,int sync,int channel,int value','client,sync,channel,value')]),
 ('ModernPipePacketMixin','aztech.modern_industrialization.pipes.impl.PipePackets',[
  ('lambda$static$0','Player player,int sync,boolean value','player,sync,value'),
  ('lambda$static$2','Player player,int sync,int value','player,sync,value'),
  ('lambda$static$6','Player player,int sync,@Coerce Object value','player,sync,value')])]
for name,owner,methods in specs:
    text=imports+f'@Pseudo @Mixin(targets="{owner}",remap=false)\npublic abstract class {name} {{\n'
    for i,(method,args,values) in enumerate(methods):
        guard='if(player!=Minecraft.getInstance().player)return;' if args.startswith('Player') else ''
        text+=f'''    @Inject(method="{method}",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private static void salts$route{i}({args},CallbackInfo ci){{
        {guard}
        if(ModernIndustrializationClient.route(sync,()->ModernIndustrializationClient.invoke("{owner}","{method}",{values})))ci.cancel();
    }}
'''
    write(legacy/f'nativewindows/mixin/{name}.java',text+'}\n')

for mc,base in [('1.20.1',legacy),('1.21.1',modern)]:
    if mc!='1.20.1':
        for p in (legacy/'modernindustrialization').glob('*.java'):
            if p.name=='ModernIndustrializationPackets.java':continue
            text=p.read_text().replace('new ResourceLocation("salts_inventory_compat","modern-industrialization/"','ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","modern-industrialization/"')
            text=text.replace('new ResourceLocation("salts_inventory_update","textures/gui/window.png")','ResourceLocation.fromNamespaceAndPath("salts_inventory_update","textures/gui/window.png")')
            text=text.replace('new ResourceLocation("modern_industrialization","textures/gui/container/background.png")','ResourceLocation.fromNamespaceAndPath("modern_industrialization","textures/gui/container/background.png")')
            write(base/'modernindustrialization'/p.name,text)
        for name in ['ModernHandledScreenMixin','ModernMachineScreenMixin']:
            text=(legacy/f'nativewindows/mixin/{name}.java').read_text()
            if name=='ModernMachineScreenMixin':text=text.replace('aztech.modern_industrialization.machines.gui.MachineScreen','aztech.modern_industrialization.client.machines.gui.MachineScreen')
            write(base/f'nativewindows/mixin/{name}.java',text)
    boot=base/'CompatibilityBootstrap.java';text=boot.read_text()
    for method in ['initializeCommon','initializeClient']:
        line=f'        initialize("modern_industrialization", "{prefix}.ModernIndustrializationWindows", "{method}");\n'
        if line not in text:text=text.replace(f'    public static void {method}() {{\n',f'    public static void {method}() {{\n'+line)
    write(boot,text)
    config=ROOT/f'compatibility/versions/{mc}/common/src/main/resources/salts_inventory_compat.reviewed-native.mixins.json'
    data=json.loads(config.read_text());names=['ModernHandledScreenMixin','ModernMachineScreenMixin']
    names+=['ModernComponentPacketMixin','ModernInventoryPacketMixin','ModernPriorityPacketMixin','ModernPipePacketMixin','ModernFluidScreenMixin'] if mc=='1.20.1' else ['ModernAddressedPacketMixin']
    for name in names:
        if name not in data['client']:data['client'].append(name)
    write(config,json.dumps(data,indent=2)+'\n')

packet=modern/'modernindustrialization/ModernIndustrializationPackets.java'
write(packet,'''package com.salts_inventory_update.compat.modernindustrialization;
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
''')
targets=[f'aztech.modern_industrialization.network.{s}' for s in ['machines.MachineComponentSyncPacket','machines.UpdateItemSlotPacket','machines.UpdateFluidSlotPacket','pipes.SetPriorityPacket','pipes.SetConnectionTypePacket','pipes.SetItemWhitelistPacket','pipes.SetNetworkFluidPacket']]
write(modern/'nativewindows/mixin/ModernAddressedPacketMixin.java',imports+'@Pseudo @Mixin(targets={'+','.join('"'+s+'"' for s in targets)+'},remap=false)\n'+'''public abstract class ModernAddressedPacketMixin {
    @Inject(method="handle",at=@At("HEAD"),cancellable=true,require=0,remap=false)
    private void salts$route(@Coerce Object context,CallbackInfo ci){
        if(ModernIndustrializationClient.routePacket(this,context))ci.cancel();
    }
}
''')
print('Installed MI integration and optional native bridges on the 1.20.1 and 1.21.1 source branches.')
