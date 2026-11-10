package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.client.desktop.DesktopMenuFactories;
import io.netty.buffer.Unpooled;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Moonlight keeps its extended factory in MenuType's ordinary supplier field. */
final class MoonlightMenuFactories {
    private static final java.util.Set<MenuType<?>> REGISTERED=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private MoonlightMenuFactories() { }
    @SuppressWarnings({"rawtypes","unchecked"})
    static void register(MenuType type) {
        // Sawmill deliberately opens with vanilla ServerPlayer.openMenu. Its
        // Moonlight supplier accepts a nullable buffer but consumes no opening
        // bytes. An extended factory would incorrectly require a data packet.
        var menuKey=BuiltInRegistries.MENU.getKey(type);
        if(menuKey!=null&&menuKey.getNamespace().equals("sawmill"))return;
        if(REGISTERED.contains(type)||DesktopMenuFactories.hasFactory(type))return;
        final Class<?> factoryApi;
        try { factoryApi=Class.forName("net.mehvahdjukaar.moonlight.api.client.fabric.IFabricMenuType$Factory"); }
        catch(ClassNotFoundException absentOnForge){return;}
        try {
            Object factory=type.getClass().getMethod("getConstructor").invoke(type);
            if(!factoryApi.isInstance(factory))return;
            var create=factoryApi.getMethod("create",int.class,Inventory.class,FriendlyByteBuf.class);
            DesktopMenuFactories.register(type,(minecraft,player,id,data)->{
                var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
                try { return (AbstractContainerMenu)create.invoke(factory,id,player.getInventory(),buffer); }
                catch(ReflectiveOperationException failure){throw new IllegalStateException("Moonlight native opening failed",failure);}
                finally{buffer.release();}
            });
            REGISTERED.add(type);
        }catch(ReflectiveOperationException failure){throw new IllegalStateException("Moonlight native factory lookup failed",failure);}
    }
}
