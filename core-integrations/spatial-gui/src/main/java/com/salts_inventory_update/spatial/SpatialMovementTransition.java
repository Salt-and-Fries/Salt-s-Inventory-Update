package com.salts_inventory_update.spatial;

import org.tastytrash.spatialGUI.SpatialGUI;
import org.tastytrash.spatialGUI.client.SpatialGUIClient;

/** One reversible, elapsed-time transition shared by the camera and desktop surface. */
public final class SpatialMovementTransition {
    private SpatialMovementTransition() { }
    private static Object desktop;
    private static boolean movement;
    private static boolean closing;
    private static double from;
    private static long started;
    private static int revision;

    public static int update() {
        Object current = SpatialGuiIntegration.active() ? SpatialGuiIntegration.desktop() : null;
        boolean next = current != null && SpatialGuiIntegration.movement();
        var renderer = SpatialGUIClient.renderer();
        boolean finishing = current == null && desktop != null && renderer != null
            && net.minecraft.client.Minecraft.getInstance().level != null
            && SpatialGUIClient.isEnabled() && renderer.getInventoryRenderer().isClosing();
        if (finishing) {
            if (!closing) {
                from = blend();
                closing = true;
                started = System.nanoTime();
                revision++;
            }
        } else if (current != desktop || closing) {
            if (current == null) SpatialPanelPresentation.reset();
            desktop = current;
            movement = next;
            closing = false;
            from = next ? 1 : 0;
            started = current == null ? 0 : System.nanoTime();
            revision++;
        } else if (movement != next) {
            from = blend();
            movement = next;
            started = System.nanoTime();
            revision++;
        }
        return revision;
    }

    public static double progress() {
        if (closing) return SpatialGUIClient.renderer().getCloseBlend();
        if (started == 0) return 1;
        double duration = Math.clamp(SpatialGUI.config.transitionDurationMs, 150, 1000) * 1_000_000.0;
        double t = Math.clamp((System.nanoTime() - started) / duration, 0, 1);
        return t * t * (3 - 2 * t);
    }

    public static double blend() {
        if (closing) return from;
        double target = movement ? 1 : 0;
        return from + (target - from) * progress();
    }

    /** Presentation outlives input ownership while the captured desktop closes. */
    public static boolean presenting() { return desktop != null; }
    public static boolean closing() { return closing; }
}
