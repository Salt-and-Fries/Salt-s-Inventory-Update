package com.salts_inventory_update.compat.ae2.client;

import appeng.api.util.IConfigurableObject;
import appeng.client.gui.AEBaseScreen;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.me.networktool.NetworkStatusScreen;
import appeng.client.gui.me.patternaccess.PatternAccessTermScreen;
import appeng.core.sync.BasePacket;
import appeng.core.sync.packets.*;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.crafting.CraftConfirmMenu;
import com.salts_inventory_update.compat.ae2.nativeview.NativeMenuPacketScope;

/** Applies native decoded data to the lifetime-selected model without installing a screen. */
public final class DesktopUpdates {
    private DesktopUpdates() { }
    public static void apply(BasePacket packet, AEBaseScreen<?> content) {
        var menu=content.getMenu();
        NativeMenuPacketScope.run(menu, () -> {
            if (packet instanceof GuiDataSyncPacket update) menu.receiveServerSyncData(update);
            else if (packet instanceof ConfigValuePacket update && menu instanceof IConfigurableObject configurable)
                withNativeMenu(menu, () -> update.clientPacketData(menu.getPlayer()));
            else if (packet instanceof MEInventoryUpdatePacket update && menu instanceof MEStorageMenu storage) {
                var repository=storage.getClientRepo();
                if (repository != null) repository.handleUpdate(update.siu$fullUpdate(),update.siu$entries());
            } else if (packet instanceof CraftConfirmPlanPacket update && menu instanceof CraftConfirmMenu confirm)
                confirm.setPlan(update.siu$plan());
            else if (packet instanceof CraftingStatusPacket update && content instanceof CraftingCPUScreen<?> cpu)
                cpu.postUpdate(update.siu$status());
            else if (packet instanceof NetworkStatusPacket update && content instanceof NetworkStatusScreen network)
                network.processServerUpdate(update.siu$status());
            else if (packet instanceof PatternAccessTerminalPacket update && content instanceof PatternAccessTermScreen<?> terminal) {
                if (update.siu$fullUpdate()) terminal.postFullUpdate(update.siu$inventoryId(),update.siu$sortBy(),
                        update.siu$group(),update.siu$inventorySize(),update.siu$slots());
                else terminal.postIncrementalUpdate(update.siu$inventoryId(),update.siu$slots());
            } else if (packet instanceof ClearPatternAccessTerminalPacket && content instanceof PatternAccessTermScreen<?> terminal)
                terminal.clear();
        });
    }
    private static void withNativeMenu(appeng.menu.AEBaseMenu menu, Runnable action) {
        var player=menu.getPlayer();var previous=player.containerMenu;
        player.containerMenu=menu;
        try { action.run(); } finally { player.containerMenu=previous; }
    }
}
