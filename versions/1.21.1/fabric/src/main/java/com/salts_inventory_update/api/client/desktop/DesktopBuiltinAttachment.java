package com.salts_inventory_update.api.client.desktop;

import java.util.List;
import org.jspecify.annotations.Nullable;
import net.minecraft.world.inventory.AbstractContainerMenu;
import com.salts_inventory_update.api.client.input.MouseButtonEvent;

/**
 * An independently owned contribution to a built-in window. Coordinates use desktop GUI units.
 * Returned slots are real player-menu slots; Core routes their normal authenticated gestures.
 * Instances belong to one window, and must not install a second inventory click transport.
 */
public interface DesktopBuiltinAttachment {
    default void opened(DesktopWindowContext<AbstractContainerMenu, Void> context) { }
    /** Release transient hover/global native state while hidden; the instance may resume later. */
    default void suspended(DesktopWindowContext<AbstractContainerMenu, Void> context) { }
    default void closed(DesktopWindowContext<AbstractContainerMenu, Void> context) { }
    default void tick(DesktopWindowContext<AbstractContainerMenu, Void> context) { }
    /**
     * Keep a normally conditional built-in slot visible and interactive in the owner.
     * Character currently supports the native offhand (45). Other presentations of the
     * same menu slot are retained; all of them address the same server-owned stack.
     */
    default boolean requestsBuiltinSlot(int menuSlotId) { return false; }
    /** Foreground of the owning window, above its preview and controls, below later windows. */
    default void render(DesktopRenderContext<AbstractContainerMenu, Void> context) { }
    default @Nullable DesktopSlotHit slotAt(DesktopSlotContext<AbstractContainerMenu, Void> context, double x, double y) { return null; }
    /** Blocks underlying owner slots, without blocking other windows or creating a global modal. */
    default boolean occludes(DesktopWindowContext<AbstractContainerMenu, Void> context, double x, double y) { return false; }
    /** Reserve surface-relative space for expandable panels, without painting or blocking that space. */
    default List<DesktopWindowBounds> placementBounds(DesktopWindowContext<AbstractContainerMenu, Void> context) { return List.of(); }
    default boolean mouseClicked(DesktopInputContext<AbstractContainerMenu, Void> context,
                                 MouseButtonEvent event, boolean doubleClick) { return false; }
    default boolean appendTooltip(DesktopRenderContext<AbstractContainerMenu, Void> context,
                                  int mouseX, int mouseY) { return false; }
    /** Surface-relative bounds used for input, controller slot discovery and viewport placement. */
    default List<DesktopWindowRegion> regions(DesktopWindowContext<AbstractContainerMenu, Void> context) { return List.of(); }
}
