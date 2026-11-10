"""Port the approved Powah working panels to the two published modern targets."""
from pathlib import Path
import json

ROOT=Path(__file__).resolve().parents[1]
LEGACY=ROOT/'compatibility/versions/1.20.1/fabric/src/main/java/com/salts_inventory_update/compat/powah'
PACKETS='''package com.salts_inventory_update.compat.powah;

import com.salts_inventory_update.api.server.desktop.DesktopServerPayloadContext;
import io.netty.buffer.Unpooled;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;

/** Publisher codecs and handlers, scoped to the authenticated owning window. */
final class PowahPackets {
    static final String PREFIX="owmii.powah.network.packet.";
    static final List<String> NAMES=List.of("NextEnergyConfigPacket","NextRedstoneModePacket","SetChannelPacket","SwitchGenModePacket","InteractWithTankPacket");
    private PowahPackets() { }
    static int index(Object message) {
        int i=NAMES.indexOf(message.getClass().getSimpleName());
        return i>=0&&message.getClass().getName().equals(PREFIX+NAMES.get(i))?i:-1;
    }
    @SuppressWarnings({"rawtypes","unchecked"})static byte[] encode(Object message,Player player) {
        int index=index(message);if(index<0)throw new IllegalArgumentException("Unknown Powah action");
        var data=new RegistryFriendlyByteBuf(Unpooled.buffer(),player.registryAccess());
        try {
            data.writeVarInt(index);((StreamCodec)message.getClass().getField("STREAM_CODEC").get(null)).encode(data,message);
            byte[] bytes=new byte[data.readableBytes()];data.readBytes(bytes);return bytes;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah action encoder",failure);}
        finally {data.release();}
    }
    @SuppressWarnings("rawtypes")static void receive(PowahWindows.Kind kind,DesktopServerPayloadContext<AbstractContainerMenu> c) {
        if(c.data().length<6||c.data().length>13){c.reject("Invalid Powah action size");return;}
        var data=new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(c.data()),c.player().registryAccess());
        try {
            int id=data.readVarInt();
            if(id<0||id>=NAMES.size()){c.reject("Unknown Powah action");return;}
            int expected=id==0||id==2?12:id==4?5:8;
            if(data.readableBytes()!=expected){c.reject("Invalid Powah payload length");return;}
            Object message=((StreamCodec)Class.forName(PREFIX+NAMES.get(id)).getField("STREAM_CODEC").get(null)).decode(data);
            var tile=PowahWindows.tile(c.menu());
            if(data.isReadable()||tile.isRemoved()||tile.getLevel()!=c.player().level()||c.player().level().getBlockEntity(tile.getBlockPos())!=tile) {
                c.reject("Powah owner is no longer available");return;
            }
            if(id==4) {
                if(kind!=PowahWindows.Kind.REACTOR&&kind!=PowahWindows.Kind.MAGMATOR&&kind!=PowahWindows.Kind.THERMO
                    ||((Number)PowahWindows.invoke(message,"containerId")).intValue()!=c.menu().containerId) {
                    c.reject("Invalid Powah tank owner");return;
                }
                // The original method uses this menu's cursor. Bridge it through
                // the public shared-cursor API, preserving native item rules.
                c.menu().setCarried(c.carriedStack());
                c.menu().getClass().getMethod("interactWithTank",boolean.class).invoke(c.menu(),(Boolean)PowahWindows.invoke(message,"drain"));
                c.setCarriedStack(c.menu().getCarried());
            }else {
                if(!tile.getBlockPos().equals((BlockPos)PowahWindows.invoke(message,"pos"))){c.reject("Powah position does not match this window");return;}
                if(id==0) {
                    int face=((Number)PowahWindows.invoke(message,"mode")).intValue();
                    if(face<0||face>6||kind==PowahWindows.Kind.CABLE&&face!=((Direction)PowahWindows.invoke(c.menu(),"getSide")).get3DDataValue()) {
                        c.reject("Invalid Powah face");return;
                    }
                }
                if(id==2) {
                    int channel=((Number)PowahWindows.invoke(message,"channel")).intValue();
                    if(kind!=PowahWindows.Kind.ENDER_CELL||channel<0||channel>=((Number)PowahWindows.invoke(tile,"getMaxChannels")).intValue()) {
                        c.reject("Invalid Powah channel");return;
                    }
                }
                if(id==3&&kind!=PowahWindows.Kind.REACTOR){c.reject("Generation mode requires a reactor");return;}
                message.getClass().getMethod("handleOnServer",ServerPlayer.class).invoke(message,c.player());
            }
            c.broadcastChanges();
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah native action",failure);}
        finally {data.release();}
    }
}
'''
CLIENT_METHODS='''    private static final Map<AbstractContainerMenu,DesktopInputContext<AbstractContainerMenu,State>> CONTEXTS=new IdentityHashMap<>();
    /** Positional publisher actions can only originate from their matching live
     * native working panel. No unrelated full-screen UI is intercepted. */
    public static boolean send(Object message) {
        int id=PowahPackets.index(message);
        if(id<0||id==4||DesktopClientApi.currentWorkspace().isEmpty())return false;
        var pos=(net.minecraft.core.BlockPos)PowahWindows.invoke(message,"pos");
        for(var entry:CONTEXTS.entrySet())if(entry.getValue().windowHandle().isOpen()&&PowahWindows.tile(entry.getKey()).getBlockPos().equals(pos)) {
            var c=entry.getValue();c.sendPayload(PowahWindows.ACTION,PowahPackets.encode(message,c.minecraft().player));return true;
        }
        return false;
    }
    public static boolean sendTank(Object menu,boolean drain) {
        var c=CONTEXTS.get(menu);
        if(c==null||!c.windowHandle().isOpen()||DesktopClientApi.currentWorkspace().isEmpty())return false;
        try {
            Object message=Class.forName(PowahPackets.PREFIX+"InteractWithTankPacket").getConstructor(int.class,boolean.class)
                .newInstance(((AbstractContainerMenu)menu).containerId,drain);
            c.sendPayload(PowahWindows.ACTION,PowahPackets.encode(message,c.minecraft().player));return true;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Powah tank action",failure);}
    }
'''
NETWORK_MIXIN='''package com.salts_inventory_update.compat.powah.mixin;
import com.salts_inventory_update.compat.powah.PowahClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="owmii.powah.network.Network",remap=false)
public abstract class PowahNetworkMixin {
    @Inject(method="toServer",at=@At("HEAD"),cancellable=true,remap=false)
    private static void salts$ownedAction(@Coerce Object message,CallbackInfo ci){if(PowahClient.send(message))ci.cancel();}
}
'''
TANK_MIXIN='''package com.salts_inventory_update.compat.powah.mixin;
import com.salts_inventory_update.compat.powah.PowahClient;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="owmii.powah.lib.logistics.inventory.AbstractTileContainer",remap=false)
public abstract class PowahTankMixin {
    @Inject(method="interactWithTank",at=@At("HEAD"),cancellable=true,remap=false)
    private void salts$ownedTank(boolean drain,CallbackInfo ci){if(PowahClient.sendTank(this,drain))ci.cancel();}
}
'''

