package com.salts_inventory_update.client.automatic;

import java.util.function.Supplier;
import com.salts_inventory_update.api.client.desktop.DesktopInputContext;
import com.salts_inventory_update.api.client.desktop.DesktopRenderContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;

/** Automatic hosts and explicitly opted-in native templates only; ordinary API windows never enter. */
public final class AutomaticNativeScope {
    private static final ThreadLocal<Frame> ACTIVE = new ThreadLocal<>();
    private static final ThreadLocal<Boolean> DESKTOP_SCISSOR = new ThreadLocal<>();
    public record Frame(AbstractContainerScreen<?> screen, AutomaticNativeWindow.State owner, DesktopRenderContext<?, ?> rendering,
                        DesktopInputContext<?, ?> input, AutomaticContainerLayout layout, int offsetX, int offsetY) { }
    private AutomaticNativeScope() { }
    public static Frame current() { return ACTIVE.get(); }
    public static boolean owns(Object screen) { return current() != null && current().screen() == screen; }
    public static boolean capturingSlots(Object screen) { return owns(screen) && current().owner().capturing; }
    public static int nativeScissor(int value, boolean horizontal) {return value;}
    public static void beginTextureClip(GuiGraphics graphics, int x1, int x2, int y1, int y2) {
        Frame f = current(); if (f == null || f.rendering() == null) return;
        if (f.owner().capturing) return;
        var a = (com.salts_inventory_update.mixin.client.AutomaticContainerScreenAccessor)f.screen();
        if (x1 != a.salts_inventory_update$nativeLeft() || y1 != a.salts_inventory_update$nativeTop()
            || x2 - x1 != a.salts_inventory_update$nativeWidth() || y2 - y1 != a.salts_inventory_update$nativeHeight()) return;
        var b = f.owner().baseLayout;
        // Legacy GuiGraphics scissor coordinates are absolute GUI coordinates;
        // unlike geometry vertices they are not transformed by its pose stack.
        int left = a.salts_inventory_update$nativeLeft() + b.x();
        int top = a.salts_inventory_update$nativeTop() + b.y();
        Boolean previous = DESKTOP_SCISSOR.get(); DESKTOP_SCISSOR.set(true);
        try { graphics.enableScissor(left, top, left + b.width(), top + b.height()); }
        finally { if (previous == null) DESKTOP_SCISSOR.remove(); else DESKTOP_SCISSOR.set(previous); }
        f.owner().bodyClip = true;
    }
    public static void finishTextureClip(Frame f, GuiGraphics graphics) {
        if (f != null && f.owner().bodyClip) { f.owner().bodyClip = false; graphics.disableScissor(); }
    }
    public static void recordDraw(GuiGraphics graphics, int x1, int x2, int y1, int y2) {
        Frame f = current();
        if (f == null || f.rendering() == null) return;
        var a = (com.salts_inventory_update.mixin.client.AutomaticContainerScreenAccessor) f.screen();
        var p1 = graphics.pose().transformPosition(x1,y1,new org.joml.Vector2f());
        var p2 = graphics.pose().transformPosition(x2,y2,new org.joml.Vector2f());
        int left = (int)Math.floor(Math.min(p1.x(), p2.x())) - f.offsetX() - a.salts_inventory_update$nativeLeft();
        int top = (int)Math.floor(Math.min(p1.y(), p2.y())) - f.offsetY() - a.salts_inventory_update$nativeTop();
        int right = (int)Math.ceil(Math.max(p1.x(), p2.x())) - f.offsetX() - a.salts_inventory_update$nativeLeft();
        int bottom = (int)Math.ceil(Math.max(p1.y(), p2.y())) - f.offsetY() - a.salts_inventory_update$nativeTop();
        // Ignore the complete inventory/background plate and lower-player artwork.
        // Retain small native decorations extending above/alongside the upper panel.
        AutomaticContainerLayout base = f.owner().baseLayout;
        if (bottom > base.y() + base.height() || top < -256 || right - left > 2048 || bottom - top > 512) return;
        if (right <= left || bottom <= top) return;
        // The native image's own corners, nine-slice pieces and title strip are
        // not external controls. Only outboard native decorations can extend it.
        if (left >= 0 && top >= 0 && right <= a.salts_inventory_update$nativeWidth()) return;
        f.owner().observe(left, top, right, bottom);
    }
    public static <T> T call(Frame frame, Supplier<T> action) {
        Frame previous = ACTIVE.get(); ACTIVE.set(frame);
        try { return action.get(); }
        finally { if (previous == null) ACTIVE.remove(); else ACTIVE.set(previous); }
    }
    public static void renderSlot(Object screen, GuiGraphics graphics, Slot slot) {
        Frame frame = current();
        if (frame == null || frame.screen() != screen || frame.rendering() == null) return;
        if (frame.owner().capturing) return;
        var context = frame.rendering();
        // Native renderSlot is called inside the native left/top pose. Core's renderer
        // accepts desktop coordinates and owns the shared cursor/gesture preview.
        graphics.pose().pushMatrix();
        Boolean previousScissor = DESKTOP_SCISSOR.get(); DESKTOP_SCISSOR.set(true);
        try {
            graphics.pose().translate(-(context.contentX() - frame.layout().x()),
                                      -(context.contentY() - frame.layout().y()));
            if (context.containerSlots().contains(slot)) {
                context.slot(slot, context.contentX() + slot.x - frame.layout().x(),
                                   context.contentY() + slot.y - frame.layout().y());
            }
        } finally {
            if (previousScissor == null) DESKTOP_SCISSOR.remove(); else DESKTOP_SCISSOR.set(previousScissor);
            graphics.pose().popMatrix();
        }
    }
    public static boolean captureScissor(GuiGraphics graphics, net.minecraft.client.gui.navigation.ScreenRectangle rectangle) {return false;}
}
