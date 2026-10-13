package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.client.gui.style.Blitter;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
import com.salts_inventory_update.compat.ae2.client.DesktopChrome;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.ResourceLocation;

/** Fork-used border regions for the toolbar sprite absent from AE2 15; resource-pack art can override them. */
public final class LegacyToolbarBacking {
    private static final ResourceLocation TEXTURE = new ResourceLocation("ae2", "textures/gui/sprites/vertical_buttons_bg.png");
    private static final int EDGE = 0xFF413F54, LIGHT = 0xFFF2F2F2, SHADE = 0xFF878FA5, FACE = 0xFFCBCCD4;
    private LegacyToolbarBacking() { }

    public static Blitter sprite(ResourceLocation id) {
        if (!id.equals(new ResourceLocation("ae2", "vertical_buttons_bg"))) throw new IllegalArgumentException("Unknown legacy desktop sprite: " + id);
        return Blitter.texture(TEXTURE, 21, 26).src(0, 0, 21, 26);
    }

    public static void piece(GuiGraphicsExtractor graphics, Blitter source, int u, int v, int width, int height,
            int x, int y, int targetWidth, int targetHeight) {
        if (Minecraft.getInstance().getResourceManager().getResource(TEXTURE).isPresent()) {
            source.copy().src(u, v, width, height).dest(x, y, targetWidth, targetHeight).blit(graphics.unwrap());
            return;
        }
        paint(new DesktopChrome.Graphics() {
            @Override public void fill(int x0, int y0, int x1, int y1, int color) { graphics.fill(x0, y0, x1, y1, color); }
            @Override public void sprite(String id, DesktopChrome.Bounds bounds) { throw new IllegalStateException("Toolbar border has no sprite dependency"); }
        }, u, v, width, height, x, y, targetWidth, targetHeight);
    }

    public static void paint(DesktopChrome.Graphics graphics, int u, int v, int width, int height,
            int x, int y, int targetWidth, int targetHeight) {
        // Only these six source regions are used by the fork's backingPiece.
        boolean known = (u == 2 && v == 2 && width == 1 && height == 1)
                || (u == 5 && v == 0 && width == 1 && height == 2)
                || (u == 5 && v == 22 && width == 1 && height == 4)
                || (u == 0 && v == 2 && width == 2 && height == 1)
                || (u == 0 && v == 22 && width == 2 && height == 4)
                || (u == 0 && v == 0 && width == 2 && height == 2);
        if (!known) throw new IllegalArgumentException("Unreviewed toolbar source region");
        for (int row = 0; row < targetHeight;) {
            int sourceY = v + row * height / targetHeight;
            int rowEnd = row + 1;
            while (rowEnd < targetHeight && rowColor(v + rowEnd * height / targetHeight) == rowColor(sourceY)) rowEnd++;
            for (int col = 0; col < targetWidth;) {
                int sourceX = u + col * width / targetWidth;
                int color = pixel(sourceX, sourceY);
                int colEnd = col + 1;
                while (colEnd < targetWidth && pixel(u + colEnd * width / targetWidth, sourceY) == color) colEnd++;
                graphics.fill(x + col, y + row, x + colEnd, y + rowEnd, color);
                col = colEnd;
            }
            row = rowEnd;
        }
    }

    private static int rowColor(int y) { return y == 0 || y == 25 ? EDGE : y >= 23 ? SHADE : y == 1 || y == 22 ? LIGHT : FACE; }
    private static int pixel(int x, int y) { return x == 0 ? EDGE : y == 0 || y == 25 ? EDGE : y >= 23 ? SHADE : x == 1 ? LIGHT : rowColor(y); }
}