for version in ['1.21.1','26.1.2']:
    base=ROOT/f'compatibility/versions/{version}/fabric/src/main/java/com/salts_inventory_update/compat'
    folder=base/'powah';folder.mkdir(exist_ok=True)
    for path in LEGACY.glob('*.java'):
        if version=='26.1.2'and path.name=='PowahFluids.java':continue
        s=path.read_text()
        if path.name=='PowahWindows.java':
            start=s.index('            DesktopServerApi.registerNativePayload(')
            end=s.index('\n        }\n    }',start)
            s=s[:start]+'''            DesktopServerApi.registerNativePayload(type,ACTION,c->PowahPackets.receive(kind,c));'''+s[end:]
            s=s.replace('new ResourceLocation("powah", "packet")','ResourceLocation.fromNamespaceAndPath("salts_inventory_compat", "powah/native_action")')
            s=s.replace('    private static final String[] PACKETS={"NextEnergyConfigPacket", "NextRedstoneModePacket", "SetChannelPacket", "SwitchGenModePacket"};\n','')
        if path.name=='PowahClient.java':
            s=s.replace('    private PowahClient() { }',CLIENT_METHODS+'    private PowahClient() { }')
            s=s.replace('@Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){c.state().chrome.close();}',
                '@Override public void closed(DesktopWindowContext<AbstractContainerMenu,State> c){CONTEXTS.remove(c.model());c.state().chrome.close();}\n        @Override @SuppressWarnings("unchecked")public void opened(DesktopWindowContext<AbstractContainerMenu,State> c){CONTEXTS.put(c.model(),(DesktopInputContext<AbstractContainerMenu,State>)c);}')
            s=s.replace('new ResourceLocation(', 'ResourceLocation.fromNamespaceAndPath(')
            if version=='26.1.2':s=s.replace('        @Override public void render(DesktopRenderContext<AbstractContainerMenu,State> c){PowahFluids.render(c,kind);}\n','')
        if path.name=='PowahChrome.java':s=s.replace('new ResourceLocation(', 'ResourceLocation.fromNamespaceAndPath(')
        if path.name=='PowahFluids.java':
            s=s.replace('dev.architectury.fluid.FluidStack','net.neoforged.neoforge.fluids.FluidStack')
            s=s.replace('dev.architectury.hooks.fluid.FluidStackHooks','owmii.powah.client.ClientUtils').replace('getColor','getFluidColor')
        if version=='26.1.2':
            s=s.replace('ResourceLocation','Identifier').replace('Container"','Menu"').replace('"te"','"blockEntity"')
            s=s.replace('getPixelRGBA','getPixel').replace('setPixelRGBA','setPixel')
            s=s.replace('frame=Minecraft.getInstance().getTextureManager().register("siu_powah_window",new DynamicTexture(result));',
                'frame=Identifier.fromNamespaceAndPath("salts_inventory_compat","powah/window_"+UUID.randomUUID().toString().replace("-",""));\n            Minecraft.getInstance().getTextureManager().register(frame,new DynamicTexture(()->"SIU Powah window",result));')
            if path.name=='PowahClient.java':
                start=s.index('    private static void listen(Minecraft mc) {');end=s.index('    static int width(',start)
                s=s[:start]+'''    private static void listen(Minecraft mc) { }
    public static void reloadResources(){Minecraft.getInstance().execute(()->{MASKS.clear();PowahChrome.reload();});}
'''+s[end:]
        (folder/path.name).write_text(s)
    s=PACKETS.replace('ResourceLocation','Identifier')if version=='26.1.2'else PACKETS
    (folder/'PowahPackets.java').write_text(s)
    mix=folder/'mixin';mix.mkdir(exist_ok=True)
    cable=(LEGACY.parent/'nativewindows/mixin/PowahCableOpeningMixin.java').read_text().replace('compat.nativewindows.mixin','compat.powah.mixin')
    (mix/'PowahCableOpeningMixin.java').write_text(cable)
    (mix/'PowahNetworkMixin.java').write_text(NETWORK_MIXIN)
    (mix/'PowahTankMixin.java').write_text(TANK_MIXIN.replace('AbstractTileContainer','BaseBlockEntityMenu')if version=='26.1.2'else TANK_MIXIN)
    boot=base/'CompatibilityBootstrap.java';s=boot.read_text()
    for side in ['Common','Client']:
        cls='Windows'if side=='Common'else'Client';line=f'        initialize("powah", "com.salts_inventory_update.compat.powah.Powah{cls}", "initialize{side}");'
        if line not in s:s=s.replace(f'public static void initialize{side}() {{',f'public static void initialize{side}() {{\n'+line,1)
    boot.write_text(s)
    config=dict(required=True,package='com.salts_inventory_update.compat.powah.mixin',
        compatibilityLevel='JAVA_21'if version=='1.21.1'else'JAVA_25',
        plugin='com.salts_inventory_update.compat.nativewindows.mixin.NativeWindowsMixinPlugin',
        mixins=['PowahCableOpeningMixin'],client=['PowahNetworkMixin','PowahTankMixin'],injectors={'defaultRequire':1})
    res=ROOT/f'compatibility/versions/{version}/common/src/main/resources/salts_inventory_compat.powah.mixins.json'
    res.write_text(json.dumps(config,indent=2)+'\n')
    meta=ROOT/f'compatibility/versions/{version}/neoforge/src/main/resources/META-INF/neoforge.mods.toml';s=meta.read_text()
    line='[[mixins]]\nconfig = "${mod_id}.powah.mixins.json"\n\n'
    if '.powah.mixins.json'not in s:s=s.replace('[[dependencies.${mod_id}]]',line+'[[dependencies.${mod_id}]]',1)
    meta.write_text(s)
    if version=='26.1.2':
        path=ROOT/f'compatibility/versions/{version}/neoforge/src/main/java/com/salts_inventory_update/compat/powah/PowahReloadEvents.java'
        path.parent.mkdir(parents=True,exist_ok=True)
        path.write_text('''package com.salts_inventory_update.compat.powah;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManagerReloadListener;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.AddClientReloadListenersEvent;
@EventBusSubscriber(modid="salts_inventory_compat",value=Dist.CLIENT)
public final class PowahReloadEvents {
    @SubscribeEvent public static void reloaders(AddClientReloadListenersEvent event) {
        if(net.neoforged.fml.ModList.get().isLoaded("powah"))event.addListener(
            Identifier.fromNamespaceAndPath("salts_inventory_compat","powah_windows"),
            (ResourceManagerReloadListener)resources->PowahClient.reloadResources());
    }
}
''')
print('Prepared Powah 1.21.1 and 26.1.2 native panels and authenticated actions.')
