package com.salts_inventory_update.api.client.desktop;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;

public interface DesktopInputContext<T extends AbstractContainerMenu, S> extends DesktopWindowContext<T, S> {
    boolean shiftDown();

    boolean ctrlDown();

    boolean altDown();

    boolean mouseButtonDown(int button);

    boolean sendMenuButton(int buttonId);

    boolean sendRename(String name);

    boolean clickSlot(int menuSlotId, int button, ClickType input);

    boolean quickMoveSlot(int menuSlotId);

    boolean toggleRecipeBook();

    boolean setRecipeBookSearch(String search);

    boolean sendPayload(ResourceLocation channel, byte[] data);

    default boolean sendCustomPayload(ResourceLocation channel, byte[] data) {
        return this.sendPayload(channel, data);
    }
}
