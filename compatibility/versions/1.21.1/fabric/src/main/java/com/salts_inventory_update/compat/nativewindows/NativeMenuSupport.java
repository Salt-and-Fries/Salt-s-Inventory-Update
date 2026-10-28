package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.desktop.DesktopMenuBindings;
import com.salts_inventory_update.api.server.desktop.*;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.*;

/** Shared public-API registration helpers; does not expose or call Core implementation. */
public final class NativeMenuSupport {
    private static final Set<MenuType<?>> BOUND=Collections.newSetFromMap(new IdentityHashMap<>());
    private NativeMenuSupport() { }
    @SuppressWarnings("unchecked") public static MenuType<AbstractContainerMenu> type(String id) {
        var key=ResourceLocation.parse(id);
        return BuiltInRegistries.MENU.containsKey(key)?(MenuType<AbstractContainerMenu>)BuiltInRegistries.MENU.get(key):null;
    }
    public static void bindNative(MenuType<?> type,boolean openingData) {
        bindNative(type,openingData,menu->menu.slots.size(),null);
    }
    public static void bindNative(MenuType<?> type,boolean openingData,DesktopMenuBindings.SlotAccess slots,
            DesktopMenuBindings.SnapshotPreparer prepare) {
        if(type==null)return;
        if(!BOUND.add(type))return;
        DesktopMenuBindings.register(type,slots,openingData,null,prepare,new DesktopMenuBindings.GesturePolicy(){
            @Override public boolean useNativeQuickMove(AbstractContainerMenu menu){return true;}
            @Override public boolean allowsTargetedQuickMoveSource(AbstractContainerMenu menu,Slot slot){return false;}
        });
    }
    public static void support(MenuType<AbstractContainerMenu> type,String className,boolean openingData) {
        if(type==null)return;
        bindNative(type,openingData);
        if(DesktopServerApi.findWindowHandler(type)==null)DesktopServerApi.registerWindow(type,new DesktopServerWindowHandler<AbstractContainerMenu,Void>() {
            @Override public boolean supports(net.minecraft.server.level.ServerPlayer player,AbstractContainerMenu menu){
                return menu.getClass().getName().equals(className);
            }
            @Override public boolean usesNativePlayerInventoryQuickMove(AbstractContainerMenu menu){return true;}
        });
    }
}
