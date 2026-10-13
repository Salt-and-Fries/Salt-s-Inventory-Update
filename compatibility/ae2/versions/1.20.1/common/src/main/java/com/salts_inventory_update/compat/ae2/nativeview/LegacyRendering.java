package com.salts_inventory_update.compat.ae2.nativeview;

import appeng.client.Point;
import appeng.client.gui.ICompositeWidget;
import appeng.client.gui.Icon;
import appeng.client.gui.style.Blitter;
import appeng.client.gui.style.BackgroundGenerator;
import appeng.client.gui.me.common.StackSizeRenderer;
import com.salts_inventory_update.api.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Renderable;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.Rect2i;

/** Delegates hosted drawing to the installed AE2 and Minecraft renderers. */
public final class LegacyRendering {
    private LegacyRendering() { }

    public static void blit(Blitter source, GuiGraphicsExtractor graphics) {
        source.blit(graphics.unwrap());
    }

    public static Blitter icon(Icon icon) {
        return icon.getBlitter();
    }

    public static void background(int width, int height, GuiGraphicsExtractor graphics, int x, int y) {
        BackgroundGenerator.draw(width, height, graphics.unwrap(), x, y);
    }

    public static void sizeLabel(GuiGraphicsExtractor graphics, Font font, float x, float y, String label) {
        StackSizeRenderer.renderSizeLabel(graphics.unwrap(), font, x, y, label);
    }

    public static void render(Renderable widget, GuiGraphicsExtractor graphics, int x, int y, float delta) {
        widget.render(graphics.unwrap(), x, y, delta);
        if (widget instanceof net.minecraft.client.gui.components.Button button) LegacyButtons.paint(button, graphics);
    }

    public static void attachToolbar(appeng.client.gui.WidgetContainer widgets, String name,
            ICompositeWidget toolbar, boolean attach) {
        if (attach) widgets.add(name, toolbar);
    }

    public static void compositeBackground(ICompositeWidget widget, GuiGraphicsExtractor graphics,
            Rect2i bounds, Point mouse) {
        widget.drawBackgroundLayer(graphics.unwrap(), bounds, mouse);
    }
}
