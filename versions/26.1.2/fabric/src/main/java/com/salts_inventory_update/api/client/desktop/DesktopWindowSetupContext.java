package com.salts_inventory_update.api.client.desktop;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public record DesktopWindowSetupContext<T>(
    Minecraft minecraft,
    T model,
    Component originalTitle,
    int sessionId,
    String sourceKey,
    List<Slot> containerSlots,
    int defaultContentWidth,
    int defaultContentHeight,
    int defaultWindowWidth,
    int defaultWindowHeight
) {
    public DesktopWindowSetupContext {
        containerSlots = List.copyOf(containerSlots);
    }

    /** Client-only views have no session and no inventory capability. */
    public boolean inventoryAvailable() {
        return sessionId != Integer.MIN_VALUE && model instanceof AbstractContainerMenu;
    }
}
