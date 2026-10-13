package com.salts_inventory_update.api.server.desktop;

import java.util.OptionalInt;
import java.util.function.Consumer;
import java.util.function.Predicate;
import org.jspecify.annotations.Nullable;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.salts_inventory_update.server.DesktopContainerSessions;

/** Controlled entry points for native menu openers and detached-session payloads.
 * Call on the server thread. A transition preserves the authenticated session
 * while the native menu opener replaces its menu. Payload routing is bounded
 * and succeeds only for a live session belonging to the supplied player.
 */
public final class DesktopSessions {
    private DesktopSessions() { }
    public static boolean shouldCapture(ServerPlayer player) { return DesktopContainerSessions.shouldCapture(player); }
    public static @Nullable OptionalInt openMenuSession(ServerPlayer player, MenuProvider provider,
            Consumer<FriendlyByteBuf> openingData) {
        return DesktopContainerSessions.openMenuSession(player, provider, openingData);
    }
    public static void withSessionTransition(ServerPlayer player, int sessionId, Runnable action) {
        DesktopContainerSessions.withSessionTransition(player, sessionId, action);
    }
    public static int activeNetworkSessionId(ServerPlayer player) { return DesktopContainerSessions.activeNetworkSessionId(player); }
    public static boolean hasActiveSessionTransition(ServerPlayer player) { return DesktopContainerSessions.hasActiveSessionTransition(player); }
    public static boolean hasOpenSessionMatching(Player player, Predicate<AbstractContainerMenu> predicate) {
        return DesktopContainerSessions.hasOpenSessionMatching(player, predicate);
    }
    public static String blockSourceKey(ServerPlayer player, BlockPos position) { return DesktopContainerSessions.blockSourceKey(player, position); }
    public static boolean sendSessionPayload(ServerPlayer player, int sessionId, ResourceLocation channel, byte[] data) {
        return DesktopContainerSessions.sendSessionPayload(player, sessionId, channel, data);
    }
}
