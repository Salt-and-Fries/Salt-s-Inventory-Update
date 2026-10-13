package com.salts_inventory_update.compat.ae2.client;

import io.netty.buffer.Unpooled;
import net.minecraft.network.RegistryFriendlyByteBuf;
import appeng.api.util.IConfigurableObject;
import appeng.client.gui.me.crafting.CraftingCPUScreen;
import appeng.client.gui.me.networktool.NetworkStatusScreen;
import appeng.client.gui.me.patternaccess.PatternAccessTermScreen;
import appeng.core.network.ClientboundPacket;
import appeng.core.network.bidirectional.ConfigValuePacket;
import appeng.core.network.clientbound.ClearPatternAccessTerminalPacket;
import appeng.core.network.clientbound.CraftConfirmPlanPacket;
import appeng.core.network.clientbound.CraftingStatusPacket;
import appeng.core.network.clientbound.GuiDataSyncPacket;
import appeng.core.network.clientbound.MEInventoryUpdatePacket;
import appeng.core.network.clientbound.NetworkStatusPacket;
import appeng.core.network.clientbound.PatternAccessTerminalPacket;
import appeng.core.network.clientbound.SetLinkStatusPacket;
import appeng.menu.guisync.LinkStatusAwareMenu;
import appeng.menu.me.common.MEStorageMenu;
import appeng.menu.me.crafting.CraftConfirmMenu;

/** The fork's lifetime-addressed update dispatch, using AE2's existing repositories and views. */
public final class DesktopUpdates {
    private DesktopUpdates() { }

    public static void apply(ClientboundPacket packet, appeng.client.gui.AEBaseScreen<?> content) {
        var menu = content.getMenu();
        com.salts_inventory_update.compat.ae2.menu.MenuPacketScope.run(menu, () -> {
            if (packet instanceof GuiDataSyncPacket update) {
                var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(update.syncData()),
                        menu.getPlayer().registryAccess());
                try {
                    menu.receiveServerSyncData(buffer);
                } finally {
                    buffer.release();
                }
            } else if (packet instanceof ConfigValuePacket update && menu instanceof IConfigurableObject configurable) {
                update.loadSetting(configurable);
            } else if (packet instanceof MEInventoryUpdatePacket update && menu instanceof MEStorageMenu storage) {
                if (storage.getClientRepo() != null && update.getActualEntries() != null) {
                    storage.getClientRepo().handleUpdate(update.fullUpdate(), update.getActualEntries());
                }
            } else if (packet instanceof SetLinkStatusPacket update && menu instanceof LinkStatusAwareMenu aware) {
                aware.setLinkStatus(update.linkStatus());
            } else if (packet instanceof CraftConfirmPlanPacket update && menu instanceof CraftConfirmMenu confirm) {
                confirm.setPlan(update.plan());
            } else if (packet instanceof CraftingStatusPacket update && content instanceof CraftingCPUScreen<?> cpu) {
                cpu.postUpdate(update.status());
            } else if (packet instanceof NetworkStatusPacket update && content instanceof NetworkStatusScreen network) {
                network.processServerUpdate(update.status());
            } else if (packet instanceof PatternAccessTerminalPacket update
                    && content instanceof PatternAccessTermScreen<?> terminal) {
                if (update.fullUpdate()) {
                    terminal.postFullUpdate(update.inventoryId(), update.sortBy(), update.group(),
                            update.inventorySize(), update.slots());
                } else {
                    terminal.postIncrementalUpdate(update.inventoryId(), update.slots());
                }
            } else if (packet instanceof ClearPatternAccessTerminalPacket
                    && content instanceof PatternAccessTermScreen<?> terminal) {
                terminal.clear();
            }
        });
    }

}
