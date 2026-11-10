package com.salts_inventory_update.compat.create;

import com.salts_inventory_update.api.server.desktop.*;
import io.netty.buffer.Unpooled;
import java.util.Set;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ServerboundCustomPayloadPacket;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Routes original, explicitly allowed native messages through their own receiver.
 * The desktop transport supplies the owning player/session; upstream still decodes,
 * validates permissions, and performs the action. No native packet is fabricated. */
public final class CreatePackets {
    private static final java.util.Map<AbstractContainerMenu,Runnable> REQUESTER_FINAL=new java.util.WeakHashMap<>();
    private CreatePackets() { }
    public static void afterMenuRemoved(AbstractContainerMenu menu) {var apply=REQUESTER_FINAL.remove(menu);if(apply!=null)apply.run();}
    static <T extends AbstractContainerMenu> void register(MenuType<T> type,String menu) {
        if(menu.startsWith("createbigcannons:")) {
            DesktopServerApi.registerNativePayload(type,new ResourceLocation("createbigcannons","network"),c->{
                final Object packet;
                try {packet=decodeCannon(c.data());}
                catch(ReflectiveOperationException|RuntimeException failure){c.reject("Invalid native cannon configuration");return;}
                if(packet==null){c.reject("This cannon packet does not belong to the open container");return;}
                DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->{
                    try {
                        // CBC's outer wrapper unconditionally queues another server
                        // task. Invoke the original decoded RootPacket in this
                        // authenticated scope so it sees its own containerMenu.
                        packet.getClass().getMethod("handle",java.util.concurrent.Executor.class,net.minecraft.network.PacketListener.class,net.minecraft.server.level.ServerPlayer.class)
                            .invoke(packet,(java.util.concurrent.Executor)Runnable::run,c.player().connection,c.player());
                    }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native cannon configuration handler",failure);}
                });c.broadcastChanges();
            });
            return;
        }
        if(menu.equals("railways:mounted_toolbox"))
            register(type,new ResourceLocation("railways","c2s"),data->{
                var b=buffer(data);try {int id=b.readVarInt();return id==0||id==1;}finally{b.release();}
            });
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
        if(!allowed.isEmpty())register(type,new ResourceLocation("create","main"),data->{
            var b=buffer(data);try {
                int id=b.readVarInt();Object[] packets=Class.forName("com.simibubi.create.AllPackets").getEnumConstants();
                return id>=0&&id<packets.length&&allowed.contains(((Enum<?>)packets[id]).name());
            }finally{b.release();}
        });
    }
    private static FriendlyByteBuf buffer(byte[] data){return new FriendlyByteBuf(Unpooled.wrappedBuffer(data));}
    private static Object decodeCannon(byte[] data) throws ReflectiveOperationException {
        var b=buffer(data);try {
            if(b.readVarInt()!=0)return null; // Original Fabric/Forge server wrapper.
            int rootId=b.readVarInt();
            Object packet=Class.forName("rbasamoyai.createbigcannons.network.CBCRootNetwork")
                .getMethod("constructPacket",FriendlyByteBuf.class,int.class).invoke(null,b,rootId);
            return packet.getClass().getName().equals("rbasamoyai.createbigcannons.network.ServerboundSetContainerValuePacket")&&!b.isReadable()?packet:null;
        }finally{b.release();}
    }
    @FunctionalInterface private interface Check {boolean accepts(byte[] data) throws ReflectiveOperationException;}
    private static <T extends AbstractContainerMenu> void register(MenuType<T> type,ResourceLocation channel,Check check) {
        DesktopServerApi.registerNativePayload(type,channel,c->{
            try {
                if(!check.accepts(c.data())){c.reject("This native action does not belong to this Create window");return;}
            }catch(ReflectiveOperationException|RuntimeException failure){
                c.reject("Invalid or unsupported native Create message");return;
            }
            Runnable receive=()->DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->{
                var b=buffer(c.data());try {
                    c.player().connection.handleCustomPayload(new ServerboundCustomPayloadPacket(channel,b));
                }finally{if(b.refCnt()>0)b.release();}
            });
            boolean requesterFinal=false;
            if(type==net.minecraft.core.registries.BuiltInRegistries.MENU.get(new ResourceLocation("create","redstone_requester"))) {
                var bytes=buffer(c.data());try {
                    int id=bytes.readVarInt();Object[] packets=Class.forName("com.simibubi.create.AllPackets").getEnumConstants();
                    requesterFinal=id>=0&&id<packets.length&&((Enum<?>)packets[id]).name().equals("CONFIGURE_REDSTONE_REQUESTER");
                }catch(ReflectiveOperationException failure){throw new IllegalStateException(failure);}finally{bytes.release();}
            }
            if(requesterFinal) {
                // The native requester menu writes its ghost rack on removal.
                // Vanilla receives the screen's final BE configuration after
                // that removal. Preserve that order or quantities revert to 1.
                // Its original receiver validates the BE and player distance;
                // it does not require a containerMenu or a surviving session.
                byte[] data=c.data().clone();var player=c.player();
                REQUESTER_FINAL.put(c.menu(),()->{
                    var b=buffer(data);try{player.connection.handleCustomPayload(new ServerboundCustomPayloadPacket(channel,b));}
                    finally{if(b.refCnt()>0)b.release();}
                });
            }else receive.run();
            c.broadcastChanges();
        });
    }
}
