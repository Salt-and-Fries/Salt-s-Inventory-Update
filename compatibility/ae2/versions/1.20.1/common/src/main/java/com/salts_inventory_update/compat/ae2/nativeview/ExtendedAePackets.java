package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.menu.AEBaseMenu;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.ae2.menu.MenuPacketScope;
import io.netty.buffer.Unpooled;
import java.util.*;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.MenuType;

/** Original addon codecs and handlers, addressed to one authenticated desktop lifetime. */
public final class ExtendedAePackets {
    public static final ResourceLocation ACTION=new ResourceLocation("salts_inventory_compat","extended_ae/action");
    public static final ResourceLocation SYNC=new ResourceLocation("salts_inventory_compat","extended_ae/sync");
    private record Route(ServerPlayer player,int session) { }
    private static final Map<AEBaseMenu,Route> ROUTES=Collections.synchronizedMap(new WeakHashMap<>());
    private static final ThreadLocal<AEBaseMenu> SENDER=new ThreadLocal<>();
    private ExtendedAePackets() { }
    /** The addon has its own Pattern Access screen, rather than AE2's subclass. */
    public static boolean applyNativeUpdate(appeng.core.sync.BasePacket packet,Object screen){
        if(!supports(screen))return false;
        String name=screen.getClass().getSimpleName();
        if(!(name.contains("PatternTerminal")||name.contains("WirelessExPAT")||name.contains("UWirelessExPAT")))return false;
        if(packet instanceof appeng.core.sync.packets.PatternAccessTerminalPacket update){
            if(update.siu$fullUpdate())invoke(screen,"postFullUpdate",update.siu$inventoryId(),update.siu$sortBy(),update.siu$group(),update.siu$inventorySize(),update.siu$slots());
            else invoke(screen,"postIncrementalUpdate",update.siu$inventoryId(),update.siu$slots());
            return true;
        }
        if(packet instanceof appeng.core.sync.packets.ClearPatternAccessTerminalPacket){invoke(screen,"clear");return true;}
        return false;
    }
    public static boolean supports(Object owner) {String n=owner.getClass().getName();return n.startsWith("com.github.glodblock.extendedae.")||n.startsWith("com.glodblock.github.extendedae.");}
    private static boolean fabric(Object owner){return owner.getClass().getName().startsWith("com.github.glodblock.extendedae.");}
    private static String packetClass(Object owner,int kind,boolean client){
        String prefix=fabric(owner)?"com.github.glodblock.extendedae":"com.glodblock.github.extendedae";
        if(kind==0)return (fabric(owner)?prefix:"com.glodblock.github.glodium")+".network.packet."+(client?"SGenericPacket":"CGenericPacket");
        return prefix+".network.packet."+(client?(kind==1?"SExPatternInfo":"SAssemblerMatrixUpdate"):"CUpdatePage");
    }
    public static void register(MenuType<AEBaseMenu> type){
        // AE2 owns the registry namespace, so eligibility is checked per live
        // model below rather than by assuming an extendedae: menu namespace.
        DesktopServerApi.registerPayload(type,ACTION,request->{
            var menu=request.menu();if(!supports(menu)||!menu.stillValid(request.player())){request.reject("Invalid Extended AE window");return;}
            var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(request.data()));
            try{
                int kind=b.readUnsignedByte();if(kind>1)throw new IllegalArgumentException("Unknown Extended AE action");
                Object packet=newPacket(menu,kind,false);invoke(packet,"fromBytes",b);
                if(b.isReadable())throw new IllegalArgumentException("Trailing Extended AE action data");
                if(kind==0){String name=(String)field(packet,"name");if(!actions(menu).containsKey(name))throw new IllegalArgumentException("Unknown native Extended AE action");}
                else{int page=((Number)field(packet,"page")).intValue();int max=menu.getClass().getSimpleName().equals("ContainerPatternModifier")?2:menu.getClass().getSimpleName().equals("ContainerExMolecularAssembler")||menu.getClass().getSimpleName().equals("ContainerExInscriber")?3:1;if(page<0||page>max)throw new IllegalArgumentException("Invalid native page");}
                var player=request.player();var previous=player.containerMenu;menu.setCarried(request.carriedStack());player.containerMenu=menu;
                try{scoped(menu,()->DesktopSessions.withSessionTransition(player,request.sessionId(),()->invoke(packet,"onMessage",player)));}
                finally{try{if(DesktopSessions.hasOpenSessionMatching(player,candidate->candidate==menu))request.setCarriedStack(menu.getCarried());else menu.setCarried(player.inventoryMenu.getCarried().copy());request.broadcastChanges();}finally{if(player.containerMenu==menu)player.containerMenu=previous;}}
            }finally{b.release();}
        });
    }
    public static void opened(DesktopServerSessionContext<AEBaseMenu,?> c){if(supports(c.menu())){ROUTES.put(c.menu(),new Route(c.player(),c.sessionId()));scoped(c.menu(),()->{var update=actions(c.menu()).get("update");if(update!=null)update.accept(null);});}}
    public static void closed(AEBaseMenu menu){ROUTES.remove(menu);}
    public static void tick(AEBaseMenu menu){if(supports(menu)){var show=actions(menu).get("show");if(show!=null)scoped(menu,()->show.accept(null));}}
    public static void scoped(Object owner,Runnable run){
        if(!(owner instanceof AEBaseMenu menu)||!supports(menu)){run.run();return;}
        var previous=SENDER.get();SENDER.set(menu);try{run.run();}finally{if(previous==null)SENDER.remove();else SENDER.set(previous);}
    }
    public static boolean clientSend(Object packet){
        Object current=MenuPacketScope.current();if(!(current instanceof AEBaseMenu menu)||!supports(menu)||menu.desktopSyncId()==null)return false;
        String name=packet.getClass().getSimpleName();int kind=name.equals("CGenericPacket")?0:name.equals("CUpdatePage")?1:-1;if(kind<0)return false;
        // These are native screen refresh requests, not user actions. Initial
        // values arrive through the server's original broadcast/update flow.
        if(kind==0&&Set.of("show","update").contains(String.valueOf(field(packet,"name"))))return true;
        byte[] bytes=encode(packet,kind);if(bytes.length>32768)throw new IllegalArgumentException("Extended AE action too large");AEWindow.sendAddonPayload(menu,ACTION,bytes);return true;
    }
    public static boolean serverSend(Object packet,ServerPlayer player){
        var menu=SENDER.get();
        if(menu==null){var active=DesktopSessions.activeNetworkMenu(player).orElse(null);if(active instanceof AEBaseMenu ae)menu=ae;}
        if(menu==null||!supports(menu))return false;var route=ROUTES.get(menu);
        if(route==null||route.player()!=player||!menu.stillValid(player))return false;
        int kind=switch(packet.getClass().getSimpleName()){case"SGenericPacket"->0;case"SExPatternInfo"->1;case"SAssemblerMatrixUpdate"->2;default->-1;};if(kind<0)return false;
        byte[] data=encode(packet,kind);if(data.length>2*1024*1024)throw new IllegalArgumentException("Extended AE update too large");
        UUID message=UUID.randomUUID();int count=(data.length+29999)/30000;
        for(int i=0;i<count;i++){var b=new FriendlyByteBuf(Unpooled.buffer());try{b.writeUUID(message);b.writeVarInt(i);b.writeVarInt(count);b.writeVarInt(data.length);b.writeBytes(data,i*30000,Math.min(30000,data.length-i*30000));byte[] part=new byte[b.readableBytes()];b.readBytes(part);DesktopSessions.sendSessionPayload(player,route.session(),SYNC,part);}finally{b.release();}}
        return true;
    }
    private record Pending(int count,int size,byte[][] parts,long started){ }
    private static final Map<Object,Map<UUID,Pending>> PARTS=new WeakHashMap<>();
    public static void receive(Object screen,byte[] bytes){
        if(!supports(screen))return;var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try{
            UUID id=b.readUUID();int index=b.readVarInt(),count=b.readVarInt(),size=b.readVarInt();if(size<=0||size>2*1024*1024||count!=(size+29999)/30000||index<0||index>=count||b.readableBytes()!=Math.min(30000,size-index*30000))throw new IllegalArgumentException("Invalid Extended AE update fragment");
            var queue=PARTS.computeIfAbsent(screen,k->new LinkedHashMap<>());queue.values().removeIf(p->System.currentTimeMillis()-p.started()>10000);if(queue.size()>=8&&!queue.containsKey(id))throw new IllegalArgumentException("Too many Extended AE updates");
            var pending=queue.computeIfAbsent(id,k->new Pending(count,size,new byte[count][],System.currentTimeMillis()));if(pending.count()!=count||pending.size()!=size||pending.parts()[index]!=null)throw new IllegalArgumentException("Duplicate Extended AE update fragment");
            pending.parts()[index]=new byte[b.readableBytes()];b.readBytes(pending.parts()[index]);if(Arrays.stream(pending.parts()).anyMatch(Objects::isNull))return;
            byte[] data=new byte[size];int off=0;for(byte[] p:pending.parts()){System.arraycopy(p,0,data,off,p.length);off+=p.length;}queue.remove(id);decode(screen,data);
        }finally{b.release();}
    }
    private static void decode(Object screen,byte[] data){
        var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(data));try{int kind=b.readUnsignedByte();if(kind>2)throw new IllegalArgumentException("Unknown Extended AE update");Object packet=newPacket(screen,kind,true);invoke(packet,"fromBytes",b);if(b.isReadable())throw new IllegalArgumentException("Trailing Extended AE update data");
            if(kind==0){var action=actions(screen).get(field(packet,"name"));if(action!=null){
                Object parameters=optionalField(packet,"paras");
                if(!fabric(screen))try{parameters=Class.forName("com.glodblock.github.glodium.network.packet.sync.Paras").getConstructor(Object[].class).newInstance(new Object[]{parameters});}
                catch(ReflectiveOperationException error){throw new IllegalStateException("Native Glodium parameters",error);}
                action.accept(parameters);
            }}
            else if(kind==1){Object face=optionalField(packet,"face");if(fabric(screen))invoke(screen,"postTileInfo",field(packet,"id"),field(packet,"pos"),field(packet,"dim"));else invoke(screen,"postTileInfo",field(packet,"id"),field(packet,"pos"),field(packet,"dim"),face);}
            else invoke(screen,"receiveUpdate",field(packet,"patternID"),field(packet,"updateMap"));
        }finally{b.release();}
    }
    private static Object newPacket(Object owner,int kind,boolean client){try{return Class.forName(packetClass(owner,kind,client)).getConstructor().newInstance();}catch(ReflectiveOperationException e){throw new IllegalStateException("Unsupported Extended AE packet",e);}}
    private static byte[] encode(Object packet,int kind){var b=new FriendlyByteBuf(Unpooled.buffer());try{b.writeByte(kind);invoke(packet,"toBytes",b);byte[] data=new byte[b.readableBytes()];b.readBytes(data);return data;}finally{b.release();}}
    @SuppressWarnings("unchecked") private static Map<Object,Consumer<Object>> actions(Object owner){Object map=optionalInvoke(owner,"getActionMap");return map instanceof Map<?,?>?(Map<Object,Consumer<Object>>)map:Map.of();}
    public static Object field(Object owner,String name){Object value=optionalField(owner,name);if(value==null)throw new IllegalStateException("Missing Extended AE field "+name);return value;}
    public static Object optionalField(Object owner,String name){for(Class<?> c=owner.getClass();c!=null;c=c.getSuperclass())try{var f=c.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}catch(NoSuchFieldException ignored){}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}return null;}
    public static Object invoke(Object owner,String name,Object...args){Object result=call(owner,name,args,false);return result;}
    private static Object optionalInvoke(Object owner,String name,Object...args){return call(owner,name,args,true);}
    private static Object call(Object owner,String name,Object[]args,boolean optional){for(Class<?> c=owner.getClass();c!=null;c=c.getSuperclass())for(var m:c.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length)try{m.setAccessible(true);return m.invoke(owner,args);}catch(ReflectiveOperationException e){throw new IllegalStateException("Native Extended AE callback "+name,e);}if(optional)return null;throw new IllegalStateException("Missing Extended AE callback "+name);}
}
