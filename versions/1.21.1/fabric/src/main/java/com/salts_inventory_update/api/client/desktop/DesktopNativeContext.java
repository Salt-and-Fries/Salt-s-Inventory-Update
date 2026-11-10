package com.salts_inventory_update.api.client.desktop;

/** Native geometry and ordinary public window context, using the consumer's state. */
public interface DesktopNativeContext<T,S> extends DesktopWindowContext<T,S> {
    DesktopNativeView nativeView();
    /** Inverse of the committed composition, including native scrolling and exterior controls. */
    default java.util.Optional<DesktopNativeLayout.Point> nativeSourceAt(double desktopX,double desktopY) {
        return java.util.Optional.empty();
    }
}
