package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;

/** Preserve Exposure's native name setter inside its authenticated window. */
final class ExposureRenameActions {
    private static boolean initialized;
    private static final java.util.Set<MenuType<?>> BOUND=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private ExposureRenameActions() { }
    static synchronized void initialize() {
        if(initialized||!DesktopEnvironment.isModLoaded("exposure"))return;
        initialized=true;
        install();
        DesktopEnvironment.onServerStarting(server->install());
    }
    static synchronized void install() {
        if(!DesktopEnvironment.isModLoaded("exposure"))return;
        var type=NativeMenuSupport.type("exposure:item_rename");
        if(type==null||!BOUND.add(type))return;
        DesktopServerApi.registerNativePayload(type,new net.minecraft.resources.ResourceLocation("minecraft","rename_item"),c->{
            var menu=c.menu();
            if(!menu.getClass().getName().equals("io.github.mortuusars.exposure.world.inventory.ItemRenameMenu")
                ||!menu.stillValid(c.player()))return;
            var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
            String name;
            try {
                name=buffer.readUtf(32767);
                if(buffer.isReadable())throw new IllegalArgumentException("Trailing native film rename data");
            } finally {buffer.release();}
            // Same public setter as Exposure's ordinary rename-packet listener.
            // Its native formatting/length validation and preview stay intact;
            // the original Apply button remains responsible for committing it.
            DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->setNativeName(menu,name));
            c.broadcastChanges();
        });
    }
    private static void setNativeName(AbstractContainerMenu menu,String name) {
        try {
            var setter=menu.getClass().getMethod("setItemName",String.class);
            if(setter.getReturnType()!=boolean.class)throw new IllegalStateException("Unsupported native film rename return type");
            setter.invoke(menu,name);
        } catch(ReflectiveOperationException error) {
            throw new IllegalStateException("Unsupported native film rename contract",error);
        }
    }
}
