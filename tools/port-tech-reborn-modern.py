"""Adapt the approved native Tech Reborn composition to typed-payload releases."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / 'compatibility/versions/1.20.1/fabric/src/main'

WINDOWS = r'''package com.salts_inventory_update.compat.techreborn;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import com.salts_inventory_update.compat.nativewindows.NativePayloads;
import io.netty.buffer.Unpooled;
import java.lang.reflect.*;
import java.util.*;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;
import net.minecraft.world.level.block.entity.BlockEntity;

/** Original typed codecs/receivers, confined to the authenticated native menu. */
public final class TechRebornWindows {
    public static final ResourceLocation UPDATE=ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","techreborn/update");
    private static final Set<String> BOUND=new HashSet<>();
    private static final Map<ResourceLocation,Object> RECEIVERS=new HashMap<>();
    private static final Map<AbstractContainerMenu,DesktopServerSessionContext<AbstractContainerMenu,Void>> OWNERS=new IdentityHashMap<>();
    private static final Map<AbstractContainerMenu,ContainerListener> LISTENERS=new IdentityHashMap<>();
    private static final ThreadLocal<AbstractContainerMenu> SYNC=new ThreadLocal<>();
    private static final ThreadLocal<DesktopServerPayloadContext<AbstractContainerMenu>> ACTION=new ThreadLocal<>();
    private TechRebornWindows(){}
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(s->bind());}
    @SuppressWarnings("unchecked") private static synchronized void bind(){
        for(var raw:BuiltInRegistries.MENU){var id=BuiltInRegistries.MENU.getKey(raw);
            if(id==null||!id.getNamespace().equals("techreborn")||!BOUND.add(id.toString()))continue;
            var type=(MenuType<AbstractContainerMenu>)raw;
            NativeMenuSupport.bindNative(type,true);DesktopMenuBindings.registerStorage(type,(menu,slot)->false);
            DesktopServerApi.registerOpeningData(type,(p,m,b)->b.writeBlockPos(position(m)));
            DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){
                @Override public boolean supports(ServerPlayer p,AbstractContainerMenu m){return m.getClass().getName().equals("reborncore.common.screen.BuiltScreenHandler")&&block(m)==p.level().getBlockEntity(position(m));}
                @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu m){return true;}
                @Override public void opened(DesktopServerSessionContext<AbstractContainerMenu,Void> c){OWNERS.put(c.menu(),c);
                    Object cache=RebornAccess.field(c.menu(),"syncPairCache");if(cache instanceof Map map)map.replaceAll((k,v)->null);
                    var listener=new ContainerListener(){public void slotChanged(AbstractContainerMenu m,int i,net.minecraft.world.item.ItemStack s){} public void dataChanged(AbstractContainerMenu m,int i,int v){}};
                    LISTENERS.put(c.menu(),listener);c.menu().addSlotListener(listener);c.menu().broadcastChanges();}
                @Override public void tick(DesktopServerSessionContext<AbstractContainerMenu,Void> c){c.menu().broadcastChanges();}
                @Override public void closed(DesktopServerSessionContext<AbstractContainerMenu,Void> c){var listener=LISTENERS.remove(c.menu());if(listener!=null)c.menu().removeSlotListener(listener);OWNERS.remove(c.menu());}
            });
            for(String name:List.of("FluidConfigSavePayload","SlotConfigSavePayload","FluidIoSavePayload","IoSavePayload","SlotSavePayload","ChunkLoaderRequestPayload","SetRedstoneStatePayload"))register(type,"reborncore.common.network.serverbound."+name);
            for(String name:switch(id.getPath()){
                case "aesu"->List.of("AESUConfigPayload");case "auto_crafting_table"->List.of("AutoCraftingLockPayload");case "rolling_machine"->List.of("RollingMachineLockPayload");
                case "storage_unit"->List.of("StorageUnitLockPayload");case "fusion_controller","fusion_control_computer"->List.of("FusionControlSizePayload");case "chunk_loader"->List.of("ChunkloaderPayload");
                case "iron_furnace"->List.of("ExperiencePayload");case "player_detector"->List.of("DetectorRadiusPayload");case "launchpad"->List.of("LaunchSpeedPayload");
                case "pump"->List.of("PumpDepthPayload","PumpRangePayload");default->List.<String>of();})register(type,"techreborn.packets.serverbound."+name);
        }
    }
    @SuppressWarnings({"unchecked","rawtypes"}) private static void register(MenuType<AbstractContainerMenu> type,String name){
        Class<?> packetType=RebornAccess.type(name);Object key=RebornAccess.staticField(packetType,"ID");
        ResourceLocation channel=((CustomPacketPayload.Type<?>)key).id();
        DesktopServerApi.registerNativePayload(type,channel,c->{
            try{
                if(c.data().length>24576||!c.menu().stillValid(c.player())||block(c.menu())!=c.player().level().getBlockEntity(position(c.menu())))throw new IllegalArgumentException("owner");
                Object packet=NativePayloads.decode((StreamCodec)RebornAccess.codec(packetType),c.data(),c.player());
                if(!position(c.menu()).equals(RebornAccess.call(packet,"pos")))throw new IllegalArgumentException("position");
                if(name.endsWith("IoSavePayload")&&!name.endsWith("FluidIoSavePayload")){
                    int slot=(Integer)RebornAccess.call(packet,"slotID");
                    if(slot<0||!(block(c.menu())instanceof net.minecraft.world.Container inventory)||slot>=inventory.getContainerSize())throw new IllegalArgumentException("slot");
                }
                Object handler=RECEIVERS.get(channel);
                if(handler==null)throw new IllegalStateException("Missing native receiver "+channel);
                Class<?> contextType=RebornAccess.type("net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking$Context");
                Object context=Proxy.newProxyInstance(contextType.getClassLoader(),new Class<?>[]{contextType},(proxy,method,args)->switch(method.getName()){
                    case "player"->c.player();case "server"->c.player().level().getServer();case "toString"->"SIU Tech Reborn native context";default->throw new IllegalStateException(method.getName());});
                ACTION.set(c);try{RebornAccess.call(handler,"receive",packet,context);}finally{ACTION.remove();}
                c.broadcastChanges();
            }catch(RuntimeException malformed){c.reject("Invalid Reborn action: "+malformed.getMessage());}
        });
    }
    public static BlockEntity block(AbstractContainerMenu m){return(BlockEntity)RebornAccess.call(m,"getBlockEntity");}
    public static void remember(CustomPacketPayload.Type<?> type,Object handler){var id=type.id();if(id.getNamespace().equals("reborncore")||id.getNamespace().equals("techreborn"))RECEIVERS.put(id,handler);}
    public static BlockPos position(AbstractContainerMenu m){return block(m).getBlockPos();}
    public static Boolean authorize(ServerPlayer player,Object payload,Predicate<AbstractContainerMenu> predicate){
        var c=ACTION.get();if(c==null)return null;
        return c.player()==player&&position(c.menu()).equals(RebornAccess.call(payload,"pos"))&&predicate.test(c.menu())&&c.menu().stillValid(player);
    }
    public static void beginSync(AbstractContainerMenu menu){if(OWNERS.containsKey(menu))SYNC.set(menu);else SYNC.remove();}
    public static void endSync(){SYNC.remove();}
    @SuppressWarnings("unchecked") public static Optional<ServerPlayer> listener(Object listener){
        var owner=OWNERS.get(SYNC.get());if(owner!=null)return Optional.of(owner.player());
        return(Optional<ServerPlayer>)RebornAccess.call(RebornAccess.type("reborncore.common.screen.ServerPlayerEntityScreenHandlerHelper"),"getServerPlayerEntity",listener);
    }
    @SuppressWarnings({"unchecked","rawtypes"}) public static boolean sendUpdate(CustomPacketPayload payload,ServerPlayer player){
        var owner=OWNERS.get(SYNC.get());if(owner==null||owner.player()!=player||!payload.getClass().getSimpleName().equals("ScreenHandlerUpdatePayload"))return false;
        var buffer=new RegistryFriendlyByteBuf(Unpooled.buffer(),player.registryAccess());
        try{((StreamCodec)RebornAccess.staticField(payload.getClass(),"PACKET_CODEC")).encode(buffer,payload);byte[] bytes=new byte[buffer.readableBytes()];buffer.readBytes(bytes);DesktopSessions.sendSessionPayload(player,owner.sessionId(),UPDATE,bytes);}finally{buffer.release();}
        return true;
    }
}
'''

SYNC = r'''package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.TechRebornWindows;
import java.util.Optional;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="reborncore.common.screen.BuiltScreenHandler",remap=false)
public abstract class RebornSyncMixin {
    @Inject(method="sendContentUpdatePacketToListener",at=@At("HEAD"),remap=false) private void salts$begin(@Coerce Object listener,CallbackInfo ci){TechRebornWindows.beginSync((AbstractContainerMenu)(Object)this);}
    @Inject(method="sendContentUpdatePacketToListener",at=@At("RETURN"),remap=false) private void salts$end(@Coerce Object listener,CallbackInfo ci){TechRebornWindows.endSync();}
    @Redirect(method="sendContentUpdatePacketToListener",at=@At(value="INVOKE",target="Lreborncore/common/screen/ServerPlayerEntityScreenHandlerHelper;getServerPlayerEntity",remap=false),remap=false)
    private Optional<ServerPlayer> salts$owner(@Coerce Object listener){return TechRebornWindows.listener(listener);}
}
'''
NETWORK = r'''package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.TechRebornWindows;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="reborncore.common.network.NetworkManager",remap=false)
public abstract class RebornNetworkMixin {
    @Inject(method="sendToPlayer",at=@At("HEAD"),cancellable=true,remap=false) private static void salts$update(CustomPacketPayload payload,ServerPlayer player,CallbackInfo ci){if(TechRebornWindows.sendUpdate(payload,player))ci.cancel();}
}
'''
POSITION = r'''package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.TechRebornWindows;
import java.util.function.Predicate;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
@Pseudo @Mixin(targets="reborncore.common.network.BlockPosPayload",remap=false)
public interface RebornBlockPosMixin {
    @Inject(method="canUse",at=@At("HEAD"),cancellable=true,remap=false)
    default void salts$owner(ServerPlayer player,Predicate<AbstractContainerMenu> predicate,CallbackInfoReturnable<Boolean> ci){var allowed=TechRebornWindows.authorize(player,this,predicate);if(allowed!=null)ci.setReturnValue(allowed);}
}
'''
CLIENT_PACKET = r'''package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.TechRebornScreens;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
@Pseudo @Mixin(targets="net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking",remap=false)
public abstract class RebornClientPacketMixin {
    @Inject(method="send",at=@At("HEAD"),cancellable=true,remap=false) private static void salts$key(CustomPacketPayload packet,CallbackInfo ci){if(TechRebornScreens.send(packet))ci.cancel();}
}
'''
RECEIVER = r'''package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.TechRebornWindows;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
/** Retain the publisher's handler when it registers through Fabric's public API. */
@Pseudo @Mixin(targets="net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking",remap=false)
public abstract class RebornReceiverMixin {
    @Inject(method="registerGlobalReceiver",at=@At("HEAD"),remap=false)
    private static void salts$remember(CustomPacketPayload.Type<?> type,@Coerce Object handler,CallbackInfoReturnable<Boolean> ci){TechRebornWindows.remember(type,handler);}
}
'''
FACE = r'''package com.salts_inventory_update.compat.techreborn.mixin;
import com.salts_inventory_update.compat.techreborn.RebornAccess;
import com.salts_inventory_update.compat.techreborn.TechRebornScreens;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.*;
/** Submit the publisher's custom picture state directly, outside native capture. */
@Pseudo @Mixin(targets="reborncore.client.gui.config.elements.AbstractConfigPopupElement",remap=false)
public abstract class RebornFaceCaptureMixin {
    @Redirect(method="draw",at=@At(value="INVOKE",target="Lnet/minecraft/client/renderer/state/gui/GuiRenderState;addPicturesInPictureState",remap=false),remap=false)
    private void salts$defer(@Coerce Object state,@Coerce Object picture){if(!TechRebornScreens.capturing())RebornAccess.call(state,"addPicturesInPictureState",picture);}
}
'''

def main():
    for mc in ['1.21.1','26.1.2','26.2','26.3']:
        modern=mc!='1.21.1'
        main=ROOT/f'compatibility/versions/{mc}/fabric/src/main'
        dest=main/'java/com/salts_inventory_update/compat/techreborn';(dest/'mixin').mkdir(parents=True,exist_ok=True)
        source=BASE/'java/com/salts_inventory_update/compat/techreborn'
        files={p.name:p.read_text()for p in source.glob('*.java')}
        client=files['TechRebornClient.java']
        client=client.replace('new ResourceLocation(', 'ResourceLocation.fromNamespaceAndPath(')
        old='try{return((net.fabricmc.fabric.api.screenhandler.v1.ExtendedScreenHandlerType<?>)type).create(sync,player.getInventory(),buffer);}finally{buffer.release();}'
        new='try{var constructor=RebornAccess.type("techreborn.blockentity.GuiType$ScreenHandlerData").getDeclaredConstructor(net.minecraft.core.BlockPos.class);constructor.setAccessible(true);return(AbstractContainerMenu)RebornAccess.call(type,"create",sync,player.getInventory(),constructor.newInstance(buffer.readBlockPos()));}catch(ReflectiveOperationException failure){throw new IllegalStateException("Reborn opening data",failure);}finally{buffer.release();}'
        assert old in client;client=client.replace(old,new)
        start=client.index('            var b=new FriendlyByteBuf(',client.index('@Override public void customPayload'))
        end=client.index('\n        @Override public String layoutRevision',start)
        client=client[:start]+'''            var b=com.salts_inventory_update.compat.nativewindows.NativePayloads.buffer(data,c.minecraft().player.registryAccess());
            try{
                var codec=(net.minecraft.network.codec.StreamCodec)RebornAccess.staticField(RebornAccess.type("reborncore.common.network.clientbound.ScreenHandlerUpdatePayload"),"PACKET_CODEC");
                Object packet=codec.decode(b);if(b.isReadable())throw new IllegalArgumentException("Reborn update trailing bytes");
                RebornAccess.call(c.model(),"applyScreenHandlerData",RebornAccess.call(packet,"UPDATE_ACCESSOR"));
            }finally{b.release();}
        }
'''.replace('UPDATE_ACCESSOR','updatedValues'if mc in('26.2','26.3')else'data')+client[end:]
        screens=files['TechRebornScreens.java']
        start=screens.index('    public static boolean send(');end=screens.index('    public static AbstractContainerMenu constructionMenu',start)
        screens=screens[:start]+'''    public static boolean send(net.minecraft.network.protocol.common.custom.CustomPacketPayload packet){var c=INPUT.get();if(c==null)return false;
        return c.sendPayload(packet.type().id(),com.salts_inventory_update.compat.nativewindows.NativePayloads.bytes(packet,c.minecraft().player.registryAccess()));
    }
'''+screens[end:]
        start=screens.index('            Object types=');end=screens.index('            CONSTRUCTING.set(menu);',start)
        screens=screens[:start]+'''            Class<?> types=RebornAccess.type("techreborn.client.ClientGuiType");Object factory=null;
            for(var field:types.getFields())if(java.lang.reflect.Modifier.isStatic(field.getModifiers())&&field.getType()==types){
                Object record=field.get(null),guiType=RebornAccess.call(record,"guiType");
                if(RebornAccess.call(guiType,"getScreenHandlerType")==menu.getType()){factory=RebornAccess.call(record,"guiFactory");break;}
            }
            if(factory==null)throw new IllegalStateException("Missing original Reborn screen factory");
'''+screens[end:]
        access=files['RebornAccess.java'];i=access.index('    public static Object field(')
        access=access[:i]+'''    public static Object staticField(Class<?> type,String name){try{var f=type.getField(name);f.setAccessible(true);return f.get(null);}catch(ReflectiveOperationException error){throw new IllegalStateException(name,error);}}
    public static Object codec(Class<?> type){try{return staticField(type,"PACKET_CODEC");}catch(IllegalStateException alternate){return staticField(type,"CODEC");}}
'''+access[i:]
        screen_mixin=(source/'mixin/RebornScreenMixin.java').read_text()
        mixins={'RebornSyncMixin.java':SYNC,'RebornNetworkMixin.java':NETWORK,'RebornBlockPosMixin.java':POSITION,'RebornReceiverMixin.java':RECEIVER,'RebornClientPacketMixin.java':CLIENT_PACKET,'RebornScreenMixin.java':screen_mixin}
        if not modern:mixins['RebornPopupScissorMixin.java']=(source/'mixin/RebornPopupScissorMixin.java').read_text()
        if modern:
            client=client.replace('com.salts_inventory_update.api.client.input.','net.minecraft.client.input.')
            client=client.replace('e.key()==256','e.key()==com.mojang.blaze3d.platform.InputConstants.KEY_ESCAPE')
            client=client.replace('case "GuiSolar","GuiElevator"','case "GuiNuclearReactor"->142;case "GuiSolar","GuiElevator"')
            client=client.replace('            int sy=upgrades?86:6;','            if(name.equals("GuiNuclearReactor"))builder.exterior("native-reactor-energy",new DesktopWindowBounds(-25,6,28,75),-26,24);\n            int sy=upgrades?86:6;')
            screens=screens.replace('RebornAccess.field(screen(menu),"tabs")','RebornAccess.call(screen(menu),"getTabs")')
            screens=screens.replace('RebornAccess.call(tab,"keyPress",key,scan,modifiers)','RebornAccess.call(tab,"keyPress",new net.minecraft.client.input.KeyEvent(key,scan,modifiers))')
            index=screens.index('    private static void translate(')
            screens=screens[:index]+'''    static void drawFaces(DesktopRenderContext<AbstractContainerMenu,?> c){
        Object element=face(popup(c.model()));if(element==null)return;
        int x=c.windowX()+c.windowWidth()+42,y=c.windowY()+50;
        int mouseX=c.mouseX()-x+204,mouseY=c.mouseY()-y+4;
        c.withClip(x,y,75,85,()->
            c.withTransform(x-204,y-4,1,()->RebornAccess.call(element,"draw",c.nativeGraphics(),screen(c.model()),mouseX,mouseY)));
    }
    private static Object face(Object element){
        if(element==null)return null;
        if(RebornAccess.type("reborncore.client.gui.config.elements.AbstractConfigPopupElement").isInstance(element))return element;
        Object children=RebornAccess.field(element,"elements");if(children instanceof List<?> list)for(Object child:list){Object found=face(child);if(found!=null)return found;}
        return null;
    }
'''+screens[index:]
            client=client.replace('            c.text(c.state().popupTitle', '            TechRebornScreens.drawFaces(c);\n            c.text(c.state().popupTitle')
            mixins['RebornScreenMixin.java']=screen_mixin.replace('GuiGraphics','GuiGraphicsExtractor')
            constructors='{"<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/inventory/AbstractContainerMenu;)V","<init>(Lnet/minecraft/world/entity/player/Player;Lnet/minecraft/world/level/block/entity/BlockEntity;Lnet/minecraft/world/inventory/AbstractContainerMenu;II)V"}'
            mixins['RebornScreenMixin.java']=mixins['RebornScreenMixin.java'].replace('method="<init>"','method='+constructors)
            mixins['RebornFaceCaptureMixin.java']=FACE
        if mc=='26.3':client=client.replace('e.scancode()','e.keycode()')
        for name,data in {'TechRebornClient.java':client,'TechRebornScreens.java':screens,'RebornAccess.java':access,'TechRebornWindows.java':WINDOWS,**{'mixin/'+k:v for k,v in mixins.items()}}.items():
            if modern:data=data.replace('ResourceLocation','Identifier').replace('Identifier.fromNamespaceAndPath(','Identifier.fromNamespaceAndPath(')
            (dest/name).write_text(data)
        resources=main/'resources';resources.mkdir(exist_ok=True)
        config={'required':True,'package':'com.salts_inventory_update.compat.techreborn.mixin','compatibilityLevel':'JAVA_21'if not modern else'JAVA_25','mixins':['RebornSyncMixin','RebornNetworkMixin','RebornBlockPosMixin','RebornReceiverMixin'],'client':['RebornScreenMixin','RebornClientPacketMixin']+(['RebornFaceCaptureMixin']if modern else['RebornPopupScissorMixin']),'injectors':{'defaultRequire':1}}
        config_name='salts_inventory_compat.techreborn.mixins.json';(resources/config_name).write_text(json.dumps(config,indent=2)+'\n')
        p=resources/'fabric.mod.json';s=p.read_text();meta=json.loads(s)
        if config_name not in meta['mixins']:
            meta['mixins'].append(config_name);p.write_text(json.dumps(meta,indent=2)+'\n')
        p=main/'java/com/salts_inventory_update/compat/CompatibilityBootstrap.java';s=p.read_text()
        for method,cls in [('initializeCommon','TechRebornWindows'),('initializeClient','TechRebornClient')]:
            marker='    public static void '+method+'() {'
            if 'compat.techreborn.'+cls not in s:s=s.replace(marker,marker+'\n        if (DesktopEnvironment.isModLoaded("fabricloader")) initialize("techreborn", "com.salts_inventory_update.compat.techreborn.'+cls+'", "'+method+'");')
        p.write_text(s)
        print('Ported',mc)

if __name__=='__main__':main()
