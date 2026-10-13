package com.salts_inventory_update.compat.ae2.menu;

import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PacketByteBufs;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.level.ServerPlayer;

/** AE2 updates retain their native payload size independently of Salt's bounded action channel. */
public final class DesktopUpdateNetwork {
    private DesktopUpdateNetwork() { }
    public static void initializeCommon() { }
    public static void initializeClient() {
        ClientPlayNetworking.registerGlobalReceiver(DesktopMenuUpdatePacket.CHANNEL,
                (client, connection, buffer, sender) -> {
                    var packet = DesktopMenuUpdatePacket.read(buffer);
                    client.execute(packet::receive);
                });
    }
    public static boolean supports(ServerPlayer player) {
        return ServerPlayNetworking.canSend(player, DesktopMenuUpdatePacket.CHANNEL);
    }
    public static void send(ServerPlayer player, DesktopMenuUpdatePacket packet) {
        var buffer = PacketByteBufs.create();
        packet.write(buffer);
        ServerPlayNetworking.send(player, DesktopMenuUpdatePacket.CHANNEL, buffer);
    }
}
