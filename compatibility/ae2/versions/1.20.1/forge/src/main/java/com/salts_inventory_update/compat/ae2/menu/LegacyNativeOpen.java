package com.salts_inventory_update.compat.ae2.menu;

import com.salts_inventory_update.api.server.desktop.DesktopSessions;
import java.util.function.Consumer;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraftforge.network.NetworkHooks;

/** Routes Forge's native opener through the public desktop API when eligible. */
public final class LegacyNativeOpen {
    private LegacyNativeOpen() { }
    public static void openScreen(ServerPlayer player, MenuProvider provider, Consumer<FriendlyByteBuf> nativeData) {
        // The registered AE2 opening writer supplies the desktop lifetime and
        // native locator. Ordinary opens keep Forge's original extra data.
        if (DesktopSessions.openMenuSession(player,provider,null)==null)
            NetworkHooks.openScreen(player,provider,nativeData);
    }
}
