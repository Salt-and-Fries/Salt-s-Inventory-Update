package com.salts_inventory_update.api.client.desktop;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public interface DesktopWindowContext<T extends AbstractContainerMenu, S> {
    Minecraft minecraft();

    T menu();

    Component originalTitle();

    int sessionId();

    String sourceKey();

    S state();

    List<Slot> containerSlots();

    int windowX();

    int windowY();

    int windowWidth();

    int windowHeight();

    int contentX();

    int contentY();

    boolean focused();

    boolean minimized();

    boolean ghosted();

    ItemStack carriedStack();

    @Nullable Slot menuSlot(int menuSlotId);

    @Nullable Slot containerSlot(int containerSlotIndex);

    int menuSlotId(Slot slot);
}
