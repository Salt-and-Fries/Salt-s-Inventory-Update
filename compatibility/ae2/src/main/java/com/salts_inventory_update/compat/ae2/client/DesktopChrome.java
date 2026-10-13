package com.salts_inventory_update.compat.ae2.client;

/** Fork desktop drawing, with native sprite rendering delegated to the AE2 adapter. */
public final class DesktopChrome {
    public interface Graphics {
        void fill(int left, int top, int right, int bottom, int color);
        void sprite(String identifier, Bounds bounds);
    }

    public record Bounds(int x, int y, int width, int height) {
        public int getX() { return x; }
        public int getY() { return y; }
        public int getWidth() { return width; }
        public int getHeight() { return height; }
    }

    public static final int SURFACE = 0xFFC6C6C6;
    public static final int SLOT = 0xFF8B8B8B;
    public static final int TEXT = 0xFF404040;
    public static final int TAB_SIZE = 26;

    public enum Edge {
        LEFT, RIGHT, TOP, BOTTOM
    }

    private DesktopChrome() {
    }

    public static void panel(Graphics graphics, Bounds bounds) {
        bevel(graphics, bounds, SURFACE, false);
    }

    public static void recessed(Graphics graphics, Bounds bounds) {
        bevel(graphics, bounds, SLOT, true);
    }

    public static void textField(Graphics graphics, Bounds bounds) {
        bevel(graphics, bounds, 0xFF202020, true);
    }

    public static void slot(Graphics graphics, int x, int y) {
        graphics.fill(x - 1, y - 1, x + 17, y + 17, 0xFFFFFFFF);
        graphics.fill(x - 1, y - 1, x + 16, y, 0xFF373737);
        graphics.fill(x - 1, y, x, y + 16, 0xFF373737);
        graphics.fill(x, y, x + 16, y + 16, SLOT);
    }

    public static void button(Graphics graphics, Bounds bounds, boolean hovered, boolean focused,
            boolean active) {
        bevel(graphics, bounds, active && hovered ? 0xFFD7D7D7 : SURFACE, false);
        if (focused) {
            outline(graphics, new Bounds(bounds.getX() + 2, bounds.getY() + 2,
                    bounds.getWidth() - 4, bounds.getHeight() - 4), 0xFFB8A442);
        }
    }

    public static void tab(Graphics graphics, Bounds bounds, Edge edge, boolean selected,
            boolean hovered, boolean focused) {
        if (edge == Edge.TOP || edge == Edge.BOTTOM) {
            String sprite = "container/creative_inventory/tab_" + (edge == Edge.TOP ? "top_" : "bottom_")
                    + (selected ? "selected_2" : "unselected_2");
            graphics.sprite("minecraft:" + sprite, bounds);
            if (hovered && !selected) {
                graphics.fill(bounds.getX() + 3, bounds.getY() + 3, bounds.getX() + bounds.getWidth() - 3,
                        bounds.getY() + bounds.getHeight() - 3, 0x22FFFFFF);
            }
        } else {
            bevel(graphics, bounds, hovered && !selected ? 0xFFD7D7D7 : SURFACE, false);
        }
        // Erase the attached seam on the selected tab so it joins the owning native frame.
        if (selected) {
            int x = bounds.getX(), y = bounds.getY(), w = bounds.getWidth(), h = bounds.getHeight();
            switch (edge) {
                case LEFT -> graphics.fill(x + w - 3, y + 2, x + w + 2, y + h - 2, SURFACE);
                case RIGHT -> graphics.fill(x - 2, y + 2, x + 3, y + h - 2, SURFACE);
                case TOP -> graphics.fill(x + 2, y + h - 3, x + w - 2, y + h + 2, SURFACE);
                case BOTTOM -> graphics.fill(x + 2, y - 2, x + w - 2, y + 3, SURFACE);
            }
        }
        if (focused) {
            outline(graphics, new Bounds(bounds.getX() + 2, bounds.getY() + 2,
                    bounds.getWidth() - 4, bounds.getHeight() - 4), 0xFFB8A442);
        }
    }

    private static void bevel(Graphics graphics, Bounds bounds, int fill, boolean recessed) {
        int x = bounds.getX(), y = bounds.getY(), w = bounds.getWidth(), h = bounds.getHeight();
        graphics.fill(x + 2, y, x + w - 2, y + h, 0xFF111111);
        graphics.fill(x, y + 2, x + w, y + h - 2, 0xFF111111);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, 0xFF111111);
        graphics.fill(x + 1, y + 1, x + w - 1, y + h - 1, fill);
        int light = recessed ? 0xFF555555 : 0xFFFFFFFF;
        int dark = recessed ? 0xFFFFFFFF : 0xFF555555;
        graphics.fill(x + 1, y + 1, x + w - 2, y + 2, light);
        graphics.fill(x + 1, y + 2, x + 2, y + h - 2, light);
        graphics.fill(x + 2, y + h - 2, x + w - 1, y + h - 1, dark);
        graphics.fill(x + w - 2, y + 2, x + w - 1, y + h - 2, dark);
    }

    private static void outline(Graphics graphics, Bounds bounds, int color) {
        int x = bounds.getX(), y = bounds.getY(), w = bounds.getWidth(), h = bounds.getHeight();
        graphics.fill(x, y, x + w, y + 1, color);
        graphics.fill(x, y + h - 1, x + w, y + h, color);
        graphics.fill(x, y, x + 1, y + h, color);
        graphics.fill(x + w - 1, y, x + w, y + h, color);
    }

    public static void arrow(Graphics graphics, int x, int y) {
        graphics.fill(x, y + 5, x + 15, y + 10, SLOT);
        for (int i = 0; i < 8; i++) {
            graphics.fill(x + 15 + i, y + i - 1, x + 16 + i, y + 16 - i, SLOT);
        }
    }
}
