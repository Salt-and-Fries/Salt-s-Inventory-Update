package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.menu.AEBaseMenu;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.ae2.menu.MenuPacketScope;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Adapts the addon's screen-global packets without changing its request model. */
public final class RequesterDesktop {
    public static final ResourceLocation SYNC=new ResourceLocation("salts_inventory_compat","requester/sync");
    private static final ResourceLocation ACTION=new ResourceLocation("salts_inventory_compat","requester/action");
    private static final Map<AEBaseMenu,CompoundTag> LAST=Collections.synchronizedMap(new WeakHashMap<>());
    private RequesterDesktop() { }
    public static void bottom(appeng.client.gui.AEBaseScreen<?> screen,com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor graphics) {
        var footer=(net.minecraft.client.renderer.Rect2i)invoke(screen,"getFooterBounds");
        var texture=(ResourceLocation)field(screen,"texture");var body=screen.desktopContentBounds();
        int x=body.getX(),y=body.getY()+body.getHeight()-4,v=footer.getY()+footer.getHeight()-4;
        int sourceWidth=Math.min(176,footer.getWidth()),width=body.getWidth();var g=graphics.unwrap();
        g.blit(texture,x,y,footer.getX(),v,4,4);
        for(int dx=4;dx<width-4;){int part=Math.min(sourceWidth-8,width-4-dx);g.blit(texture,x+dx,y,footer.getX()+4,v,part,4);dx+=part;}
        g.blit(texture,x+width-4,y,footer.getX()+sourceWidth-4,v,4,4);
    }
    private static boolean supports(Object menu){return menu.getClass().getName().startsWith("com.almostreliable.merequester.");}
    public static void register(MenuType<AEBaseMenu> type) {
        var id=net.minecraft.core.registries.BuiltInRegistries.MENU.getKey(type);
        // AE2's builder owns these registry IDs even though the addon supplies
        // the menu classes and custom packets.
        if(id==null||!Set.of("ae2:requester","ae2:requester_terminal","merequester:requester","merequester:requester_terminal","merequester:wireless_requester_terminal").contains(id.toString()))return;
        DesktopServerApi.registerNativePayload(type,ACTION,c->{
            if(!supports(c.menu())||!c.menu().stillValid(c.player()))return;
            var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
            try {
                int kind=b.readUnsignedByte();long owner=b.readLong();int slot=b.readVarInt();
                if(slot<0)return;
                var tracker=invoke(c.menu(),"getRequestTracker",owner);
                if(tracker==null||((Number)invoke(tracker,"getId")).longValue()!=owner||slot>=((Number)invoke(invoke(tracker,"getServer"),"size")).intValue())return;
                if(kind==0){boolean enabled=b.readBoolean();if(b.isReadable())return;invoke(c.menu(),"updateRequesterState",owner,slot,enabled);}
                else if(kind==1){long batch=b.readLong(),stock=b.readLong();if(b.isReadable()||batch<0||stock<0)return;invoke(c.menu(),"updateRequesterNumbers",owner,slot,batch,stock);}
                else if(kind==2){var stack=b.readItem();if(b.isReadable())return;invoke(c.menu(),"applyDragAndDrop",c.player(),slot,owner,stack);}
                c.broadcastChanges();
            } finally { b.release(); }
        });
    }
    public static void update(DesktopServerSessionContext<AEBaseMenu,?> c) {
        if(!supports(c.menu()))return;
        var trackers=new ArrayList<Object>();
        Object own=field(c.menu(),"requestTracker");
        if(own!=null)trackers.add(own);
        Object all=field(c.menu(),"byId");if(all instanceof Map<?,?> map)trackers.addAll(map.values());
        var entries=new ListTag();
        for(Object t:trackers){
            var data=(CompoundTag)invoke(invoke(t,"getServer"),"serialize");
            data.putString("unique_name",(String)invoke(t,"getName"));data.putLong("sort_by",((Number)invoke(t,"getSortBy")).longValue());
            var entry=new CompoundTag();entry.putLong("id",((Number)invoke(t,"getId")).longValue());entry.put("data",data);entries.add(entry);
        }
        var snapshot=new CompoundTag();snapshot.put("entries",entries);
        if(snapshot.equals(LAST.get(c.menu())))return;
        // A terminal can see many requesters. Keep packets bounded per native
        // tracker rather than imposing a 32 KiB limit on the whole network.
        var packets=new ArrayList<byte[]>();var reset=new CompoundTag();reset.putBoolean("reset",true);packets.add(encode(reset));
        for(var entry:entries){var packet=new CompoundTag();var one=new ListTag();one.add(entry);packet.put("entries",one);packets.add(encode(packet));}
        for(var packet:packets)c.sendToClient(SYNC,packet);
        LAST.put(c.menu(),snapshot.copy());
    }
    private static byte[] encode(CompoundTag packet){var b=new FriendlyByteBuf(Unpooled.buffer());try{b.writeNbt(packet);byte[] bytes=new byte[b.readableBytes()];b.readBytes(bytes);if(bytes.length>32768)throw new IllegalStateException("Native requester data exceeds window payload limit");return bytes;}finally{b.release();}}
    public static void receive(Object screen,byte[] bytes) {
        if(!supports(screen))return;
        var b=new FriendlyByteBuf(Unpooled.wrappedBuffer(bytes));
        try {var data=b.readNbt();if(data==null||b.isReadable())throw new IllegalArgumentException("Invalid requester snapshot");
            if(data.getBoolean("reset"))invoke(screen,"updateFromMenu",true,-1L,new CompoundTag());
            for(var entry:data.getList("entries",Tag.TAG_COMPOUND)){var row=(CompoundTag)entry;invoke(screen,"updateFromMenu",false,row.getLong("id"),row.getCompound("data"));}
        }finally{b.release();}
    }
    private static boolean send(int kind,long owner,int slot,java.util.function.Consumer<FriendlyByteBuf> tail) {
        Object current=MenuPacketScope.current();if(!(current instanceof AEBaseMenu menu)||!supports(menu))return false;
        var b=new FriendlyByteBuf(Unpooled.buffer());try{b.writeByte(kind);b.writeLong(owner);b.writeVarInt(slot);tail.accept(b);byte[] bytes=new byte[b.readableBytes()];b.readBytes(bytes);AEWindow.sendAddonPayload(menu,ACTION,bytes);return true;}finally{b.release();}
    }
    public static boolean state(long id,int slot,boolean enabled){return send(0,id,slot,b->b.writeBoolean(enabled));}
    public static boolean numbers(long id,int slot,long batch,long stock){return send(1,id,slot,b->{b.writeLong(batch);b.writeLong(stock);});}
    public static boolean drop(long id,int slot,ItemStack stack){return send(2,id,slot,b->b.writeItem(stack));}
    private static Object field(Object owner,String name){for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}catch(NoSuchFieldException absent){}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}return null;}
    private static Object invoke(Object owner,String name,Object...args){
        for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())for(var m:t.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){try{m.setAccessible(true);return m.invoke(owner,args);}catch(ReflectiveOperationException e){throw new IllegalStateException("Unsupported native requester method "+name,e);}}
        throw new IllegalStateException("Missing native requester method "+name);
    }
}
