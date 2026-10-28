package com.salts_inventory_update.compat.chipped;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;

/** Selection and crafting are authenticated against this window's original native menu. */
public final class ChippedIntegration {
    public static final ResourceLocation ACTION=ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","chipped/action");
    public static final ResourceLocation STATE=ResourceLocation.fromNamespaceAndPath("salts_inventory_compat","chipped/state");
    private static boolean bindings;
    public static final class ServerState { int selected=-1;ItemStack expected=ItemStack.EMPTY; }
    private ChippedIntegration() { }
    static synchronized void bindings() {
        if(bindings)return;var type=NativeMenuSupport.type("chipped:workbench");if(type==null)return;
        DesktopMenuBindings.register(type,menu->menu.slots.size(),true,(player,menu,data)->{
            var source=ChippedSources.get(menu);
            return source==null?null:DesktopSessions.blockSourceKey(player,source.pos());
        },null,new DesktopMenuBindings.GesturePolicy(){
            @Override public boolean allowsTargetedQuickMoveSource(AbstractContainerMenu m,net.minecraft.world.inventory.Slot s){return false;}
            @Override public boolean allowsPickupAll(AbstractContainerMenu m,ItemStack stack,net.minecraft.world.inventory.Slot s){return false;}
            @Override public boolean canQuickCraft(AbstractContainerMenu m,net.minecraft.world.inventory.Slot s,ItemStack stack){return false;}
        });
        bindings=true;
    }
    public static boolean open(ServerPlayer player,net.minecraft.core.BlockPos pos,net.minecraft.network.chat.Component title) {
        if(!DesktopSessions.shouldCapture(player))return false;
        var provider=new net.minecraft.world.SimpleMenuProvider((id,inventory,user)->{
            ChippedSources.begin(player,pos);
            try {
                return (AbstractContainerMenu)Class.forName(ChippedNative.MENU).getConstructor(int.class,net.minecraft.world.entity.player.Inventory.class)
                    .newInstance(id,inventory);
            }catch(ReflectiveOperationException failure){throw new IllegalStateException("Native Chipped menu construction failed",failure);}
            finally{ChippedSources.end();}
        },title);
        return DesktopSessions.openMenuSession(player,provider,(buffer)->buffer.writeBlockPos(pos))!=null;
    }
    public static void initializeCommon() {
        DesktopEnvironment.onServerStarting(server->DesktopEnvironment.registerIntegration("chipped",()->{
            bindings();var type=NativeMenuSupport.type("chipped:workbench");if(type==null||DesktopServerApi.findWindowHandler(type)!=null)return;
            DesktopServerApi.registerOpeningData(type,(player,menu,buffer)->{
                var source=ChippedSources.get(menu);if(source==null)throw new IllegalStateException("Missing Chipped source");
                buffer.writeBlockPos(source.pos());
            });
            DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,ServerState>(){
                @Override public boolean supports(ServerPlayer p,AbstractContainerMenu m){var s=ChippedSources.get(m);return s!=null&&s.valid(p);}
                @Override public ServerState createState(DesktopServerSessionContext<AbstractContainerMenu,ServerState> c){return new ServerState();}
                @Override public void tick(DesktopServerSessionContext<AbstractContainerMenu,ServerState> c){
                    var s=c.state();if(s.selected>=0&&!ItemStack.matches(s.expected,c.player().getInventory().getItem(s.selected)))reset(c,1);
                }
            });
            DesktopServerApi.registerPayload(type,ACTION,c->{
                if(!(c.state() instanceof ServerState s)||c.data().length>8192||c.data().length<1)return;
                var source=ChippedSources.get(c.menu());if(source==null||!source.valid(c.player()))return;
                var b=new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(c.data()),c.player().registryAccess());
                try {
                    int op=b.readUnsignedByte();
                    if(op==0) {
                        int index=b.readVarInt();var expected=ItemStack.OPTIONAL_STREAM_CODEC.decode(b);
                        if(b.isReadable()||index<0||index>=36||!ItemStack.matches(expected,c.player().getInventory().getItem(index))
                            ||!ChippedNative.eligible(c.menu(),expected)){reset(c,1);return;}
                        s.selected=index;s.expected=expected.copy();ChippedNative.select(c.menu(),index);ChippedNative.filter(c.menu(),null);reply(c,0);
                    } else if(op==1) {
                        boolean all=b.readBoolean();var choice=ItemStack.OPTIONAL_STREAM_CODEC.decode(b);
                        if(b.isReadable()||s.selected<0||!ItemStack.matches(s.expected,c.player().getInventory().getItem(s.selected))
                            ||!ChippedNative.eligible(c.menu(),s.expected)){reset(c,1);return;}
                        ChippedNative.select(c.menu(),s.selected);ChippedNative.filter(c.menu(),null);
                        var allowed=ChippedNative.results(c.menu()).stream().filter(v->ItemStack.isSameItemSameComponents(v,choice)).findFirst();
                        if(allowed.isEmpty()){reset(c,1);return;}
                        ChippedNative.craft(c.menu(),allowed.get().copyWithCount(1),all);
                        c.broadcastChanges();reset(c,2);
                    }
                } catch(IndexOutOfBoundsException|IllegalArgumentException malformed) { reset(c,1); }
                finally { b.release(); }
            });
        }));
    }
    private static void reset(DesktopServerSessionContext<AbstractContainerMenu,?> c,int status) {
        var s=(ServerState)c.state();s.selected=-1;s.expected=ItemStack.EMPTY;ChippedNative.reset(c.menu());reply(c,status);
    }
    private static void reply(DesktopServerSessionContext<AbstractContainerMenu,?> c,int status) {
        var s=(ServerState)c.state();var b=new RegistryFriendlyByteBuf(Unpooled.buffer(),c.player().registryAccess());
        try { b.writeByte(status);b.writeVarInt(s.selected);ItemStack.OPTIONAL_STREAM_CODEC.encode(b,s.expected);byte[] data=new byte[b.readableBytes()];b.readBytes(data);c.sendToClient(STATE,data); }
        finally { b.release(); }
    }
}
