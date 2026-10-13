package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.client.gui.AEBaseScreen;
import net.minecraft.world.inventory.ClickType;

/** Fork-owned callbacks scoped to one hosted input event. */
public interface NativeContentActions {
    boolean clickSlot(int slot, int button, ClickType action);
    void close();
    void switchContent(AEBaseScreen<?> content);
}
