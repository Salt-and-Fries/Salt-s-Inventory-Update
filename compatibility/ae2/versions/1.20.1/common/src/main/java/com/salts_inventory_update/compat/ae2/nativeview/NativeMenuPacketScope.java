package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.core.sync.BasePacket;
import appeng.menu.AEBaseMenu;
import com.salts_inventory_update.compat.ae2.menu.MenuPacketScope;
import java.util.function.Supplier;
import com.salts_inventory_update.compat.ae2.menu.DesktopNativeTransport;

/** Native signature adapter; ordinary AE2 widgets keep their original transport. */
public final class NativeMenuPacketScope {
    private NativeMenuPacketScope() { }
    public static <T> T call(AEBaseMenu menu, Supplier<T> action) { return MenuPacketScope.call(menu, action); }
    public static void run(AEBaseMenu menu, Runnable action) { MenuPacketScope.run(menu, action); }
    public static void send(BasePacket packet) {
        MenuPacketScope.send(packet, (menu, action) -> ((AEBaseMenu) menu).sendPacketToServer(action),
                DesktopNativeTransport::sendDefault);
    }
}
