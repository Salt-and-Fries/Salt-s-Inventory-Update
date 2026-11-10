package com.salts_inventory_update.compat.ssn;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.desktop.DesktopItemSources;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import com.salts_inventory_update.compat.ssn.SsnLayouts.Kind;
import io.netty.buffer.Unpooled;
import java.lang.reflect.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;

/** Authenticated native menu ownership; no access to Core implementation classes. */
public final class SsnWindows {
    public static final Identifier ACTION=id("action"),UPDATE=id("update");
    private static final Set<Kind> BOUND=EnumSet.noneOf(Kind.class);
    private static final Map<AbstractContainerMenu,Owner> OWNERS=new IdentityHashMap<>();
    private static final Map<ServerPlayer,Owner> OPENING=new WeakHashMap<>();
    private static final ThreadLocal<Owner> ACTIVE=new ThreadLocal<>();
    private static final int CHUNK=24000,MAX_UPDATE=16*1024*1024;
    private static final class Owner {
        final ServerPlayer player;final AbstractContainerMenu menu;final int session;final Kind kind;final long opened;
        final DesktopServerSessionContext<AbstractContainerMenu,?> context;
        int ticks,sequence;byte[] previous;
        net.minecraft.world.item.ItemStack responseCursor;
        Owner(DesktopServerSessionContext<AbstractContainerMenu,?> c,Kind k){context=c;player=c.player();menu=c.menu();session=c.sessionId();kind=k;opened=player.level().getGameTime();}
    }
    private SsnWindows() { }
    public static Identifier id(String path){return Identifier.fromNamespaceAndPath("salts_inventory_compat","ssn/"+path);}
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}
    static synchronized void bind(){
        for(var kind:Kind.values()){
            var type=NativeMenuSupport.type("storagenetwork:"+kind.path);if(type==null||!BOUND.add(kind))continue;
            if(kind.remote){
                DesktopMenuBindings.register(type,menu->menu.slots.size(),true,(p,m,data)->{
                    var source=remoteSource(p,m);return source==null?"ssn-remote:"+p.getUUID()+":"+kind.path:"ssn-remote:"+p.getUUID()+":"+source.inventorySlot();
                },null,new DesktopMenuBindings.GesturePolicy(){@Override public boolean useNativeQuickMove(AbstractContainerMenu menu){return true;}});
                DesktopItemSources.register(type,SsnWindows::remoteSource);
            }else NativeMenuSupport.bindNative(type,true);
            DesktopMenuBindings.registerStorage(type,(menu,slot)->false);
            DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){
                @Override public boolean supports(ServerPlayer player,AbstractContainerMenu menu){return menu.getClass().getName().equals(kind.menu)&&menu.slots.size()==kind.slots+36;}
                @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu menu){return true;}
                @Override public void opened(DesktopServerSessionContext<AbstractContainerMenu,Void> c){var o=new Owner(c,kind);OWNERS.put(c.menu(),o);OPENING.put(c.player(),o);}
                @Override public void closed(DesktopServerSessionContext<AbstractContainerMenu,Void> c){var o=OWNERS.remove(c.menu());if(OPENING.get(c.player())==o)OPENING.remove(c.player());}
                @Override public void tick(DesktopServerSessionContext<AbstractContainerMenu,Void> c){
                    var o=OWNERS.get(c.menu());if(o==null||++o.ticks%20!=0||!kind.network)return;
                    // Keep other open views current after automation/other players change storage.
                    run(o,()->SsnPackets.receiveServer(c.player(),SsnPackets.newRefresh()));
                }
            });
            DesktopServerApi.registerOpeningData(type,(player,menu,data)->{if(kind.remote)data.writeByte(1);else data.writeBlockPos(position(menu));});
            DesktopServerApi.registerPayload(type,ACTION,c->{
                var owner=OWNERS.get(c.menu());if(owner==null)return;
                try{
                    var message=SsnPackets.decode(c.data(),c.player());
                    if(!SsnPackets.allowed(kind,message)){c.reject("Action does not belong to this SSN menu");return;}
                    SsnPackets.validate(kind,message,c.menu());
                    run(owner,()->SsnPackets.receiveServer(c.player(),message));c.broadcastChanges();
                }catch(IllegalArgumentException malformed){c.reject("Invalid SSN action: "+malformed.getMessage());}
            });
        }
    }
    private static void run(Owner owner,Runnable action){
        var previous=ACTIVE.get();var menu=owner.player.containerMenu;ACTIVE.set(owner);owner.player.containerMenu=owner.menu;
        var carried=owner.context.carriedStack();owner.responseCursor=null;owner.menu.setCarried(carried.copy());
        try{action.run();}finally{
            // Native broadcastChanges may already have cleared a detached menu's
            // cursor. The publisher's explicit response is the mutation result.
            owner.context.setCarriedStack(owner.responseCursor==null?carried:owner.responseCursor);owner.responseCursor=null;
            owner.player.containerMenu=menu;if(previous==null)ACTIVE.remove();else ACTIVE.set(previous);
        }
    }
    /** Called only by the optional publisher's own outbound transport. */
    public static boolean send(ServerPlayer player,Object message){
        if(!SsnPackets.clientMessage(message))return false;
        var owner=ACTIVE.get();
        if(owner==null||owner.player!=player){
            owner=DesktopSessions.activeNetworkMenu(player).map(OWNERS::get).orElse(null);
            if(owner==null)owner=OWNERS.get(player.containerMenu);
            if(owner==null){var opening=OPENING.get(player);if(opening!=null&&opening.opened==player.level().getGameTime())owner=opening;}
        }
        if(owner==null||owner.player!=player)return false;
        if(ACTIVE.get()==owner&&message.getClass().getSimpleName().equals("StackResponseClientMessage"))
            owner.responseCursor=((net.minecraft.world.item.ItemStack)field(message,"stack")).copy();
        byte[] bytes=SsnPackets.encode(message,player);
        if(message.getClass().getSimpleName().equals("StackRefreshClientMessage")){
            if(Arrays.equals(owner.previous,bytes))return true;owner.previous=bytes.clone();
        }
        if(bytes.length>MAX_UPDATE)throw new IllegalArgumentException("SSN update exceeds native transport bound");
        int sequence=++owner.sequence;
        for(int offset=0;offset<bytes.length;offset+=CHUNK){
            var b=new FriendlyByteBuf(Unpooled.buffer());
            try{b.writeInt(sequence);b.writeInt(bytes.length);b.writeInt(offset);b.writeBytes(bytes,offset,Math.min(CHUNK,bytes.length-offset));
                byte[] chunk=new byte[b.readableBytes()];b.readBytes(chunk);DesktopSessions.sendSessionPayload(player,owner.session,UPDATE,chunk);
            }finally{b.release();}
        }
        return true;
    }
    public static BlockPos position(AbstractContainerMenu menu){
        for(String name:List.of("tile","tileRequest")){
            Object tile=optionalField(menu,name);if(tile instanceof net.minecraft.world.level.block.entity.BlockEntity be)return be.getBlockPos();
        }
        throw new IllegalArgumentException("SSN menu lacks a native block position");
    }
    private static DesktopItemSources.Source remoteSource(net.minecraft.world.entity.player.Player player,AbstractContainerMenu menu){
        var remote=(net.minecraft.world.item.ItemStack)invoke(menu,"getRemote");
        for(int i=0;i<player.getInventory().getContainerSize();i++)if(player.getInventory().getItem(i)==remote){
            var item=remote.getItem();return new DesktopItemSources.Source(i,"ssn:"+player.getUUID()+":"+i,remote,s->s.is(item)&&s.getCount()==1);
        }
        return null;
    }
    public static AbstractContainerMenu create(Kind kind,int id,Inventory inventory,byte[] opening){
        try{
            var type=Class.forName(kind.menu);
            if(kind.remote){if(opening.length!=1||opening[0]!=1)throw new IllegalArgumentException("Invalid SSN remote opening marker");return(AbstractContainerMenu)type.getConstructor(int.class,Inventory.class).newInstance(id,inventory);}
            if(opening.length!=8)throw new IllegalArgumentException("Invalid SSN block opening data");
            var data=new FriendlyByteBuf(Unpooled.wrappedBuffer(opening));
            try{return(AbstractContainerMenu)type.getConstructor(int.class,Level.class,BlockPos.class,Inventory.class,net.minecraft.world.entity.player.Player.class)
                .newInstance(id,inventory.player.level(),data.readBlockPos(),inventory,inventory.player);}finally{data.release();}
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("SSN menu factory: "+kind.path,failure);}
    }
    public static Object optionalField(Object owner,String name){
        for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())try{var f=type.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}
        catch(NoSuchFieldException absent){}catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        return null;
    }
    public static Object field(Object owner,String name){var value=optionalField(owner,name);if(value==null)throw new IllegalStateException("Missing native SSN field: "+name);return value;}
    public static Object invoke(Object owner,String name,Object...args){
        for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())for(var m:type.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length)try{
            m.setAccessible(true);return m.invoke(owner,args);
        }catch(IllegalArgumentException mismatch){}catch(InvocationTargetException failure){
            if(failure.getCause()instanceof RuntimeException r)throw r;throw new IllegalStateException(name,failure.getCause());
        }catch(ReflectiveOperationException failure){throw new IllegalStateException(name,failure);}
        throw new IllegalStateException("Missing native SSN method: "+name);
    }
}
