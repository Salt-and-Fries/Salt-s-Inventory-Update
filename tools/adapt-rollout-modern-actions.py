"""Modern typed-payload, component-state and native-widget adaptations."""
from pathlib import Path
import importlib.util,re
ROOT=Path(__file__).resolve().parents[1]
def module(name,file):
    spec=importlib.util.spec_from_file_location(name,ROOT/'tools'/file);m=importlib.util.module_from_spec(spec);spec.loader.exec_module(m);return m
ports=module('ports','port-rollout-compatibility.py');core=module('core','resolve-rollout-core.py')

def main():
    for v in ports.VERSIONS:
        root=ROOT/f'compatibility/versions/{v}/fabric/src/main/java/com/salts_inventory_update/compat'
        p=root/'travelersbackpack/TravelersPackets.java';p.write_text(ports.convert((ROOT/'tools/rollout-templates/TravelersPackets.java').read_text(),v))
        p=root/'travelersbackpack/TravelersWindows.java';s=p.read_text()
        s=s.replace('CompoundTag result;','ItemStack result=ItemStack.EMPTY;int resultSlot=-1;')
        # Stable item-source identity belongs in CUSTOM_DATA on component versions.
        s=re.sub(r'if\(number\(w,"getScreenID"\)==1&&!stack\(m\)\.getOrCreateTag\(\)\.hasUUID\(ID\)\)stack\(m\)\.getOrCreateTag\(\)\.putUUID\(ID,UUID.randomUUID\(\)\);','if(number(w,"getScreenID")==1)identity(stack(m),true);',s)
        s=s.replace('stack(m).getOrCreateTag().hasUUID(ID)','identity(stack(m),false)!=null').replace('stack(m).getTag().getUUID(ID)','identity(stack(m),false)')
        s=s.replace('!identity(stack(m),false)!=null','identity(stack(m),false)==null')
        s=s.replace('s->s.hasTag()&&s.getTag().hasUUID(ID)&&s.getTag().getUUID(ID).equals(id)','s->id.equals(identity(s,false))')
        a=s.index('            DesktopServerApi.registerOpeningData(type,');b=s.index('            DesktopServerApi.registerWindow(type,',a);s=s[:a]+s[b:]
        a=s.index('            DesktopMenuFactories.register(type,');b=s.index('            DesktopClientApi.registerNativeMatcher(',a);s=s[:a]+s[b:]
        s=core.replace_method(s,'        private void applyResult(','''        private void applyResult(DesktopWindowContext<AbstractContainerMenu,State> c){
            int id=c.state().resultSlot;
            if(id>=0&&id<c.model().slots.size()&&c.model().getSlot(id).container instanceof ResultContainer result)
                result.setItem(0,c.state().result.copy());
        }''')
        s=core.replace_method(s,'        @Override public void customPayload(','''        @Override public void customPayload(DesktopWindowContext<AbstractContainerMenu,State> c,ResourceLocation channel,byte[] data){
            if(!channel.equals(TravelersPackets.PRESENTATION)||c.minecraft().level==null)return;
            var b=com.salts_inventory_update.compat.nativewindows.NativePayloads.buffer(data,c.minecraft().level.registryAccess());
            try {
                ItemStack copy=ItemStack.OPTIONAL_STREAM_CODEC.decode(b);int result=b.readVarInt();
                ItemStack output=ItemStack.OPTIONAL_STREAM_CODEC.decode(b);
                if(b.isReadable())throw new IllegalArgumentException("Trailing backpack snapshot");
                Object w=wrapper(c.model());ItemStack old=stack(c.model());
                boolean changed=!ItemStack.isSameItemSameComponents(old,copy);
                call(w,"setBackpackStack",copy);c.state().resultSlot=result;c.state().result=output;
                if(changed){call(w,"loadUpgrades");DIRTY.add(w);}applyResult(c);
            } finally {b.release();}
        }''')
        helper='''    private static UUID identity(ItemStack stack,boolean create){
        var data=stack.get(net.minecraft.core.component.DataComponents.CUSTOM_DATA);
        var tag=data==null?new CompoundTag():data.copyTag();
        String text=tag.READ_STRING;
        try {if(!text.isEmpty())return UUID.fromString(text);}catch(IllegalArgumentException invalid){if(!create)return null;}
        if(!create)return null;UUID id=UUID.randomUUID();tag.putString(ID,id.toString());
        stack.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(tag));return id;
    }
'''.replace('READ_STRING','getString(ID)' if v=='1.21.1' else 'getStringOr(ID,"")')
        s=s.replace('    static final class State {',helper+'    static final class State {')
        p.write_text(ports.convert(s,v))
        p=root/'tradingpost/TradingPostWindows.java';s=p.read_text()
        s=s.replace('byte[] expected=new byte[payload.getData().readableBytes()];payload.getData().getBytes(payload.getData().readerIndex(),expected);payload.getData().release();','byte[] expected=com.salts_inventory_update.compat.nativewindows.NativePayloads.bytes(payload.payload(),net.minecraft.core.RegistryAccess.EMPTY);')
        s=s.replace('payload.getIdentifier()','payload.payload().type().id()');p.write_text(s)
    # Create 1.21.1 uses Catnip's typed per-action packet registry.
    root=ROOT/'compatibility/versions/1.21.1/fabric/src/main/java/com/salts_inventory_update/compat'
    p=root/'create/CreatePackets.java';source=p.read_text();a=source.index('        Set<String> allowed=switch(menu)');b=source.index('        if(!allowed.isEmpty())',a);allowed=source[a:b]
    p.write_text('''package com.salts_inventory_update.compat.create;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativePayloads;
import java.util.*;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.*;
public final class CreatePackets {
    private static final Map<AbstractContainerMenu,Runnable> REQUESTER_FINAL=new WeakHashMap<>();
    private CreatePackets() { }
    public static void afterMenuRemoved(AbstractContainerMenu menu){var apply=REQUESTER_FINAL.remove(menu);if(apply!=null)apply.run();}
    static <T extends AbstractContainerMenu> void register(MenuType<T> type,String menu){
        if(menu.startsWith("createbigcannons:")){cannons(type);return;}
'''+allowed+'''
        try {
            for(Object action:Class.forName("com.simibubi.create.AllPackets").getEnumConstants()){
                String name=((Enum<?>)action).name();if(!allowed.contains(name))continue;
                var field=action.getClass().getDeclaredField("type");field.setAccessible(true);Object descriptor=field.get(action);
                var channel=((CustomPacketPayload.Type<?>)descriptor.getClass().getMethod("type").invoke(descriptor)).id();
                var codec=(StreamCodec<?,?>)descriptor.getClass().getMethod("codec").invoke(descriptor);
                DesktopServerApi.registerNativePayload(type,channel,c->{
                    Object packet=NativePayloads.decode(codec,c.data(),c.player());
                    Runnable receive=()->DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->NativePayloads.dispatch(packet,c.player()));
                    if(menu.equals("create:redstone_requester")&&name.equals("CONFIGURE_REDSTONE_REQUESTER"))
                        REQUESTER_FINAL.put(c.menu(),()->NativePayloads.dispatch(packet,c.player()));
                    else receive.run();c.broadcastChanges();
                });
            }
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Create native action registry",failure);}
    }
    private static <T extends AbstractContainerMenu> void cannons(MenuType<T> type){
        try {
            var cls=Class.forName("rbasamoyai.createbigcannons.network.CBCNeoForgePacket");
            var channel=((CustomPacketPayload.Type<?>)cls.getField("TYPE").get(null)).id();
            var codec=(StreamCodec<?,?>)cls.getField("STREAM_CODEC").get(null);
            DesktopServerApi.registerNativePayload(type,channel,c->{
                Object wrapper=NativePayloads.decode(codec,c.data(),c.player());
                try {
                    Object packet=wrapper.getClass().getMethod("pkt").invoke(wrapper);
                    if(!packet.getClass().getName().equals("rbasamoyai.createbigcannons.network.ServerboundSetContainerValuePacket")){c.reject("Cannon action does not belong to this menu");return;}
                    DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->{
                        try {packet.getClass().getMethod("handle",java.util.concurrent.Executor.class,net.minecraft.network.PacketListener.class,net.minecraft.world.entity.player.Player.class)
                            .invoke(packet,(java.util.concurrent.Executor)Runnable::run,c.player().connection,c.player());}
                        catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
                    });c.broadcastChanges();
                }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}
            });
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("CBC native typed packet",failure);}
    }
}
''')
    p=root/'cataclysm/MinistrosityWindows.java';s=p.read_text().replace('c.nativeGraphics(),x+37,y+58,36,\n                    (float)(x+31-c.mouseX()),(float)(y+43-c.mouseY()),entity','c.nativeGraphics(),x+5,y+7,x+58,y+80,36,0.0F,(float)c.mouseX(),(float)c.mouseY(),entity');p.write_text(s)
    print('Modern actions, item-component snapshots and native Create codecs adapted.')

if __name__=='__main__':main()
