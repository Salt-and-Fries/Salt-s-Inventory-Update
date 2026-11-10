package com.salts_inventory_update.compat.nativewindows;

import com.salts_inventory_update.api.integration.DesktopEnvironment;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;
import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.MenuType;

/** The publisher's main/1 UTF name action, scoped to its owning Anvil session. */
final class EasyAnvilsActions {
    private static boolean initialized;
    private static final java.util.Set<MenuType<?>> BOUND=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
    private EasyAnvilsActions() { }
    static synchronized void initialize() {
        if(initialized||!DesktopEnvironment.isModLoaded("easyanvils"))return;
        initialized=true;
        install();
        DesktopEnvironment.onServerStarting(server->install());
    }
    static synchronized void install() {
        if(!DesktopEnvironment.isModLoaded("easyanvils"))return;
        // New releases own easyanvils:repair instead of Minecraft's menu type.
        // Register after loader-owned registries are populated on both sides.
        for(String id:java.util.List.of("minecraft:anvil","easyanvils:repair")) {
            var type=NativeMenuSupport.type(id);
            if(type==null||!BOUND.add(type))continue;
            DesktopServerApi.registerNativePayload(type,net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("easyanvils","main/1"),c->{
            if(!(c.menu() instanceof AnvilMenu menu)
                ||!menu.getClass().getName().startsWith("fuzs.easyanvils.")
                ||!menu.stillValid(c.player()))return;
            var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(c.data()));
            String name;
            try {
                name=buffer.readUtf(32767);
                if(buffer.isReadable())throw new IllegalArgumentException("Trailing native anvil rename data");
            } finally {buffer.release();}
            // Same stillValid + native setItemName operation as the publisher's
            // listener. Its own formatting/cost/result logic stays authoritative.
            DesktopSessions.withSessionTransition(c.player(),c.sessionId(),()->menu.setItemName(name));
            c.broadcastChanges();
            });
        }
    }
}
