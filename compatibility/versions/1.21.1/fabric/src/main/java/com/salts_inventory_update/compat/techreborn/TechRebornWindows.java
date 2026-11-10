package com.salts_inventory_update.compat.techreborn;
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
