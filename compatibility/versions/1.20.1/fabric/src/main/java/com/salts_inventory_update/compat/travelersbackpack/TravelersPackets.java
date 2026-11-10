package com.salts_inventory_update.compat.travelersbackpack;

import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.server.desktop.*;
import io.netty.buffer.Unpooled;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Supplier;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.*;

/** Preserve the mod's original decoded actions inside the owning native menu session. */
public final class TravelersPackets {
    static final ResourceLocation PRESENTATION=new ResourceLocation("salts_inventory_compat","travelers/presentation");
    private static final Map<String,String> TYPES=Map.of("action_tag","ServerboundActionTagPacket","slots","ServerboundSlotPacket","filter_settings","ServerboundFilterSettingsPacket","filter_tags","ServerboundFilterTagsPacket");
    private static final ThreadLocal<DesktopInputContext<?,?>> INPUT=new ThreadLocal<>();
    private static Boolean supported;
    private TravelersPackets() { }
    static boolean supported(){
        if(supported!=null)return supported;
        try{
            for(String name:TYPES.values()){
                Class<?> cls=Class.forName("com.tiviacz.travelersbackpack.network."+name);
                cls.getMethod("decode",FriendlyByteBuf.class);cls.getMethod("encode",cls,FriendlyByteBuf.class);
                if(action(cls)==null)return supported=false;
            }
            return supported=true;
        }catch(ReflectiveOperationException unsupported){return supported=false;}
    }
    private static Method action(Class<?> type){
        return Arrays.stream(type.getDeclaredMethods()).filter(m->m.getName().startsWith("lambda$handle$")&&m.getParameterCount()==2
            &&Arrays.asList(m.getParameterTypes()).contains(type)
            &&(Arrays.asList(m.getParameterTypes()).contains(ServerPlayer.class)||Arrays.asList(m.getParameterTypes()).contains(Supplier.class))).findFirst().orElse(null);
    }
    private static ResourceLocation channel(Object packet){
        // Fabric exposes IPacket#getPacketId; Forge's equivalent records use
        // SimpleChannel instead. Both encode the same four owned actions.
        try{return (ResourceLocation)packet.getClass().getMethod("getPacketId").invoke(packet);}
        catch(NoSuchMethodException forge){
            return TYPES.entrySet().stream().filter(e->packet.getClass().getName().equals("com.tiviacz.travelersbackpack.network."+e.getValue()))
                .map(e->new ResourceLocation("travelersbackpack",e.getKey())).findFirst().orElse(null);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native backpack packet identity",failure);}
    }
    @SuppressWarnings({"unchecked","rawtypes"})
    private static Supplier<?> forgeSender(ServerPlayer player){
        try{
            Class<?> type=Class.forName("net.minecraftforge.network.NetworkEvent$Context");
            Class<?> direction=Class.forName("net.minecraftforge.network.NetworkDirection");
            var ctor=type.getDeclaredConstructor(net.minecraft.network.Connection.class,direction,int.class);ctor.setAccessible(true);
            // Forge exposes this connection as a public field. Select it by
            // type so both named development and SRG release names work.
            var connectionField=Arrays.stream(player.connection.getClass().getFields())
                .filter(f->f.getType()==net.minecraft.network.Connection.class).findFirst().orElseThrow();
            Object connection=connectionField.get(player.connection);
            Object context=ctor.newInstance(connection,Enum.valueOf((Class)direction,"PLAY_TO_SERVER"),0);
            return ()->context;
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native Forge backpack context",failure);}
    }
    static <R> R input(DesktopInputContext<?,?> c,Supplier<R> action){var previous=INPUT.get();INPUT.set(c);try{return action.get();}finally{if(previous==null)INPUT.remove();else INPUT.set(previous);}}
    public static boolean send(Object packet){
        var c=INPUT.get();if(c==null)return false;
        ResourceLocation channel=channel(packet);
        if(channel==null||!channel.getNamespace().equals("travelersbackpack")||!TYPES.containsKey(channel.getPath()))return false;
        var b=new FriendlyByteBuf(Unpooled.buffer());
        try{TravelersAccess.call(packet,"encode",packet,b);byte[] data=new byte[b.readableBytes()];b.readBytes(data);c.sendPayload(channel,data);return true;}finally{b.release();}
    }
    static void register(MenuType<AbstractContainerMenu> type){
        for(var entry:TYPES.entrySet()){
            var channel=new ResourceLocation("travelersbackpack",entry.getKey());
            DesktopServerPayloadHandler<AbstractContainerMenu> handler=c->{
                Object packet;Method action;boolean cursor=false;var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
                try{
                    Class<?> cls=Class.forName("com.tiviacz.travelersbackpack.network."+entry.getValue());
                    packet=cls.getMethod("decode",FriendlyByteBuf.class).invoke(null,b);
                    if(b.isReadable())throw new IllegalArgumentException("Trailing native packet bytes");
                    if(entry.getKey().equals("action_tag")){
                        int id=((CompoundTag)TravelersAccess.call(packet,"actionTag")).getInt("ActionType");
                        if(id<0||id>17)throw new IllegalArgumentException("Unknown backpack action");
                        cursor=id==5;
                    }else if(!entry.getKey().equals("slots")){
                        int slot=TravelersAccess.integer(packet,"slot");
                        if(slot<0||slot>=TravelersAccess.number(TravelersAccess.call(TravelersAccess.wrapper(c.menu()),"getUpgrades"),"getSlots"))throw new IllegalArgumentException("Invalid upgrade slot");
                    }
                    // The upstream receiver queues this original lambda on its
                    // server. Invoke that same decoded action synchronously so
                    // its containerMenu checks see the authenticated session.
                    action=action(cls);if(action==null)throw new IllegalArgumentException("Unsupported native backpack receiver");action.setAccessible(true);
                }catch(ReflectiveOperationException|RuntimeException failure){c.reject("Invalid or unsupported native backpack action");return;}finally{b.release();}
                Object decoded=packet;Method run=action;
                if(cursor)c.menu().setCarried(c.carriedStack());
                try{DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->{
                        Object wrapper=TravelersAccess.wrapper(c.menu());TravelersAccess.call(wrapper,"addUser",c.player());
                        // Vanilla closes settings before the block factory picks
                        // its next menu. A detached replacement is constructed
                        // first, so clear only this native settings flag now.
                        if(entry.getKey().equals("action_tag")){
                            CompoundTag tag=(CompoundTag)TravelersAccess.call(decoded,"actionTag");
                            if(tag.getInt("ActionType")==10&&!tag.getBoolean("Arg1")&&TravelersAccess.settings(c.menu())&&TravelersAccess.number(wrapper,"getScreenID")==3)
                                TravelersAccess.call(c.player().level().getBlockEntity((net.minecraft.core.BlockPos)TravelersAccess.call(wrapper,"getBackpackPos")),"removeSettingsUser");
                        }
                        try{
                            Object sender=Arrays.asList(run.getParameterTypes()).contains(ServerPlayer.class)?c.player():forgeSender(c.player());
                            run.invoke(null,run.getParameterTypes()[0].isInstance(decoded)?new Object[]{decoded,sender}:new Object[]{sender,decoded});
                        }
                        catch(ReflectiveOperationException failure){throw new IllegalStateException("Original backpack action",failure);}
                    });
                }finally{// A native settings return can retire this menu; Core retains the canonical cursor.
                    if(cursor&&DesktopSessions.hasOpenSessionMatching(c.player(),menu->menu==c.menu()))
                        c.setCarriedStack(c.menu().getCarried());}
                c.broadcastChanges();
            };
            DesktopServerApi.registerNativePayload(type,channel,handler);
        }
    }
    static final class Sync {CompoundTag previous;}
    // Core owns registered source leases for every detached session.
    static void sync(DesktopServerSessionContext<AbstractContainerMenu,Sync> c){
        // Old/new menus share this upstream set during a settings transition.
        // Closing the previous menu removes the user; retain the live session.
        TravelersAccess.call(TravelersAccess.wrapper(c.menu()),"addUser",c.player());
        CompoundTag tag=TravelersAccess.stack(c.menu()).getOrCreateTag().copy();
        tag.remove("Inventory");tag.remove("ToolsInventory"); // real cells use SIU's slot synchronization
        if(!TravelersAccess.settings(c.menu())){
            int result=TravelersAccess.integer(c.menu(),"CRAFTING_RESULT");
            if(result>0&&result<c.menu().slots.size()){
                tag.putInt("SiuResultSlot",result);
                tag.put("SiuResult",c.menu().getSlot(result).getItem().save(new CompoundTag()));
                if(c.menu().getSlot(result).container instanceof ResultContainer container&&container.getRecipeUsed()!=null)
                    tag.putString("SiuRecipe",container.getRecipeUsed().getId().toString());
            }
        }
        if(tag.equals(c.state().previous))return;c.state().previous=tag.copy();
        var b=new FriendlyByteBuf(Unpooled.buffer());try{b.writeNbt(tag);byte[] bytes=new byte[b.readableBytes()];b.readBytes(bytes);c.sendToClient(PRESENTATION,bytes);}finally{b.release();}
    }
}
