package com.salts_inventory_update.compat.minecells;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.ForeignMenuAccess;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import io.netty.buffer.Unpooled;
import java.lang.reflect.InvocationTargetException;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.entity.BlockEntity;

/** The original Cell Crafter menu and actions, addressed to their owning window. */
public final class MineCellsWindows {
    public static final String MENU="com.github.mim1q.minecells.screen.cellcrafter.CellCrafterScreenHandler";
    static final ResourceLocation SYNC=id("salts_inventory_compat","minecells/crafter_state");
    private static final ResourceLocation CRAFT=id("minecells","cell_crafter_craft_request");
    private static final ResourceLocation REQUEST=id("minecells","request_unlocked_cell_crafter_recipes");
    private static boolean bound;
    private MineCellsWindows() { }
    static ResourceLocation id(String namespace,String path){return new ResourceLocation(namespace,path);}
    public static void initializeCommon(){bind();DesktopEnvironment.onServerStarting(server->bind());}
    private static synchronized void bind(){
        var type=NativeMenuSupport.type("minecells:cell_forge");if(type==null||bound)return;
        NativeMenuSupport.bindNative(type,false);
        DesktopMenuBindings.registerStorage(type,(menu,slot)->false);
        DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){
            @Override public boolean supports(ServerPlayer player,AbstractContainerMenu menu){
                if(!menu.getClass().getName().equals(MENU)||!valid(menu,player))return false;
                attach(menu,player);return true;
            }
            @Override public void opened(DesktopServerSessionContext<AbstractContainerMenu,Void> c){sync(c);}
        });
        DesktopServerApi.registerNativePayload(type,REQUEST,c->{
            if(c.data().length!=4){c.reject("Invalid Cell Crafter recipe request");return;}
            var data=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
            try{if(data.readInt()!=c.player().getId()){c.reject("Wrong Cell Crafter player");return;}}
            finally{data.release();}
            sync(c);
        });
        DesktopServerApi.registerNativePayload(type,CRAFT,c->{
            if(c.data().length>512||!valid(c.menu(),c.player())){c.reject("Invalid Cell Crafter owner");return;}
            var data=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
            BlockPos pos;ResourceLocation recipe;
            try{
                pos=data.readBlockPos();recipe=data.readResourceLocation();
                if(data.isReadable()||!pos.equals(entity(c.menu()).getBlockPos())){c.reject("Wrong Cell Crafter block");return;}
            }catch(RuntimeException malformed){c.reject("Invalid Cell Crafter action");return;}
            finally{data.release();}
            // Reuse the original server action: ingredient checks, consumption,
            // cooldown and the block's queued world output remain native.
            try{
                var action=Class.forName("com.github.mim1q.minecells.network.ServerPacketHandler").getDeclaredMethod(
                    "lambda$init$4",ServerPlayer.class,BlockPos.class,MinecraftServer.class,ResourceLocation.class);
                action.setAccessible(true);action.invoke(null,c.player(),pos,c.player().server,recipe);
            }catch(ReflectiveOperationException failure){throw upstream("Cell Crafter crafting",failure);}
            c.broadcastChanges();
        });
        bound=true;
    }
    private static void sync(DesktopServerSessionContext<AbstractContainerMenu,?> c){
        if(!valid(c.menu(),c.player()))return;
        FriendlyByteBuf original=null;var data=new FriendlyByteBuf(Unpooled.buffer());
        try{
            original=(FriendlyByteBuf)Class.forName("com.github.mim1q.minecells.network.s2c.SendUnlockedCellCrafterRecipesS2CPacket")
                .getConstructor(ServerPlayer.class).newInstance(c.player());
            data.writeBlockPos(entity(c.menu()).getBlockPos());data.writeBytes(original,original.readerIndex(),original.readableBytes());
            byte[] bytes=new byte[data.readableBytes()];data.getBytes(data.readerIndex(),bytes);c.sendToClient(SYNC,bytes);
        }catch(ReflectiveOperationException failure){throw upstream("Cell Crafter recipe synchronization",failure);}
        finally{if(original!=null)original.release();data.release();}
    }
    private static BlockEntity entity(AbstractContainerMenu menu){return field(menu,"entity") instanceof BlockEntity entity?entity:null;}
    private static boolean valid(AbstractContainerMenu menu,ServerPlayer player){
        var entity=entity(menu);
        return entity!=null&&!entity.isRemoved()&&entity.getLevel()==player.level()
            &&player.level().getBlockEntity(entity.getBlockPos())==entity;
    }
    static void attach(AbstractContainerMenu menu,Player player){
        ForeignMenuAccess.invoke(menu,"owo$attachToPlayer",new Class<?>[]{Player.class},player);
    }
    /** Only the optional mod/owo object's own fields are reflected. */
    public static Object field(Object owner,String name){
        if(owner==null)return null;
        for(Class<?> type=owner.getClass();type!=null;type=type.getSuperclass())try{
            var field=type.getDeclaredField(name);field.setAccessible(true);return field.get(owner);
        }catch(NoSuchFieldException next){}catch(ReflectiveOperationException failure){throw upstream(name,failure);}
        return null;
    }
    public static boolean ownsProperties(AbstractContainerMenu menu){
        if(!menu.getClass().getName().equals(MENU))return false;
        Object player=ForeignMenuAccess.invoke(menu,"player");
        return player instanceof ServerPlayer p&&DesktopSessions.hasOpenSessionMatching(p,m->m==menu);
    }
    static IllegalStateException upstream(String message,ReflectiveOperationException failure){
        return new IllegalStateException(message,failure instanceof InvocationTargetException invocation?invocation.getCause():failure);
    }
}
