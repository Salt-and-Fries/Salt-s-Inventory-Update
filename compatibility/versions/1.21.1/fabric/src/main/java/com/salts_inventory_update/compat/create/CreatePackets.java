package com.salts_inventory_update.compat.create;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativePayloads;
import java.util.*;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.inventory.*;
public final class CreatePackets {
    static final net.minecraft.resources.ResourceLocation STOCK_CLOSE=net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","rename_item");
    private static final Map<AbstractContainerMenu,Runnable> REQUESTER_FINAL=new WeakHashMap<>();
    private CreatePackets() { }
    public static void afterMenuRemoved(AbstractContainerMenu menu){var apply=REQUESTER_FINAL.remove(menu);if(apply!=null)apply.run();}
    static <T extends AbstractContainerMenu> void register(MenuType<T> type,String menu){
        if(menu.startsWith("createbigcannons:")){cannons(type);return;}
        if(menu.equals("create:package_filter"))packageAddress(type);
        if(menu.equals("create:stock_keeper_request"))stockClose(type);
        Set<String> allowed=switch(menu) {
            case "create:toolbox","railways:mounted_toolbox"->Set.of("TOOLBOX_DISPOSE_ALL","TOOLBOX_EQUIP");
            case "create:schematicannon"->Set.of("CONFIGURE_SCHEMATICANNON");
            case "create:schematic_table"->Set.of("UPLOAD_SCHEMATIC","SYNC_SCHEMATIC");
            case "create:crafting_blueprint"->Set.of("CLEAR_CONTAINER","SUBMIT_GHOST_ITEM","BLUEPRINT_COMPLETE_RECIPE");
            case "create:filter","create:attribute_filter"->Set.of("CLEAR_CONTAINER","CONFIGURE_FILTER","SUBMIT_GHOST_ITEM");
            case "create:package_filter"->Set.of("CLEAR_CONTAINER","CONFIGURE_FILTER");
            case "create:linked_controller"->Set.of("CLEAR_CONTAINER","SUBMIT_GHOST_ITEM");
            case "create:schedule"->Set.of("CONFIGURE_SCHEDULE","SUBMIT_GHOST_ITEM");
            case "create:package_port"->Set.of("PACKAGE_PORT_CONFIGURATION");
            case "create:redstone_requester"->Set.of("CONFIGURE_REDSTONE_REQUESTER","SUBMIT_GHOST_ITEM");
            case "create:stock_keeper_category"->Set.of("CONFIGURE_STOCK_KEEPER_CATEGORIES","SUBMIT_GHOST_ITEM","REFUND_STOCK_KEEPER_CATEGORY");
            case "create:stock_keeper_request"->Set.of("LOGISTICS_STOCK_REQUEST","LOGISTICS_PACKAGE_REQUEST","REFUND_STOCK_KEEPER_CATEGORY","LOCK_STOCK_KEEPER","STOCK_KEEPER_HIDE_CATEGORY");
            case "create:factory_panel_set_item"->Set.of("SUBMIT_GHOST_ITEM");
            default->Set.of();
        };

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
    private static <T extends AbstractContainerMenu> void stockClose(MenuType<T> type){
        // Native removed() saves address and hidden categories with two packets.
        // Submit that one settings snapshot as one authenticated mutation, then
        // let both original receivers apply it in their original order.
        DesktopServerApi.registerNativePayload(type,STOCK_CLOSE,c->{
            // This registered native text intent carries the entire desired
            // settings value; it is safe to retain/coalesce until dispatch.
            var text=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(c.data()));
            final byte[] data;
            try{
                data=Base64.getDecoder().decode(text.readUtf(32767));
                if(text.isReadable())throw new IllegalArgumentException("Trailing stock settings text");
            }finally{text.release();}
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(data));
            final net.minecraft.core.BlockPos pos;final String address;final List<Integer> indices;
            try{
                pos=buffer.readBlockPos();address=buffer.readUtf(32767);
                int count=buffer.readVarInt();
                if(count<0||count>4096)throw new IllegalArgumentException("Invalid hidden category count");
                indices=new ArrayList<>(count);
                for(int i=0;i<count;i++)indices.add(buffer.readVarInt());
                if(buffer.isReadable())throw new IllegalArgumentException("Trailing stock settings data");
            }finally{buffer.release();}
            Object holder=CreateOwners.field(c.menu(),"contentHolder");
            if(!(holder instanceof net.minecraft.world.level.block.entity.BlockEntity owner)||!owner.getBlockPos().equals(pos)){
                c.reject("Stock settings do not belong to this menu");return;
            }
            Object categories=CreateOwners.field(holder,"categories");
            if(!(categories instanceof List<?> list)||indices.size()>list.size()+1||new HashSet<>(indices).size()!=indices.size()
                ||indices.stream().anyMatch(i->i< -1||i>=list.size())){
                c.reject("Invalid stock category indices");return;
            }
            try{
                var orderClass=Class.forName("com.simibubi.create.content.logistics.stockTicker.PackageOrderWithCrafts");
                Object order=orderClass.getMethod("empty").invoke(null);
                Object addressPacket=Class.forName("com.simibubi.create.content.logistics.stockTicker.PackageOrderRequestPacket")
                    .getConstructor(net.minecraft.core.BlockPos.class,orderClass,String.class,boolean.class).newInstance(pos,order,address,false);
                Object categoriesPacket=Class.forName("com.simibubi.create.content.logistics.stockTicker.StockKeeperCategoryHidingPacket")
                    .getConstructor(net.minecraft.core.BlockPos.class,List.class).newInstance(pos,indices);
                DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->{
                    NativePayloads.dispatch(addressPacket,c.player());NativePayloads.dispatch(categoriesPacket,c.player());
                });c.broadcastChanges();
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native stock settings contract",failure);}
        });
    }
    private static <T extends AbstractContainerMenu> void packageAddress(MenuType<T> type){
        // Complete address values use the existing bounded text-intent lane.
        // The original Create packet remains responsible for applying them.
        DesktopServerApi.registerNativePayload(type,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("minecraft","rename_item"),c->{
            var buffer=new net.minecraft.network.FriendlyByteBuf(io.netty.buffer.Unpooled.wrappedBuffer(c.data()));
            final String address;
            try{
                address=buffer.readUtf(32767);
                if(buffer.isReadable())throw new IllegalArgumentException("Trailing native package address data");
            }finally{buffer.release();}
            try{
                var optionClass=Class.forName("com.simibubi.create.content.logistics.filter.FilterScreenPacket$Option");
                @SuppressWarnings({"rawtypes","unchecked"})
                Object option=Enum.valueOf((Class)optionClass,"UPDATE_ADDRESS");
                var data=new net.minecraft.nbt.CompoundTag();data.putString("Address",address);
                var packet=Class.forName("com.simibubi.create.content.logistics.filter.FilterScreenPacket")
                    .getConstructor(optionClass,net.minecraft.nbt.CompoundTag.class).newInstance(option,data);
                DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->NativePayloads.dispatch(packet,c.player()));
                c.broadcastChanges();
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native package address contract",failure);}
        });
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
