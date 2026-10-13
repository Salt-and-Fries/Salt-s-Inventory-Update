package com.salts_inventory_update.compat.ae2.nativeview;

import com.salts_inventory_update.api.client.desktop.DesktopClientApi;
import com.salts_inventory_update.api.client.desktop.DesktopMenuFactories;
import com.salts_inventory_update.compat.ae2.menu.NativeDesktopBuilder;
import com.salts_inventory_update.compat.ae2.menu.NativeMenuRegistry;
import appeng.menu.AEBaseMenu;
import io.netty.buffer.Unpooled;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.inventory.MenuType;

/** Invoked by the verified optional bootstrap after AE2 screen registration completes. */
public final class AE2ClientIntegration {
    private AE2ClientIntegration() { }
    public static void initialize() {
        NativeMenuRegistry.builders().forEach((type,builder) ->
                register((MenuType<?>)type,(NativeDesktopBuilder)builder));
        AEWindow.installHeaderKeyboard();
    }

    @SuppressWarnings("unchecked")
    private static void register(MenuType<?> untyped, NativeDesktopBuilder builder) {
        var type=(MenuType<AEBaseMenu>)untyped;
        DesktopClientApi.registerWindow(type,new AEWindow(type));
        DesktopMenuFactories.register(type,(minecraft,player,id,data) -> {
            var buffer=new FriendlyByteBuf(Unpooled.wrappedBuffer(data));
            try {
                var lifetime=buffer.readUUID();boolean companion=buffer.readBoolean();
                var menu=builder.createDesktopMenu(id,player.getInventory(),buffer);
                if (buffer.isReadable()) throw new IllegalArgumentException("Trailing AE2 desktop opening data");
                menu.setDesktopSyncId(lifetime);menu.setDesktopToolboxCompanion(companion);
                return menu;
            } finally { buffer.release(); }
        });
    }
}
