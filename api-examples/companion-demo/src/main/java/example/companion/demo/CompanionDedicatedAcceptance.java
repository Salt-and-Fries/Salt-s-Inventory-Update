package example.companion.demo;

import example.companion.CompanionMenus;
import example.companion.ToolInventoryMenu;
import net.minecraft.server.level.ServerPlayer;
import com.salts_inventory_update.api.server.desktop.DesktopServerApi;

/** Optional server probe resolves only common/server API, never a client class. */
public final class CompanionDedicatedAcceptance {
    public static void verify(ServerPlayer player, ToolInventoryMenu menu) {
        if (!DesktopServerApi.hasWindowSupport(CompanionMenus.main()) || !DesktopServerApi.hasWindowSupport(CompanionMenus.tool())
            || !DesktopServerApi.supportsWindow(player, menu)) throw new IllegalStateException("Common companion providers are unavailable");
        System.out.println("COMPANION DEDICATED PASS: common-only optional registration and menu support");
    }
}
