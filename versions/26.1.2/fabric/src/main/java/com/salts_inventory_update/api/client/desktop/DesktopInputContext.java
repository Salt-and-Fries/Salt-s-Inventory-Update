package com.salts_inventory_update.api.client.desktop;

import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerInput;

public interface DesktopInputContext<T extends AbstractContainerMenu, S> extends DesktopWindowContext<T, S> {
    boolean shiftDown();

    boolean sendMenuButton(int buttonId);

    boolean sendRename(String name);

    boolean clickSlot(int menuSlotId, int button, ContainerInput input);

    boolean quickMoveSlot(int menuSlotId);

    boolean sendCustomPayload(Identifier channel, byte[] data);
}
