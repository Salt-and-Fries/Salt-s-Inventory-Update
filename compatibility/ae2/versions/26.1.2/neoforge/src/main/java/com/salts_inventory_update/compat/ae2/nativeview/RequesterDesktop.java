package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.menu.AEBaseMenu;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.ae2.menu.MenuPacketScope;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.nbt.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.ItemStack;

/** Adapts the addon's screen-global packets without changing its request model. */
public final class RequesterDesktop {
    public static final Identifier SYNC=Identifier.fromNamespaceAndPath("salts_inventory_compat","requester/sync");
    private static final Identifier ACTION=Identifier.fromNamespaceAndPath("salts_inventory_compat","requester/action");
    private static final Map<AEBaseMenu,CompoundTag> LAST=Collections.synchronizedMap(new WeakHashMap<>());
    private RequesterDesktop() { }

    /** Finish the addon's custom background with its original inventory footer. */
    public static void extractContent(appeng.client.gui.AEBaseScreen<?> screen,net.minecraft.client.gui.GuiGraphicsExtractor graphics,int mouseX,int mouseY) {
        if(!supports(screen)||!screen.usesAeDesktopLayout()||screen.desktopInventoryCut()==0){
            screen.extractDesktopContent(graphics,mouseX,mouseY);return;
        }
        var body=screen.desktopContentBounds();var nativeGraphics=graphics;
        nativeGraphics.enableScissor(body.getX(),body.getY(),body.getX()+body.getWidth(),body.getY()+Math.max(0,body.getHeight()-4));
        try{screen.extractDesktopContent(nativeGraphics,mouseX,mouseY);}finally{nativeGraphics.disableScissor();}
        var footer=(net.minecraft.client.renderer.Rect2i)invoke(screen,"getFooterBounds");
        var texture=(net.minecraft.resources.Identifier)field(screen,"texture");
        var source=appeng.client.gui.style.Blitter.texture(texture,256,256).src(footer.getX(),footer.getY(),footer.getWidth(),footer.getHeight());
        com.salts_inventory_update.compat.ae2.nativeview.style.DesktopFrameEdges.bottom(nativeGraphics,source,Math.min(176,footer.getWidth()),body.getX(),body.getY()+body.getHeight()-4,body.getWidth());
    }
    private static boolean supports(Object menu){return menu.getClass().getName().startsWith("com.almostreliable.merequester.");}
    public static void register(MenuType<AEBaseMenu> type) {
        var id=net.minecraft.core.registries.BuiltInRegistries.MENU.getKey(type);
        // AE2's builder owns these registry IDs even though the addon supplies
        // the menu classes and custom packets.
        if(id==null||!Set.of("ae2:requester","ae2:requester_terminal","merequester:requester","merequester:requester_terminal","merequester:wireless_requester_terminal").contains(id.toString()))return;
        DesktopServerApi.registerNativePayload(type,ACTION,c->{
            if(!supports(c.menu())||!c.menu().stillValid(c.player()))return;
            var b=new net.minecraft.network.RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(c.data()),c.player().registryAccess());
            try {
                int kind=b.readUnsignedByte();long owner=b.readLong();int slot=b.readVarInt();
                if(slot<0)return;
                var tracker=invoke(c.menu(),"getRequestTracker",owner);
                if(tracker==null||((Number)invoke(tracker,"getId")).longValue()!=owner||slot>=((Number)invoke(invoke(tracker,"getServer"),"size")).intValue())return;
                if(kind==0){boolean enabled=b.readBoolean();if(b.isReadable())return;invoke(c.menu(),"updateRequesterState",owner,slot,enabled);}
                else if(kind==1){long amount=b.readLong(),batch=b.readLong();if(b.isReadable()||amount<0||batch<0)return;invoke(c.menu(),"updateRequesterNumbers",owner,slot,amount,batch);}
                else if(kind==2){var stack=ItemStack.OPTIONAL_STREAM_CODEC.decode(b);if(b.isReadable())return;invoke(c.menu(),"applyDragAndDrop",c.player(),slot,owner,stack);}
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
            var output=net.minecraft.world.level.storage.TagValueOutput.createWithContext(net.minecraft.util.ProblemReporter.DISCARDING,c.player().registryAccess());
            invoke(invoke(t,"getServer"),"serialize",output);var data=output.buildResult();
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
            // This publisher release changed updateFromMenu's third parameter
            // from NBT to a typed payload. Restore each native RequestManager
            // through its own ValueInput deserializer, including request status.
            if(data.getBooleanOr("reset",false))invoke(screen,"updateFromMenu",true,-1L,(Object)null);
            for(var entry:data.getListOrEmpty("entries")){
                var row=(CompoundTag)entry;var tag=row.getCompoundOrEmpty("data");
                var reference=invoke(screen,"getById",row.getLongOr("id",0L),tag.getStringOr("unique_name",""),tag.getLongOr("sort_by",0L));
                var input=net.minecraft.world.level.storage.TagValueInput.create(net.minecraft.util.ProblemReporter.DISCARDING,net.minecraft.client.Minecraft.getInstance().level.registryAccess(),tag);
                invoke(invoke(reference,"getRequestManager"),"deserialize",input);
                invoke(screen,"refreshList");
            }
        }finally{b.release();}
    }
    private static boolean send(int kind,long owner,int slot,java.util.function.Consumer<net.minecraft.network.RegistryFriendlyByteBuf> tail) {
        Object current=MenuPacketScope.current();if(!(current instanceof AEBaseMenu menu)||!supports(menu))return false;
        var b=new net.minecraft.network.RegistryFriendlyByteBuf(Unpooled.buffer(),net.minecraft.client.Minecraft.getInstance().level.registryAccess());try{b.writeByte(kind);b.writeLong(owner);b.writeVarInt(slot);tail.accept(b);byte[] bytes=new byte[b.readableBytes()];b.readBytes(bytes);AEWindow.sendAddonPayload(menu,ACTION,bytes);return true;}finally{b.release();}
    }
    public static boolean state(long id,int slot,boolean enabled){return send(0,id,slot,b->b.writeBoolean(enabled));}
    public static boolean numbers(long id,int slot,long amount,long batch){return send(1,id,slot,b->{b.writeLong(amount);b.writeLong(batch);});}
    public static boolean drop(long id,int slot,ItemStack stack){return send(2,id,slot,b->ItemStack.OPTIONAL_STREAM_CODEC.encode(b,stack));}
    /** Newer Requester releases send their records directly through NeoForge. */
    public static boolean routePacket(Object packet) {
        Object current=MenuPacketScope.current();
        if(!(current instanceof AEBaseMenu)||!supports(current))return false;
        if(packet instanceof appeng.core.network.ServerboundPacket action){
            NativeMenuPacketScope.send(action);return true;
        }
        if(packet.getClass().getName().equals("com.almostreliable.merequester.network.DragAndDropPacket")){
            return drop(((Number)invoke(packet,"requesterId")).longValue(),((Number)invoke(packet,"requestIndex")).intValue(),(ItemStack)invoke(packet,"item"));
        }
        if(packet.getClass().getName().equals("com.almostreliable.merequester.network.RequestUpdatePacket")){
            long owner=((Number)invoke(packet,"requesterId")).longValue();
            int slot=((Number)invoke(packet,"requestIndex")).intValue();
            return switch(String.valueOf(invoke(packet,"updateType"))){
                case "STATE"->state(owner,slot,(Boolean)invoke(packet,"state"));
                case "NUMBERS"->numbers(owner,slot,((Number)invoke(packet,"amount")).longValue(),((Number)invoke(packet,"batch")).longValue());
                default->false;
            };
        }
        return false;
    }
    private static Object field(Object owner,String name){for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())try{var f=t.getDeclaredField(name);f.setAccessible(true);return f.get(owner);}catch(NoSuchFieldException absent){}catch(ReflectiveOperationException e){throw new IllegalStateException(e);}return null;}
    private static Object invoke(Object owner,String name,Object...args){
        for(Class<?> t=owner.getClass();t!=null;t=t.getSuperclass())for(var m:t.getDeclaredMethods())if(m.getName().equals(name)&&m.getParameterCount()==args.length){try{m.setAccessible(true);return m.invoke(owner,args);}catch(ReflectiveOperationException e){throw new IllegalStateException("Unsupported native requester method "+name,e);}}
        throw new IllegalStateException("Missing native requester method "+name);
    }
}
