package com.salts_inventory_update.client;

import java.util.List;

import org.jspecify.annotations.Nullable;

import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowContext;
import com.salts_inventory_update.api.client.desktop.DesktopWindowSize;

/** Internal extensions used by bundled hosted-screen definitions. */
public interface InternalDesktopWindowDefinition {
    /**
     * Renders decorations that visually sit underneath the Salt window frame. This is intentionally
     * internal: hosted screens use it for native side tabs whose connector must be covered by the
     * window edge.
     */
    default void renderBehindFrame(DesktopRenderContext<?, ?> context) {
    }

    default @Nullable DesktopWindowSize desiredWindowSize(DesktopWindowContext<?, ?> context) {
        return null;
    }

    /**
     * Updates hover-owned native state before the desktop chooses which tooltip to render. The
     * {@code active} flag is true only when this is the topmost hovered window and tooltips are
     * currently eligible to render. Hosted controls can use the inactive notification to release
     * transient focus even when their normal tooltip callback is skipped.
     */
    default void updateTooltipState(
        DesktopWindowContext<?, ?> context,
        int mouseX,
        int mouseY,
        boolean active
    ) {
    }

    /** Cancels an internally captured pointer without dispatching a release action. */
    default void cancelPointer(DesktopWindowContext<?, ?> context) {
    }

    /**
     * Returns the current visual and interactive regions protruding beyond the base Salt window.
     * Region coordinates are relative to {@link DesktopWindowContext#windowX()} and
     * {@link DesktopWindowContext#windowY()}. Implementations must keep this query side-effect free because the
     * desktop calls it during rendering, hit-testing, placement, and pointer movement.
     */
    default List<InternalDesktopWindowRegion> externalRegions(DesktopWindowContext<?, ?> context) {
        return List.of();
    }
}
