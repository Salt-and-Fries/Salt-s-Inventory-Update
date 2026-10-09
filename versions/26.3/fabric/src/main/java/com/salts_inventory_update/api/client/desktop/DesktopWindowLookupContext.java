package com.salts_inventory_update.api.client.desktop;

import java.util.List;

import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.inventory.Slot;

public record DesktopWindowLookupContext(
    AbstractContainerMenu menu,
    MenuType<?> menuType,
    Component title,
    int sessionId,
    String sourceKey,
    int specialKind,
    List<Slot> containerSlots,
    int defaultContentWidth,
    int defaultContentHeight
) {
}
