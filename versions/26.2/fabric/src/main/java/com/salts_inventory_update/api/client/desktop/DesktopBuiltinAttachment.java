package com.salts_inventory_update.api.client.desktop;

import java.util.List;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.client.input.MouseButtonEvent;

/**
 * A contribution to one built-in window, using its normal desktop coordinates and lifetime.
 * Surface regions extend the owner's hit/placement bounds. Use a separately registered
 * menu window for another inventory; this contribution does not create a second slot transport.
 */
public interface DesktopBuiltinAttachment {
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
