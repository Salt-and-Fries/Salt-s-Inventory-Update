package com.salts_inventory_update.api.client.desktop;

import java.util.List;

/** Custom Salt-managed chrome. Resolve textures through native resources; retain no render context. */
public interface DesktopWindowDecoration<T, S> {
    default DesktopChromeMetrics metrics() { return DesktopChromeMetrics.SALT; }

    /** Draw frame, background, title and controls. Salt adds none of these when a renderer is supplied. */
    void render(DesktopRenderContext<T, S> context, List<DesktopControlState> controls);
}
