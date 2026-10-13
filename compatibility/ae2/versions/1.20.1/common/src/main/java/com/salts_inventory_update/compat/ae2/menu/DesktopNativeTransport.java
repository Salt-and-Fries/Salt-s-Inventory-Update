package com.salts_inventory_update.compat.ae2.menu;

import appeng.core.sync.BasePacket;
import appeng.core.sync.network.NetworkHandler;
import appeng.menu.AEBaseMenu;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;

/** Only desktop additions change transport; ordinary AE2 retains its native channel and handlers. */
public final class DesktopNativeTransport {
    private DesktopNativeTransport() { }

    public static void sendDefault(BasePacket packet) { NetworkHandler.instance().sendToServer(packet); }

    public static void sendUpdate(AEBaseMenu menu, UUID owner, BasePacket packet) {
        if (menu.getPlayer() instanceof ServerPlayer player) {
            DesktopUpdateNetwork.send(player, new DesktopMenuUpdatePacket(owner, DesktopMenuPackets.channel(packet),
                    DesktopMenuPackets.encodeUpdate(packet, null)));
        }
    }
}
