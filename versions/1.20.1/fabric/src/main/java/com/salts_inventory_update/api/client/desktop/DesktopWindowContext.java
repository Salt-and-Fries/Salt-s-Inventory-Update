package com.salts_inventory_update.api.client.desktop;

import java.util.List;

import org.jspecify.annotations.Nullable;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

public interface DesktopWindowContext<T, S> {
    Minecraft minecraft();

    /** The integration's model: its menu for container windows, its own model for client-only views. */
    T model();

    boolean inventoryAvailable();

    /** Eligible, visible contents. Minimized, ghosted, hidden and disposed windows return false. */
    boolean visible();

    /** True for a shared, server-owned inventory companion. */
    boolean companion();

    /** Current parent session IDs; immutable and local to this connection. */
    java.util.List<Integer> companionOwnerSessionIds();

    DesktopWindowHandle windowHandle();

    /** The currently committed immutable surface. */
    DesktopWindowSurface surface();

    DesktopWindowEnvironment environment();

    DesktopGestureSnapshot gesture();

    boolean pointerCaptured();

    java.util.List<DesktopPopup> popups();

    /** Lifetime-owned scope; close individual observations when a subview stops using them. */
    DesktopWindowTasks tasks();

    default DesktopTransform surfaceTransform() { return new DesktopTransform(windowX(), windowY(), 1); }

    default DesktopTransform contentTransform() { return new DesktopTransform(contentX(), contentY(), 1); }

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

    int contentWidth();

    int contentHeight();

    /** Selected inventory transfer target, independent of text/keyboard focus. */
    boolean focused();

    boolean keyboardFocused();

    boolean minimized();

    boolean ghosted();

    ItemStack carriedStack();

    boolean recipeBookVisible();

    boolean refreshRecipeBook();

    @Nullable Slot menuSlot(int menuSlotId);

    @Nullable Slot containerSlot(int containerSlotIndex);

    int menuSlotId(Slot slot);

    int fontWidth(String text);

    int fontWidth(Component text);

    String trimToWidth(String text, int maxWidth);
}
