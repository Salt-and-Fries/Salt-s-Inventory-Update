package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.client.gui.AEBaseScreen;
import net.minecraft.world.inventory.ContainerInput;

/** AE-owned callbacks; implementations belong to optional integrations and live for one input callback. */
public interface NativeContentActions {
    boolean clickSlot(int slot, int button, ContainerInput action);

    void close();

    void switchContent(AEBaseScreen<?> content);
}
