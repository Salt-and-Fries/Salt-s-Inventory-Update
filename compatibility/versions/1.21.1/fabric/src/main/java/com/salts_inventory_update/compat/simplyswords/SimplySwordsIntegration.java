package com.salts_inventory_update.compat.simplyswords;

import java.lang.reflect.Field;
import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.*;
import com.salts_inventory_update.compat.nativewindows.NativeMenuSupport;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

/** The original upstream handler owns acceptance, preview synchronization, editing and commit/refund behavior. */
public final class SimplySwordsIntegration {
    public static final String MENU="net.sweenus.simplyswords.screen.RunicForgeScreenHandler";
    private static boolean bound;
    private static Field forgePos;
    private SimplySwordsIntegration() { }
    public static AbstractContainerMenu create(int id,Inventory inventory,BlockPos pos) {
        try{return (AbstractContainerMenu)Class.forName(MENU).getConstructor(int.class,Inventory.class,BlockPos.class).newInstance(id,inventory,pos);}
        catch(ReflectiveOperationException e){throw new IllegalStateException("Simply Swords menu construction failed",e);}
    }
    static BlockPos position(AbstractContainerMenu menu) {
        try{
            if(forgePos==null){forgePos=menu.getClass().getDeclaredField("forgePos");forgePos.setAccessible(true);}
            return (BlockPos)forgePos.get(menu);
        }catch(ReflectiveOperationException e){throw new IllegalStateException("Simply Swords source unavailable",e);}
    }
    static synchronized void bindings() {
        if(bound)return;var type=NativeMenuSupport.type("simplyswords:runic_forge");if(type==null)return;
        DesktopMenuBindings.register(type,new DesktopMenuBindings.SlotAccess(){
            @Override public int size(AbstractContainerMenu menu){return menu.slots.size();}
            @Override public boolean isVirtualSlot(AbstractContainerMenu menu,Slot slot){return slot.index==47;}
        },true,(player,menu,data)->DesktopSessions.blockSourceKey(player,position(menu)),null,new DesktopMenuBindings.GesturePolicy(){
            @Override public boolean useNativeQuickMove(AbstractContainerMenu menu){return true;}
            @Override public boolean allowsTargetedQuickMoveSource(AbstractContainerMenu menu,Slot slot){return false;}
        });bound=true;
    }
    public static void initializeCommon() {
        DesktopEnvironment.onServerStarting(server->DesktopEnvironment.registerIntegration("simplyswords",()->{
            bindings();var type=NativeMenuSupport.type("simplyswords:runic_forge");
            if(type==null||DesktopServerApi.findWindowHandler(type)!=null)return;
            DesktopServerApi.registerOpeningData(type,(player,menu,buffer)->buffer.writeBlockPos(position(menu)));
            DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>(){
                @Override public boolean supports(ServerPlayer player,AbstractContainerMenu menu){
                    if(!menu.getClass().getName().equals(MENU)||menu.slots.size()!=48)return false;
                    for(int i=0;i<11;i++)if(menu.getSlot(i).x!=SimplySwordsLayout.slotX(i)||menu.getSlot(i).y!=SimplySwordsLayout.slotY(i))return false;
                    return menu.stillValid(player);
                }
                @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu menu){return true;}
            });
        }));
    }
    /** Scoped to the upstream block's own interaction; the native opener remains the fallback. */
    public static boolean open(ServerPlayer player,BlockPos pos) {
        if(!DesktopSessions.shouldCapture(player))return false;
        return DesktopSessions.openMenuSession(player,new SimpleMenuProvider((id,inventory,owner)->create(id,inventory,pos),
            Component.translatable("container.simplyswords.runic_forge")),buffer->buffer.writeBlockPos(pos))!=null;
    }
}
