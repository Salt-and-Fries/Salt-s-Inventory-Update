package com.salts_inventory_update.compat.ae2.menu;

import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkDirection;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

/** Optional extension channel; native AE2 codecs stay in the installed AE2 JAR. */
public final class DesktopUpdateNetwork {
    private static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(DesktopMenuUpdatePacket.CHANNEL,
            () -> "1", DesktopUpdateNetwork::acceptVersion, DesktopUpdateNetwork::acceptVersion);
    private static boolean initialized;
    private DesktopUpdateNetwork() { }
    private static boolean acceptVersion(String version) {
        return "1".equals(version) || NetworkRegistry.ABSENT.equals(version) || NetworkRegistry.ACCEPTVANILLA.equals(version);
    }
    public static void initializeCommon() {
        if (initialized) return;
        CHANNEL.messageBuilder(DesktopMenuUpdatePacket.class, 0, NetworkDirection.PLAY_TO_CLIENT)
                .encoder(DesktopMenuUpdatePacket::write).decoder(DesktopMenuUpdatePacket::read)
                .consumerMainThread((packet, context) -> packet.receive()).add();
        initialized = true;
    }
    public static void initializeClient() { }
    public static boolean supports(ServerPlayer player) { return CHANNEL.isRemotePresent(player.connection.connection); }
    public static void send(ServerPlayer player, DesktopMenuUpdatePacket packet) {
        CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), packet);
    }
}
