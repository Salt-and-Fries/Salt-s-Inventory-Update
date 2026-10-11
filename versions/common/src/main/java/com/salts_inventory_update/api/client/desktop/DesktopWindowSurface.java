package com.salts_inventory_update.api.client.desktop;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Immutable layout committed by the window manager and shared by rendering and input. */
public record DesktopWindowSurface(DesktopSurfaceMode mode, DesktopInsets insets, boolean bodyInteractive,
                                   List<DesktopWindowRegion> regions, DesktopViewportPolicy viewportPolicy) {
    public static final DesktopWindowSurface SALT = new DesktopWindowSurface(DesktopSurfaceMode.SALT,
        DesktopInsets.SALT, true, List.of(), DesktopViewportPolicy.CLAMP);

    public DesktopWindowSurface {
        Objects.requireNonNull(mode, "mode");
        Objects.requireNonNull(insets, "insets");
        regions = List.copyOf(regions);
        Objects.requireNonNull(viewportPolicy, "viewportPolicy");
    }

    /** No Salt decoration, no host insets. Explicit regions define the hit shape. */
    public static DesktopWindowSurface modOwned(List<DesktopWindowRegion> regions) {
        return new DesktopWindowSurface(DesktopSurfaceMode.MOD_OWNED, DesktopInsets.ZERO, false,
            regions, DesktopViewportPolicy.PRESERVE);
    }

    public DesktopWindowSurface withRegions(List<DesktopWindowRegion> additional) {
        List<DesktopWindowRegion> combined = new ArrayList<>(regions);
        combined.addAll(additional);
        return new DesktopWindowSurface(mode, insets, bodyInteractive, combined, viewportPolicy);
    }

    public DesktopHitRole hitRole(int windowX, int windowY, int width, int height, double mouseX, double mouseY) {
        DesktopHitRole result = bodyInteractive && containsBody(windowX, windowY, width, height, mouseX, mouseY)
            ? DesktopHitRole.CONTENT : DesktopHitRole.NONE;
        // A body's default ownership does not mask explicitly declared drag/resize areas.
        int declaredPriority = -1;
        for (DesktopWindowRegion region : regions) {
            if (region.interactive() && region.contains(windowX, windowY, mouseX, mouseY)
                && region.role().priority() >= declaredPriority) {
                result = region.role();
                declaredPriority = region.role().priority();
            }
        }
        return result;
    }

    public boolean occludes(int windowX, int windowY, int width, int height, double mouseX, double mouseY) {
        if (bodyInteractive && containsBody(windowX, windowY, width, height, mouseX, mouseY)) return true;
        for (DesktopWindowRegion region : regions) {
            if ((region.visual() || region.interactive()) && region.contains(windowX, windowY, mouseX, mouseY)) return true;
        }
        return false;
    }

    private static boolean containsBody(int x, int y, int width, int height, double mouseX, double mouseY) {
        return mouseX >= x && mouseY >= y && mouseX < (double) x + width && mouseY < (double) y + height;
    }
}
