package com.salts_inventory_update.api.client.desktop;

import java.util.List;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;

public record DesktopWindowSetupContext<T extends AbstractContainerMenu>(
    Minecraft minecraft,
    T menu,
    Component originalTitle,
    int sessionId,
    String sourceKey,
    List<Slot> containerSlots,
    int defaultContentWidth,
    int defaultContentHeight,
    int defaultWindowWidth,
    int defaultWindowHeight
) {
}
