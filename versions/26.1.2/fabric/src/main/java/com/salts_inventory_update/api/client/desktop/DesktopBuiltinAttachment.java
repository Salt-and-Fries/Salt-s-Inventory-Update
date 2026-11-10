package com.salts_inventory_update.api.client.desktop;

import org.jspecify.annotations.Nullable;

import java.util.List;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * A contribution to one built-in window, using its normal desktop coordinates and lifetime.
 * Surface regions extend the owner's hit/placement bounds. Use a separately registered
 * menu window for another inventory; this contribution does not create a second slot transport.
 */
public interface DesktopBuiltinAttachment {
    /** Opt-in content replacement. Core retains its frame, guide and effects. */
    default boolean replacesContent(DesktopWindowContext<AbstractContainerMenu, Void> context) { return false; }
    default void renderContent(DesktopRenderContext<AbstractContainerMenu, Void> context) { }
    /** Absolute GUI bounds; empty retains normal built-in geometry. */
    default java.util.Optional<DesktopWindowBounds> builtinSlotBounds(DesktopWindowContext<AbstractContainerMenu, Void> context, int menuSlotId) { return java.util.Optional.empty(); }
    default java.util.Optional<DesktopWindowBounds> recipeBookButtonBounds(DesktopWindowContext<AbstractContainerMenu, Void> context) { return java.util.Optional.empty(); }
    /** A real cell of a live, registered embedded session; Core validates ownership and slot ID. */
    default @Nullable DesktopSessionSlotHit embeddedSlotAt(DesktopSlotContext<AbstractContainerMenu, Void> context, double x, double y) { return null; }

    /** Native player-menu hit; Core retains its normal authenticated gesture transport. */
    default @Nullable DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu, Void> context, double x, double y) { return null; }
    /** Blocks underlying owner slots within an active content replacement. */
    default boolean occludes(DesktopWindowContext<AbstractContainerMenu, Void> context, double x, double y) { return false; }
    default void opened(DesktopWindowContext<AbstractContainerMenu, Void> context) { }
    default void suspended(DesktopWindowContext<AbstractContainerMenu, Void> context) { }
    default void closed(DesktopWindowContext<AbstractContainerMenu, Void> context) { }
    default void tick(DesktopWindowContext<AbstractContainerMenu, Void> context) { }
    /** Above the owner's content and below later windows. */
    default void render(DesktopRenderContext<AbstractContainerMenu, Void> context) { }
    default boolean mouseClicked(DesktopInputContext<AbstractContainerMenu, Void> context,
                                 MouseButtonEvent event, boolean doubleClick) { return false; }
    default boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu, Void> context,
                                  int mouseX, int mouseY) { return false; }
    default List<DesktopWindowRegion> regions(DesktopWindowContext<AbstractContainerMenu, Void> context) { return List.of(); }
}
