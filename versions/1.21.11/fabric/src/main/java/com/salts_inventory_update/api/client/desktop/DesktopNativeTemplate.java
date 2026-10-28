package com.salts_inventory_update.api.client.desktop;

import net.minecraft.world.inventory.AbstractContainerMenu;

/**
 * Explicit opt-in to Core's native host. Register through DesktopClientApi like any other definition.
 * Ordinary definition hooks add controls, artwork, overlays, input and lifecycle behavior to this base.
 */
public interface DesktopNativeTemplate<T extends AbstractContainerMenu,S> extends DesktopWindowDefinition<T,S> {
    default DesktopNativeLayout nativeLayout(DesktopNativeContext<T,S> context) {
        return DesktopNativeLayout.automatic(context.nativeView().automaticBounds());
    }
    /** Draw before native pixels. render() adds controls/artwork above the composed native regions and slots. */
    default void renderNativeUnderlay(DesktopRenderContext<T,S> context) { }
    /** False replaces only this real cell's captured background with the standard SIU slot frame. */
    default boolean nativeSlotBackground(DesktopNativeContext<T,S> context,int menuSlotId) { return true; }
}
