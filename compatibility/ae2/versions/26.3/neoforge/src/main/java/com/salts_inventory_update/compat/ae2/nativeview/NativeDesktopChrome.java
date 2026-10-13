package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.client.gui.style.Blitter;
import com.salts_inventory_update.compat.ae2.client.DesktopChrome;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.resources.Identifier;

/** Native signature facade; all primitive drawing is the fork's shared pixel geometry. */
public final class NativeDesktopChrome {
    public static final int SURFACE = DesktopChrome.SURFACE, SLOT = DesktopChrome.SLOT,
            TEXT = DesktopChrome.TEXT, TAB_SIZE = DesktopChrome.TAB_SIZE;
    public enum Edge { LEFT, RIGHT, TOP, BOTTOM }
    private NativeDesktopChrome() { }
    private static DesktopChrome.Bounds bounds(Rect2i r) {
        return new DesktopChrome.Bounds(r.getX(), r.getY(), r.getWidth(), r.getHeight());
    }
    private static DesktopChrome.Graphics graphics(GuiGraphicsExtractor nativeGraphics) {
        return new DesktopChrome.Graphics() {
            @Override public void fill(int x0, int y0, int x1, int y1, int color) { nativeGraphics.fill(x0, y0, x1, y1, color); }
            @Override public void sprite(String id, DesktopChrome.Bounds b) {
                Blitter.guiSprite(Identifier.parse(id)).dest(b.x(), b.y(), b.width(), b.height()).blit(nativeGraphics);
            }
        };
    }
    public static void panel(GuiGraphicsExtractor g, Rect2i r) { DesktopChrome.panel(graphics(g), bounds(r)); }
    public static void recessed(GuiGraphicsExtractor g, Rect2i r) { DesktopChrome.recessed(graphics(g), bounds(r)); }
    public static void textField(GuiGraphicsExtractor g, Rect2i r) { DesktopChrome.textField(graphics(g), bounds(r)); }
    public static void slot(GuiGraphicsExtractor g, int x, int y) { DesktopChrome.slot(graphics(g), x, y); }
    public static void button(GuiGraphicsExtractor g, Rect2i r, boolean h, boolean f, boolean a) { DesktopChrome.button(graphics(g), bounds(r), h, f, a); }
    public static void tab(GuiGraphicsExtractor g, Rect2i r, Edge e, boolean s, boolean h, boolean f) { DesktopChrome.tab(graphics(g), bounds(r), DesktopChrome.Edge.valueOf(e.name()), s, h, f); }
    public static void arrow(GuiGraphicsExtractor g, int x, int y) { DesktopChrome.arrow(graphics(g), x, y); }
}
