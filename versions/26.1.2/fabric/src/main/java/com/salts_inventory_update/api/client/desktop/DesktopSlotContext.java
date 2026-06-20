package com.salts_inventory_update.api.client.desktop;

import org.jspecify.annotations.Nullable;

import net.minecraft.world.inventory.AbstractContainerMenu;

public interface DesktopSlotContext<T extends AbstractContainerMenu, S> extends DesktopWindowContext<T, S> {
    boolean contains(double mouseX, double mouseY, int x, int y, int width, int height);

    @Nullable DesktopSlotHit menuSlotHit(int menuSlotId, int x, int y, double mouseX, double mouseY);

    @Nullable DesktopSlotHit containerSlotHit(int containerSlotIndex, int x, int y, double mouseX, double mouseY);
}
