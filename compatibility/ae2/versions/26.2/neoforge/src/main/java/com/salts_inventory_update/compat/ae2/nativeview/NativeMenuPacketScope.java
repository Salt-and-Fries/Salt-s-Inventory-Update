package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.core.network.ServerboundPacket;
import appeng.menu.AEBaseMenu;
import com.salts_inventory_update.compat.ae2.menu.MenuPacketScope;
import java.util.function.Supplier;
import net.neoforged.neoforge.client.network.ClientPacketDistributor;

/** Native signature adapter; ordinary AE2 widgets keep their original transport. */
public final class NativeMenuPacketScope {
    private NativeMenuPacketScope() { }
    public static <T> T call(AEBaseMenu menu, Supplier<T> action) { return MenuPacketScope.call(menu, action); }
    public static void run(AEBaseMenu menu, Runnable action) { MenuPacketScope.run(menu, action); }
    public static void send(ServerboundPacket packet) {
        MenuPacketScope.send(packet, (menu, action) -> ((AEBaseMenu) menu).sendPacketToServer(action),
                ClientPacketDistributor::sendToServer);
    }
}
